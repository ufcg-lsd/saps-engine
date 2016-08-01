package org.fogbowcloud.sebal.engine.sebal.bootstrap;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.NASARepository;

public class DBUtilsImpl implements DBUtils {

    private static final Logger LOGGER = Logger.getLogger(DBUtilsImpl.class);

    private final JDBCImageDataStore imageStore;
    private NASARepository nasaRepository;
    private Properties properties;

    public DBUtilsImpl(Properties properties) throws SQLException {
        this.properties = properties;
        this.imageStore = new JDBCImageDataStore(this.properties);
        this.nasaRepository = new NASARepository(properties);
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
            	LOGGER.error("Error while parsing string to date", e);
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
            LOGGER.error("Error while gettin images in " + ImageState.CORRUPTED + " state from DB", e);
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