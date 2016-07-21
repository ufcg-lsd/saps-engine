package org.fogbowcloud.sebal.engine.sebal;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

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

    void addImage(String imageName, String downloadLink, int priority) throws SQLException;

    void addStateStamp(String imageName, ImageState state, Date timestamp) throws SQLException;

    void updateImage(ImageData imageData) throws SQLException;

    void updateImageState(String imageName, ImageState state) throws SQLException;

    void updateImageMetadata(String imageName, String stationId, String sebalVersion) throws SQLException;

    List<ImageData> getAllImages() throws SQLException;

    List<ImageData> getIn(ImageState state) throws SQLException;

    List<ImageData> getIn(ImageState state, int limit) throws SQLException;

    List<ImageData> getPurgedImages() throws SQLException;

    List<ImageData> getImagesToDownload(String federationMember, int limit) throws SQLException;

    ImageData getImage(String imageName) throws SQLException;

    void dispose();

    boolean lockImage(String imageName) throws SQLException;

    boolean unlockImage(String imageName) throws SQLException;

    void removeStateStamp(String imageName, ImageState state, Date timestamp) throws SQLException;

    List<ImageData> getImagesByFilter(ImageState state, String name, long processDateInit, long processDateEnd)
            throws SQLException;
}
