package org.fogbowcloud.saps.engine.core.database;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.saps.engine.core.model.ImageData;
import org.fogbowcloud.saps.engine.core.model.ImageState;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.notifier.Ward;

public interface ImageDataStore {

    String NONE = "None";
    int UNLIMITED = -1;

    String DATASTORE_USERNAME = "datastore_username";
    String DATASTORE_PASSWORD = "datastore_password";
    String DATASTORE_DRIVER = "datastore_driver";
    String DATASTORE_URL_PREFIX = "datastore_url_prefix";
    String DATASTORE_NAME = "datastore_name";
    String DATASTORE_IP = "datastore_ip";
    String DATASTORE_PORT = "datastore_port";

	void addImage(String imageName, String downloadLink, int priority,
			String sebalVersion, String sebalTag, String collectionTierImageName)
			throws SQLException;

    void addStateStamp(String imageName, ImageState state, Timestamp timestamp) throws SQLException;
    
	void addUser(String userEmail, String userName, String userPass,
			boolean userState, boolean userNotify, boolean adminRole) throws SQLException;
		
	void addUserNotify(String jobId, String imageName, String userEmail) throws SQLException;	
	
	void addDeployConfig(String nfsIP, String nfsPort, String federationMember) throws SQLException;
	
	List<Ward> getUsersToNotify() throws SQLException;
	
	Map<String, String> getFederationNFSConfig(String federationMember) throws SQLException;
	
	void updateUserState(String userEmail, boolean state) throws SQLException;

    void updateImage(ImageData imageData) throws SQLException;

    void updateImageState(String imageName, ImageState state) throws SQLException;

    void updateImageMetadata(String imageName, String stationId, String sebalVersion) throws SQLException;
    
    void updateImageForPhase2(String imageName, String sebalVersion, String sebalTag) throws SQLException;
    
    boolean isUserNotifiable(String userEmail) throws SQLException;
    
    boolean deployConfigExists(String federationMember) throws SQLException;
    
    boolean imageExist(String imageName) throws SQLException;

    List<ImageData> getAllImages() throws SQLException;

    List<ImageData> getIn(ImageState state) throws SQLException;

    List<ImageData> getIn(ImageState state, int limit) throws SQLException;

    List<ImageData> getPurgedImages() throws SQLException;

    List<ImageData> getImagesToDownload(String federationMember, int limit) throws SQLException;        

    ImageData getImage(String imageName) throws SQLException;
    
    SapsUser getUser(String userEmail) throws SQLException;
    
    String getNFSServerIP(String federationMember) throws SQLException;

    void dispose();

    boolean lockImage(String imageName) throws SQLException;

    boolean unlockImage(String imageName) throws SQLException;

    void removeUserNotify(String jobId, String imageName, String userEmail) throws SQLException;

    void removeStateStamp(String imageName, ImageState state, Timestamp timestamp) throws SQLException;
    
    void removeDeployConfig(String federationMember) throws SQLException;

    List<ImageData> getImagesByFilter(ImageState state, String name, long processDateInit, long processDateEnd)
            throws SQLException;
}
