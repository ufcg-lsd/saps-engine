package org.fogbowcloud.sebal.engine.sebal.bootstrap;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.sebal.DefaultNASARepository;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.USGSNasaRepository;
import org.fogbowcloud.sebal.engine.sebal.model.SebalUser;
import org.fogbowcloud.sebal.notifier.Ward;

public class DBUtilsImpl implements DBUtils {

	private static final String DATASET_LT5_TYPE = "landsat_5";
	private static final String DATASET_LE7_TYPE = "landsat_7";
	private static final String DATASET_LE8_TYPE = "landsat_8";
    private static final Logger LOGGER = Logger.getLogger(DBUtilsImpl.class);

    private final JDBCImageDataStore imageStore;
    private DefaultNASARepository nasaRepository;
    private USGSNasaRepository usgsRepository;
    private Properties properties;

    public DBUtilsImpl(Properties properties) throws SQLException {
        this.properties = properties;
        this.imageStore = new JDBCImageDataStore(this.properties);
        this.nasaRepository = new DefaultNASARepository(properties);
		this.usgsRepository = new USGSNasaRepository(properties);		
		this.usgsRepository.handleAPIKeyUpdate(Executors.newScheduledThreadPool(1));
    }
    
	@Override
	public void addUserInDB(String userEmail, String userName, String userPass,
			boolean userState, boolean userNotify, boolean adminRole)
			throws SQLException {

		try {
			imageStore.addUser(userEmail, userName, userPass, userState,
					userNotify, adminRole);
		} catch (SQLException e) {
			LOGGER.error("Error while adding user " + userEmail + " in DB", e);
			throw new SQLException(e);
		}
	}
	
	@Override
	public void updateUserState(String userEmail, boolean userState) throws SQLException {
		
		try {
			imageStore.updateUserState(userEmail, userState);
		} catch (SQLException e) {
			LOGGER.error("Error while adding user " + userEmail + " in DB", e);
			throw new SQLException(e);
		}
	}
	
	@Override
	public SebalUser getUser(String userEmail) {
		try {
			return imageStore.getUser(userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while trying to get Sebal User with email: " + userEmail + ".", e);
		}
		return null;
	}
	
	@Override
	public void addUserInNotifyDB(String jobId, String imageName, String userEmail) throws SQLException {
		
		try {
			imageStore.addUserNotify(jobId, imageName, userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while adding image " + imageName + " user " + userEmail + " in notify DB", e);
		}
	}
	
	@Override
	public void removeUserNotify(String jobId, String imageName, String userEmail)
			throws SQLException {

		try {
			imageStore.removeUserNotify(jobId, imageName, userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while removing image " + imageName + " user "
					+ userEmail + " from notify DB", e);
		}
	}
	
	@Override
	public boolean isUserNotifiable(String userEmail) throws SQLException {
		
		try {
			return imageStore.isUserNotifiable(userEmail);
		} catch(SQLException e) {
			LOGGER.error("Error while verifying user notify", e);
		}
		
		return false;
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
                
                imageStore.updateImage(imageData);
                imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
            }
        }
    }

    protected Date parseStringToDate(String day) throws ParseException {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        java.util.Date date = format.parse(day);
        java.sql.Date sqlDate = new java.sql.Date(date.getTime());
        return sqlDate;
    }

    protected boolean isBeforeDay(long date, Timestamp imageDataDay) {
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
    
    // TODO: test
    @Override
    public void setImageForPhase2(String imageName, String sebalVersion, String sebalTag) throws SQLException {
		LOGGER.debug("Updating image " + imageName + " with sebalVersion "
				+ sebalVersion + " and tag " + sebalTag + " to execute phase 2");
		
		try {
			getImageStore().updateImageForPhase2(imageName, sebalVersion, sebalTag);
		} catch(SQLException e) {
			LOGGER.error("Error while updating image " + imageName + " with sebalVersion "
					+ sebalVersion + " and tag " + sebalTag + " to execute phase 2");
		}
    }

    @Override
	public List<String> fillDB(int firstYear, int lastYear,
			List<String> regions, String dataSet, String sebalVersion,
			String sebalTag) throws IOException {

        LOGGER.debug("Regions: " + regions);
        List<String> imageNames = new ArrayList<String>();
        int priority = 0;
        for (String region : regions) {
            for (int year = firstYear; year <= lastYear; year++) {
            	//String imageList = createImageList(region, year, dataSet);
				StringBuilder imageList = new StringBuilder();
				for (int day = 1; day < 366; day += 16) {
					NumberFormat formatter = new DecimalFormat("000");
		            String imageName = new String();
		            
					if (dataSet.equals(DATASET_LT5_TYPE)) {
						imageName = "LT5";
					} else if(dataSet.equals(DATASET_LE7_TYPE)) {
						imageName = "LE7";
					} else if(dataSet.equals(DATASET_LE8_TYPE)) {
						imageName = "LE8"; 
					}
					
					//TODO: change day format
					String imageDownloadLink = getUSGSRepository()
							.getImageDownloadLink(imageName, region, String.valueOf(year), "month",
									formatter.format(day),
									getUSGSRepository().getPossibleProcessingCorrectionLevels());
					
					if(imageDownloadLink != null && !imageDownloadLink.isEmpty()) {                		
						try {
							getImageStore().addImage(imageName,
									"None", priority, sebalVersion, sebalTag);
						} catch (SQLException e) {
							LOGGER.error("Error while adding image at data base.", e);
						}
					}
				}
            }
            priority++;
        }
        return imageNames;
    }

    public List<ImageData> getImagesInDB() throws SQLException, ParseException {
    	
    	return imageStore.getAllImages();        
    }
    
    @Override
    public List<Ward> getUsersToNotify() throws SQLException {    	
    	
    	List<Ward> wards = imageStore.getUsersToNotify();    	
    	return wards;    	
    }
    
    public ImageData getImageInDB(String imageName) throws SQLException {
    	List<ImageData> allImages = imageStore.getAllImages();
    	
    	for(ImageData imageData : allImages) {
    		if(imageData.getName().equals(imageName)) {
    			return imageData;
    		}
    	}
    	
    	// FIXME: deal with this better
    	return null;
    }

    protected String createImageList(String region, int year, String dataSet) {
        StringBuilder imageList = new StringBuilder();
        for (int day = 1; day < 366; day+=16) {
            NumberFormat formatter = new DecimalFormat("000");
            String imageName = new String();
            
			if (dataSet.equals(DATASET_LT5_TYPE)) {
				imageName = "LT5" + region + year + formatter.format(day);
			} else if(dataSet.equals(DATASET_LE7_TYPE)) {
				imageName = "LE7" + region + year + formatter.format(day);
			} else if(dataSet.equals(DATASET_LE8_TYPE)) {
				imageName = "LE8" + region + year + formatter.format(day); 
			}
            
            imageList.append(imageName + "\n");
        }
        return imageList.toString().trim();
    }

    public JDBCImageDataStore getImageStore() {
        return imageStore;
    }

    protected void setNasaRepository(DefaultNASARepository nasaRepository) {
        this.nasaRepository = nasaRepository;
    }

    protected DefaultNASARepository getNasaRepository() {
        return nasaRepository;
    }
    
    protected void setUSGSRepository(USGSNasaRepository usgsRepository) {
    	this.usgsRepository = usgsRepository;
    }
    
    protected USGSNasaRepository getUSGSRepository() {
    	return usgsRepository;
    }

    public static String getImageRegionFromName(String imageName) {
        return imageName.substring(3, 9);
    }
    
    public Properties getProperties() {
    	return properties;
    }
}