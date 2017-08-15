package org.fogbowcloud.saps.engine.core.bootstrap;

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

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageData;
import org.fogbowcloud.saps.engine.core.model.ImageState;
import org.fogbowcloud.saps.engine.core.model.SebalUser;
import org.fogbowcloud.saps.engine.core.repository.DefaultImageRepository;
import org.fogbowcloud.saps.engine.core.repository.USGSNasaRepository;
import org.fogbowcloud.saps.engine.scheduler.util.SebalPropertiesConstants;
import org.fogbowcloud.saps.notifier.Ward;
import org.json.JSONArray;
import org.json.JSONException;

public class DBUtilsImpl implements DBUtils {

    private final JDBCImageDataStore imageStore;
    private DefaultImageRepository nasaRepository;
    private USGSNasaRepository usgsRepository;
    private Properties properties;
    
    private static final Logger LOGGER = Logger.getLogger(DBUtilsImpl.class);

    public DBUtilsImpl(Properties properties) throws SQLException {
        this.properties = properties;
        this.imageStore = new JDBCImageDataStore(this.properties);
        this.nasaRepository = new DefaultImageRepository(properties);
		this.usgsRepository = new USGSNasaRepository(properties);		
		this.usgsRepository.handleAPIKeyUpdate(Executors.newScheduledThreadPool(1));
    }
    
	@Override
	public void addUserInDB(String userEmail, String userName, String userPass, boolean userState,
			boolean userNotify, boolean adminRole) throws SQLException {
		try {
			imageStore.addUser(userEmail, userName, userPass, userState, userNotify, adminRole);
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
	public void removeUserNotify(String jobId, String imageName, String userEmail) throws SQLException {
		try {
			imageStore.removeUserNotify(jobId, imageName, userEmail);
		} catch (SQLException e) {
			LOGGER.error("Error while removing image " + imageName + " user " + userEmail
					+ " from notify DB", e);
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
		List<ImageData> imagesToPurge = force ? imageStore.getAllImages() : imageStore
				.getIn(ImageState.FETCHED);

        for (ImageData imageData : imagesToPurge) {
            long date = 0;
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
    
    // TODO: test
    @Override
    public void setImageForPhase2(String imageName, String sebalVersion, String sebalTag) throws SQLException {
		LOGGER.debug("Updating image " + imageName + " with sebalVersion " + sebalVersion
				+ " and tag " + sebalTag + " to execute phase 2");
		
		try {
			getImageStore().updateImageForPhase2(imageName, sebalVersion, sebalTag);
		} catch(SQLException e) {
			LOGGER.error("Error while updating image " + imageName + " with sebalVersion "
					+ sebalVersion + " and tag " + sebalTag + " to execute phase 2");
		}
    }

    @Override
	public List<String> fillDB(int firstYear, int lastYear, List<String> regions, String dataSet,
			String sebalVersion, String sebalTag) throws IOException {
		LOGGER.debug("Regions: " + regions);
		List<String> obtainedImages = new ArrayList<String>();
		String parsedDataSet = parseDataset(dataSet);
		
		int priority = 0;		
		for (String region : regions) {
			submitImagesForYears(parsedDataSet, firstYear, lastYear, region, sebalVersion,
					sebalTag, priority, obtainedImages);
			priority++;
		}
		return obtainedImages;
	}

	private String parseDataset(String dataSet) {
		if(dataSet.equals(SebalPropertiesConstants.DATASET_LT5_TYPE)) {
			return SebalPropertiesConstants.LANDSAT_5_DATASET;
		} else if(dataSet.equals(SebalPropertiesConstants.DATASET_LE7_TYPE)) {
			return SebalPropertiesConstants.LANDSAT_7_DATASET;
		} else if(dataSet.equals(SebalPropertiesConstants.DATASET_LC8_TYPE)) {
			return SebalPropertiesConstants.LANDSAT_8_DATASET;
		}
		
		return null;
	}

	private void submitImagesForYears(String dataSet, int firstYear, int lastYear, String region,
			String sebalVersion, String sebalTag, int priority, List<String> obtainedImages) {
		JSONArray availableImagesJSON = getUSGSRepository().getAvailableImagesInRange(dataSet,
				firstYear, lastYear, region);
		
		if(availableImagesJSON != null) {
			try {
				for (int i = 0; i < availableImagesJSON.length(); i++) {
					String entityId = availableImagesJSON.getJSONObject(i).getString(
							SebalPropertiesConstants.ENTITY_ID_JSON_KEY);
					String displayId = availableImagesJSON.getJSONObject(i).getString(
							SebalPropertiesConstants.DISPLAY_ID_JSON_KEY);
					
					getImageStore().addImage(entityId, "None", priority, sebalVersion, sebalTag,
							displayId);
					getImageStore().addStateStamp(entityId, ImageState.NOT_DOWNLOADED,
							getImageStore().getImage(entityId).getUpdateTime());
					obtainedImages.add(displayId);
				}
			} catch (JSONException e) {
				LOGGER.error("Error while getting entityId and displayId from JSON response", e);
			} catch (SQLException e) {
				LOGGER.error("Error while adding image to database", e);
			}			
		}
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
    	
    	return null;
    }

	protected String createImageList(String region, int year, String dataSet) {
		StringBuilder imageList = new StringBuilder();
		for (int day = 1; day < 366; day++) {
			NumberFormat formatter = new DecimalFormat("000");
			String imageName = new String();

			if (dataSet.equals(SebalPropertiesConstants.DATASET_LT5_TYPE)) {
				imageName = "LT5" + region + year + formatter.format(day);
			} else if (dataSet.equals(SebalPropertiesConstants.DATASET_LE7_TYPE)) {
				imageName = "LE7" + region + year + formatter.format(day);
			} else if (dataSet.equals(SebalPropertiesConstants.DATASET_LC8_TYPE)) {
				imageName = "LC8" + region + year + formatter.format(day);
			}

			imageList.append(imageName + "\n");
		}
		return imageList.toString().trim();
	}

    public JDBCImageDataStore getImageStore() {
        return imageStore;
    }

    protected void setNasaRepository(DefaultImageRepository nasaRepository) {
        this.nasaRepository = nasaRepository;
    }

    protected DefaultImageRepository getNasaRepository() {
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