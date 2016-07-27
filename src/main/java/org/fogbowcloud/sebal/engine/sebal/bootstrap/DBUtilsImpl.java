package org.fogbowcloud.sebal.engine.sebal.bootstrap;

import java.io.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.sebal.*;

public class DBUtilsImpl implements DBUtils {

    private static final Logger LOGGER = Logger.getLogger(DBUtilsImpl.class);

    private final JDBCImageDataStore imageStore;
    private NASARepository nasaRepository;
    private Properties properties;

    private static final String SELECT_ALL_IMAGES_SQL = "SELECT * FROM nasa_images ORDER BY priority, image_name";

    public DBUtilsImpl(Properties properties) throws SQLException {
        this.properties = properties;
        this.imageStore = new JDBCImageDataStore(this.properties);
        this.nasaRepository = new NASARepository(properties);
    }

    private Connection getConnection() throws SQLException {
        return imageStore.getConnection();
    }

    private void preparingStatement(Connection c) throws SQLException {

        PreparedStatement selectStatement = c.prepareStatement(SELECT_ALL_IMAGES_SQL);
        ResultSet rs = selectStatement.executeQuery();

        while (rs.next()) {
            ImageData imageData = new ImageData(
                    rs.getString("image_name"),
                    rs.getString("download_link"),
                    ImageState.getStateFromStr(rs.getString("state")),
                    rs.getString("federation_member"),
                    rs.getInt("priority"),
                    rs.getString("station_id"),
                    rs.getString("sebal_version"),
                    rs.getString("sebal_engine_version"),
                    rs.getString("blowout_version"),
                    rs.getDate("ctime"),
                    rs.getDate("utime"),
                    rs.getString("error_msg"));
            System.out.println(imageData);
        }
    }

    @Override
    public void setImagesToPurge(String day, boolean force) throws SQLException, ParseException {

        List<ImageData> imagesToPurge =
                force ? imageStore.getAllImages() : imageStore.getIn(ImageState.FETCHED);

        for (ImageData imageData : imagesToPurge) {
            long date = 0;
            // FIXME: deal with this better
            try {
                date = parseStringToDate(day).getTime();
            } catch (ParseException e) {
            	LOGGER.error(e);
                e.printStackTrace();
            }
            if (isBeforeDay(date, imageData.getUpdateTime())) {
                imageData.setImageStatus(ImageData.PURGED);
                imageData.setUpdateTime(new Date(Calendar.getInstance().getTimeInMillis()));
                imageStore.updateImage(imageData);
            }
        }
    }

    protected Date parseStringToDate(String day) throws ParseException {
        DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        java.util.Date date = format.parse(day);
        java.sql.Date sqlDate = new java.sql.Date(date.getTime());
        return sqlDate;
    }

    protected boolean isBeforeDay(long date, Date imageDataDay) {
        return (imageDataDay.getTime() <= date);
    }

    @Override
    public void listImagesInDB() throws SQLException, ParseException {

        List<ImageData> allImageData = imageStore.getAllImages();
        for (int i = 0; i < allImageData.size(); i++) {
            System.out.println(allImageData.get(i).toString());
        }
    }

    @Override
    public void listCorruptedImages() throws ParseException {

        List<ImageData> allImageData;
        try {
            allImageData = imageStore.getIn(ImageState.CORRUPTED);
            for (int i = 0; i < allImageData.size(); i++) {
                System.out.println(allImageData.get(i).toString());
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
    }

    @Override
    public void getRegionImages(int firstYear, int lastYear, String region) throws SQLException, ParseException {

        for (int year = firstYear; year <= lastYear; year++) {
            List<String> imageList = new ArrayList<String>();

            for (int day = 1; day < 366; day++) {
                //FIXME: extract the join stuff to an aux method
                NumberFormat formatter = new DecimalFormat("000");
                String imageName = "LT5" + region + year + formatter.format(day) + "CUB00";
                imageList.add(imageName);
                if (imageStore.getImage(imageName) != null) {
                    imageStore.getImage(imageName).toString();
                }
            }
        }
    }

    @Override
    public void fillDB(int firstYear, int lastYear, List<String> regions) throws IOException {

        LOGGER.debug("Regions: " + regions);

        int priority = 0;
        for (String region : regions) {
            for (int year = firstYear; year <= lastYear; year++) {
                String imageList = createImageList(region, year);

                File imageListFile = new File("images-" + year + ".txt");
                FileUtils.write(imageListFile, imageList);

                Map<String, String> imageAndDownloadLink = getNasaRepository()
                        .checkExistingImages(imageListFile);

                imageListFile.delete();

                for (String imageName : imageAndDownloadLink.keySet()) {
                    try {
                        getImageStore().addImage(imageName,
                                imageAndDownloadLink.get(imageName), priority);
                    } catch (SQLException e) {
                        // TODO do we need to do something?
                        LOGGER.error("Error while adding image at data base.", e);
                    }
                }
            }
            priority++;
        }
    }

    protected String createImageList(String region, int year) {
        StringBuilder imageList = new StringBuilder();
        for (int day = 1; day < 366; day++) {
            NumberFormat formatter = new DecimalFormat("000");
            String imageName = "LT5" + region + year + formatter.format(day) + "CUB00";
            imageList.append(imageName + "\n");
        }
        return imageList.toString().trim();
    }

    public JDBCImageDataStore getImageStore() {
        return imageStore;
    }

    protected void setNasaRepository(NASARepository nasaRepository) {
        this.nasaRepository = nasaRepository;
    }

    protected NASARepository getNasaRepository() {
        return nasaRepository;
    }

    public static String getImageRegionFromName(String imageName) {
        return imageName.substring(3, 9);
    }
}