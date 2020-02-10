package org.fogbowcloud.saps.engine.core.catalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.catalog.exceptions.CatalogException;
import org.fogbowcloud.saps.engine.core.catalog.exceptions.TaskNotFoundException;
import org.fogbowcloud.saps.engine.core.catalog.exceptions.UserNotFoundException;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;

public class JDBCCatalog implements Catalog {

    private String DATASTORE_USERNAME = "datastore_username";
    private String DATASTORE_PASSWORD = "datastore_password";
    private String DATASTORE_DRIVER = "datastore_driver";
    private String DATASTORE_URL_PREFIX = "datastore_url_prefix";
    private String DATASTORE_NAME = "datastore_name";
    private String DATASTORE_IP = "datastore_ip";
    private String DATASTORE_PORT = "datastore_port";

    private static final Logger LOGGER = Logger.getLogger(JDBCCatalog.class);

    private static final String IMAGE_TABLE_NAME = "TASKS";
    private static final String STATES_TABLE_NAME = "TIMESTAMPS";
    private static final String TASK_ID_COL = "task_id";
    private static final String DATASET_COL = "dataset";
    private static final String REGION_COL = "region";
    private static final String IMAGE_DATE_COL = "image_date";
    private static final String PRIORITY_COL = "priority";
    private static final String FEDERATION_MEMBER_COL = "federation_member";
    private static final String STATE_COL = "state";
    private static final String ARREBOL_JOB_ID = "arrebol_job_id";
    private static final String INPUTDOWNLOADING_TAG = "inputdownloading_tag";
    private static final String PREPROCESSING_TAG = "preprocessing_tag";
    private static final String PROCESSING_TAG = "processing_tag";
    private static final String INPUTDOWNLOADING_DIGEST = "inputdownloading_digest";
    private static final String PREPROCESSING_DIGEST = "preprocessing_digest";
    private static final String PROCESSING_DIGEST = "processing_digest";
    private static final String CREATION_TIME_COL = "creation_time";
    private static final String UPDATED_TIME_COL = "updated_time";
    private static final String IMAGE_STATUS_COL = "status";
    private static final String ERROR_MSG_COL = "error_msg";

    private static final String USERS_TABLE_NAME = "USERS";
    private static final String USER_EMAIL_COL = "user_email";
    private static final String USER_NAME_COL = "user_name";
    private static final String USER_PASSWORD_COL = "user_password";
    private static final String USER_STATE_COL = "active";
    private static final String USER_NOTIFY_COL = "user_notify";
    private static final String ADMIN_ROLE_COL = "admin_role";

    private static final String USERS_NOTIFY_TABLE_NAME = "NOTIFY";
    private static final String SUBMISSION_ID_COL = "submission_id";

    private static final String DEPLOY_CONFIG_TABLE_NAME = "DEPLOY_CONFIG";
    private static final String NFS_SERVER_IP_COL = "nfs_ip";
    private static final String NFS_SERVER_SSH_PORT_COL = "nfs_ssh_port";
    private static final String NFS_SERVER_PORT_COL = "nfs_port";

    private static final String PROVENANCE_TABLE_NAME = "PROVENANCE_DATA";
    private static final String INPUT_METADATA_COL = "input_metadata";
    private static final String INPUT_OPERATING_SYSTEM_COL = "input_operating_system";
    private static final String INPUT_KERNEL_VERSION_COL = "input_kernel_version";
    private static final String PREPROCESSING_METADATA_COL = "preprocessing_metadata";
    private static final String PREPROCESSING_OPERATING_SYSTEM_COL = "preprocessing_operating_system";
    private static final String PREPROCESSING_KERNEL_VERSION_COL = "preprocessing_kernel_version";
    private static final String OUTPUT_METADATA_COL = "output_metadata";
    private static final String OUTPUT_OPERATING_SYSTEM_COL = "output_operating_system";
    private static final String OUTPUT_KERNEL_VERSION_COL = "output_kernel_version";

    // Insert queries
    private static final String INSERT_FULL_IMAGE_TASK_SQL = "INSERT INTO " + IMAGE_TABLE_NAME
            + " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private BasicDataSource connectionPool;

    public JDBCCatalog(Properties properties) throws SQLException {

        if (checkProperties(properties))
            if (properties == null) {
                throw new IllegalArgumentException("Properties arg must not be null.");
            }

        String imageStoreIP = properties.getProperty(DATASTORE_IP);
        String imageStorePort = properties.getProperty(DATASTORE_PORT);
        String imageStoreURLPrefix = properties.getProperty(DATASTORE_URL_PREFIX);
        String dbUserName = properties.getProperty(DATASTORE_USERNAME);
        String dbUserPass = properties.getProperty(DATASTORE_PASSWORD);
        String dbDrive = properties.getProperty(DATASTORE_DRIVER);
        String dbName = properties.getProperty(DATASTORE_NAME);

        LOGGER.info("Imagestore " + imageStoreIP + ":" + imageStorePort);
        init(imageStoreIP, imageStorePort, imageStoreURLPrefix, dbUserName, dbUserPass, dbDrive, dbName);
    }

    private boolean checkProperties(Properties properties) {
        if (properties == null) {
            LOGGER.error("Properties arg must not be null.");
            return false;
        }
        if (!properties.containsKey(DATASTORE_URL_PREFIX)) {
            LOGGER.error("Required property " + DATASTORE_URL_PREFIX + " was not set");
            return false;
        }
        if (!properties.containsKey(DATASTORE_USERNAME)) {
            LOGGER.error("Required property " + DATASTORE_USERNAME + " was not set");
            return false;
        }
        if (!properties.containsKey(DATASTORE_PASSWORD)) {
            LOGGER.error("Required property " + DATASTORE_PASSWORD + " was not set");
            return false;
        }
        if (!properties.containsKey(DATASTORE_DRIVER)) {
            LOGGER.error("Required property " + DATASTORE_DRIVER + " was not set");
            return false;
        }
        if (!properties.containsKey(DATASTORE_NAME)) {
            LOGGER.error("Required property " + DATASTORE_NAME + " was not set");
            return false;
        }
        LOGGER.debug("All properties for create JDBCImageDataStore are set");
        return true;
    }

    public JDBCCatalog(String imageStoreURLPrefix, String imageStoreIP, String imageStorePort, String dbUserName,
                       String dbUserPass, String dbDrive, String dbName) throws SQLException {

        init(imageStoreIP, imageStorePort, imageStoreURLPrefix, dbUserName, dbUserPass, dbDrive, dbName);
    }

    private void init(String imageStoreIP, String imageStorePort, String imageStoreURLPrefix, String dbUserName,
                      String dbUserPass, String dbDrive, String dbName) throws SQLException {
        connectionPool = createConnectionPool(imageStoreURLPrefix, imageStoreIP, imageStorePort, dbUserName, dbUserPass,
                dbDrive, dbName);
        createTable();
    }

    private void createTable() throws SQLException {

        Connection connection = null;
        Statement statement = null;

        try {
            connection = getConnection();
            statement = connection.createStatement();

            statement.execute("CREATE TABLE IF NOT EXISTS " + USERS_TABLE_NAME + "(" + USER_EMAIL_COL
                    + " VARCHAR(255) PRIMARY KEY, " + USER_NAME_COL + " VARCHAR(255), " + USER_PASSWORD_COL
                    + " VARCHAR(100), " + USER_STATE_COL + " BOOLEAN, " + USER_NOTIFY_COL + " BOOLEAN, "
                    + ADMIN_ROLE_COL + " BOOLEAN)");

            statement.execute("CREATE TABLE IF NOT EXISTS " + IMAGE_TABLE_NAME + "(" + TASK_ID_COL
                    + " VARCHAR(255) PRIMARY KEY, " + DATASET_COL + " VARCHAR(100), " + REGION_COL + " VARCHAR(100), "
                    + IMAGE_DATE_COL + " DATE, " + STATE_COL + " VARCHAR(100), " + ARREBOL_JOB_ID + " VARCHAR(100),"
                    + FEDERATION_MEMBER_COL + " VARCHAR(255), " + PRIORITY_COL + " INTEGER, " + USER_EMAIL_COL
                    + " VARCHAR(255) REFERENCES " + USERS_TABLE_NAME + "(" + USER_EMAIL_COL + "), "
                    + INPUTDOWNLOADING_TAG + " VARCHAR(100), " + INPUTDOWNLOADING_DIGEST + " VARCHAR(255), "
                    + PREPROCESSING_TAG + " VARCHAR(100), " + PREPROCESSING_DIGEST + " VARCHAR(255), " + PROCESSING_TAG
                    + " VARCHAR(100), " + PROCESSING_DIGEST + " VARCHAR(255), " + CREATION_TIME_COL + " TIMESTAMP, "
                    + UPDATED_TIME_COL + " TIMESTAMP, " + IMAGE_STATUS_COL + " VARCHAR(255), " + ERROR_MSG_COL
                    + " VARCHAR(255))");

            statement.execute("CREATE TABLE IF NOT EXISTS " + STATES_TABLE_NAME + "(" + TASK_ID_COL + " VARCHAR(255), "
                    + STATE_COL + " VARCHAR(100), " + UPDATED_TIME_COL + " TIMESTAMP" + ")");

            statement.execute("CREATE TABLE IF NOT EXISTS " + USERS_NOTIFY_TABLE_NAME + "(" + SUBMISSION_ID_COL
                    + " VARCHAR(255), " + TASK_ID_COL + " VARCHAR(255), " + USER_EMAIL_COL + " VARCHAR(255), "
                    + " PRIMARY KEY(" + SUBMISSION_ID_COL + ", " + TASK_ID_COL + ", " + USER_EMAIL_COL + "))");

            statement.execute("CREATE TABLE IF NOT EXISTS " + DEPLOY_CONFIG_TABLE_NAME + "(" + NFS_SERVER_IP_COL
                    + " VARCHAR(100), " + NFS_SERVER_SSH_PORT_COL + " VARCHAR(100), " + NFS_SERVER_PORT_COL
                    + " VARCHAR(100), " + FEDERATION_MEMBER_COL + " VARCHAR(255), " + " PRIMARY KEY("
                    + NFS_SERVER_IP_COL + ", " + NFS_SERVER_SSH_PORT_COL + ", " + NFS_SERVER_PORT_COL + ", "
                    + FEDERATION_MEMBER_COL + "))");

            statement.execute("CREATE TABLE IF NOT EXISTS " + PROVENANCE_TABLE_NAME + "(" + TASK_ID_COL
                    + " VARCHAR(255) PRIMARY KEY, " + INPUT_METADATA_COL + " VARCHAR(255), "
                    + INPUT_OPERATING_SYSTEM_COL + " VARCHAR(100), " + INPUT_KERNEL_VERSION_COL + " VARCHAR(100), "
                    + PREPROCESSING_METADATA_COL + " VARCHAR(255), " + PREPROCESSING_OPERATING_SYSTEM_COL
                    + " VARCHAR(100), " + PREPROCESSING_KERNEL_VERSION_COL + " VARCHAR(100), " + OUTPUT_METADATA_COL
                    + " VARCHAR(255), " + OUTPUT_OPERATING_SYSTEM_COL + " VARCHAR(100), " + OUTPUT_KERNEL_VERSION_COL
                    + " VARCHAR(100))");

            statement.close();
        } catch (SQLException e) {
            LOGGER.error("Error while initializing DataStore", e);
            throw e;
        } finally {
            close(statement, connection);
        }
    }

    private BasicDataSource createConnectionPool(String imageStoreURLPrefix, String imageStoreIP, String imageStorePort,
                                                 String dbUserName, String dbUserPass, String dbDriver, String dbName) {

        String url = imageStoreURLPrefix + imageStoreIP + ":" + imageStorePort + "/" + dbName;

        LOGGER.debug("DatastoreURL: " + url);

        BasicDataSource pool = new BasicDataSource();
        pool.setUsername(dbUserName);
        pool.setPassword(dbUserPass);
        pool.setDriverClassName(dbDriver);

        pool.setUrl(url);
        pool.setInitialSize(1);

        return pool;
    }

    public Connection getConnection() throws CatalogException {

        try {
            return connectionPool.getConnection();
        } catch (SQLException e) {
            LOGGER.error("Error while getting a new connection from the connection pool", e);
            throw new CatalogException("Error while getting a new connection from the connection pool");
        }
    }

    protected void close(Statement statement, Connection conn) {
        close(statement);

        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Couldn't close connection", e);
            }
        }
    }

    private void close(Statement statement) {
        if (statement != null) {
            try {
                if (!statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Couldn't close statement", e);
            }
        }
    }

    private java.sql.Date javaDateToSqlDate(Date date) {
        return new java.sql.Date(date.getTime());
    }

    @Override
    public SapsImage addTask(String taskId, String dataset, String region, Date date, int priority, String user,
                             String inputdownloadingPhaseTag, String digestInputdownloading, String preprocessingPhaseTag,
                             String digestPreprocessing, String processingPhaseTag, String digestProcessing) throws CatalogException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        SapsImage task = new SapsImage(taskId, dataset, region, date, ImageTaskState.CREATED,
                SapsImage.NONE_ARREBOL_JOB_ID, SapsImage.NONE_FEDERATION_MEMBER, priority, user,
                inputdownloadingPhaseTag, digestInputdownloading, preprocessingPhaseTag, digestPreprocessing,
                processingPhaseTag, digestProcessing, now, now, SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA);

        if (task.getTaskId() == null || task.getTaskId().isEmpty()) {
            LOGGER.error("Task with empty id.");
            throw new IllegalArgumentException("Task with empty id.");
        }
        if (task.getDataset() == null || task.getDataset().isEmpty()) {
            LOGGER.error("Task with empty dataset.");
            throw new IllegalArgumentException("Task with empty dataset.");
        }
        if (task.getImageDate() == null) {
            LOGGER.error("Task must have a date.");
            throw new IllegalArgumentException("Task must have a date.");
        }
        if (task.getUser() == null || task.getUser().isEmpty()) {
            LOGGER.error("Task must have a user.");
            throw new IllegalArgumentException("Task must have a user.");
        }

        LOGGER.info("Adding image task " + task.getTaskId() + " with priority " + task.getPriority());
        LOGGER.info(task.toString());

        PreparedStatement insertStatement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            insertStatement = connection.prepareStatement(INSERT_FULL_IMAGE_TASK_SQL);
            insertStatement.setString(1, task.getTaskId());
            insertStatement.setString(2, task.getDataset());
            insertStatement.setString(3, task.getRegion());
            insertStatement.setDate(4, javaDateToSqlDate(task.getImageDate()));
            insertStatement.setString(5, task.getState().getValue());
            insertStatement.setString(6, task.getArrebolJobId());
            insertStatement.setString(7, task.getFederationMember());
            insertStatement.setInt(8, task.getPriority());
            insertStatement.setString(9, task.getUser());
            insertStatement.setString(10, task.getInputdownloadingTag());
            insertStatement.setString(11, task.getDigestInputdownloading());
            insertStatement.setString(12, task.getPreprocessingTag());
            insertStatement.setString(13, task.getDigestPreprocessing());
            insertStatement.setString(14, task.getProcessingTag());
            insertStatement.setString(15, task.getDigestProcessing());
            insertStatement.setTimestamp(16, task.getCreationTime());
            insertStatement.setTimestamp(17, task.getUpdateTime());
            insertStatement.setString(18, task.getStatus());
            insertStatement.setString(19, task.getError());
            insertStatement.setQueryTimeout(300);

            insertStatement.execute();
        } catch (SQLException e) {
            throw new CatalogException("Error while insert a new task");
        } finally {
            close(insertStatement, connection);
        }

        return task;
    }

    private static final String INSERT_NEW_STATE_TIMESTAMP_SQL = "INSERT INTO " + STATES_TABLE_NAME
            + " VALUES(?, ?, now())";

    @Override
    public void addStateChangeTime(String taskId, ImageTaskState state, Timestamp timestamp) throws CatalogException {
        if (taskId == null || taskId.isEmpty() || state == null) {
            LOGGER.error("Task id or state was null.");
            throw new IllegalArgumentException("Task id or state was null.");
        }
        LOGGER.info("Adding task " + taskId + " state " + state.getValue() + " with timestamp " + timestamp
                + " into Catalogue");

        PreparedStatement insertStatement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            insertStatement = connection.prepareStatement(INSERT_NEW_STATE_TIMESTAMP_SQL);
            insertStatement.setString(1, taskId);
            insertStatement.setString(2, state.getValue());
            insertStatement.setQueryTimeout(300);

            insertStatement.execute();
        } catch (SQLException e) {
            throw new CatalogException("Error while add a new state change time");
        } finally {
            close(insertStatement, connection);
        }
    }

    private static final String INSERT_NEW_USER_SQL = "INSERT INTO " + USERS_TABLE_NAME + " VALUES(?, ?, ?, ?, ?, ?)";

    @Override
    public void addUser(String userEmail, String userName, String userPass, boolean userState, boolean userNotify,
                        boolean adminRole) throws CatalogException {

        LOGGER.info("Adding user " + userName + " into DB");
        if (userName == null || userName.isEmpty() || userPass == null || userPass.isEmpty()) {
            throw new IllegalArgumentException("Unable to create user with empty name or password.");
        }

        PreparedStatement insertStatement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            insertStatement = connection.prepareStatement(INSERT_NEW_USER_SQL);
            insertStatement.setString(1, userEmail);
            insertStatement.setString(2, userName);
            insertStatement.setString(3, userPass);
            insertStatement.setBoolean(4, userState);
            insertStatement.setBoolean(5, userNotify);
            insertStatement.setBoolean(6, adminRole);
            insertStatement.setQueryTimeout(300);

            insertStatement.execute();
        } catch (SQLException e) {
            throw new CatalogException("Error while try add a new user");
        } finally {
            close(insertStatement, connection);
        }
    }

    private static final String UPDATE_IMAGEDATA_SQL = "UPDATE " + IMAGE_TABLE_NAME + " SET " + STATE_COL + " = ?, "
            + UPDATED_TIME_COL + " = now(), " + IMAGE_STATUS_COL + " = ?, " + ERROR_MSG_COL + " = ?, " + ARREBOL_JOB_ID
            + " = ? " + "WHERE " + TASK_ID_COL + " = ?";

    @Override
    public void updateImageTask(SapsImage imagetask) throws CatalogException {
        if (imagetask == null) {
            LOGGER.error("Trying to update null image task.");
            throw new IllegalArgumentException("Trying to update null image task.");
        }

        PreparedStatement updateStatement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            updateStatement = connection.prepareStatement(UPDATE_IMAGEDATA_SQL);
            updateStatement.setString(1, imagetask.getState().getValue());
            updateStatement.setString(2, imagetask.getStatus());
            updateStatement.setString(3, imagetask.getError());
            updateStatement.setString(4, imagetask.getArrebolJobId());
            updateStatement.setString(5, imagetask.getTaskId());
            updateStatement.setQueryTimeout(300);

            updateStatement.execute();
        } catch (SQLException e) {
            throw new CatalogException("Error while try updates task");
        } finally {
            close(updateStatement, connection);
        }
    }

    private static final String SELECT_ALL_IMAGES_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME;

    @Override
    public List<SapsImage> getAllTasks() throws CatalogException {
        Statement statement = null;
        Connection conn = null;
        try {
            conn = getConnection();
            statement = conn.createStatement();
            statement.setQueryTimeout(300);

            statement.execute(SELECT_ALL_IMAGES_SQL);
            ResultSet rs = statement.getResultSet();
            return extractImageTaskFrom(rs);
        } catch (SQLException e) {
            throw new CatalogException("Error while select all tasks");
        } finally {
            close(statement, conn);
        }
    }

    private static final String SELECT_USER_SQL = "SELECT * FROM " + USERS_TABLE_NAME + " WHERE " + USER_EMAIL_COL
            + " = ?";

    @Override
    public SapsUser getUserByEmail(String userEmail) throws CatalogException, UserNotFoundException {

        if (userEmail == null || userEmail.isEmpty()) {
            LOGGER.error("Invalid userEmail " + userEmail);
            return null;
        }
        PreparedStatement selectStatement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            selectStatement = connection.prepareStatement(SELECT_USER_SQL);
            selectStatement.setString(1, userEmail);
            selectStatement.setQueryTimeout(300);

            selectStatement.execute();

            ResultSet rs = selectStatement.getResultSet();
            if (rs.next()) {
                SapsUser sebalUser = extractSapsUserFrom(rs);
                return sebalUser;
            }
            rs.close();
            throw new UserNotFoundException("There is no user with email");
        } catch (SQLException e) {
            throw new CatalogException("Error while getting user by email");
        } finally {
            close(selectStatement, connection);
        }
    }

    private static final String SELECT_IMAGES_IN_STATE_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME + " WHERE ? ORDER BY "
            + PRIORITY_COL + " ASC";

    @Override
    public List<SapsImage> getTasksByState(ImageTaskState... tasksStates) throws CatalogException {
        if (tasksStates == null) {
            LOGGER.error("A state must be given");
            throw new IllegalArgumentException("Can't recover tasks. State was null.");
        }
        PreparedStatement selectStatement = null;
        Connection connection = null;
        try {
            connection = getConnection();

            selectStatement = connection.prepareStatement(SELECT_IMAGES_IN_STATE_SQL);
            selectStatement.setQueryTimeout(300);

            String whereCondition = "";
            for(int i = 0; i < tasksStates.length; i++){
                whereCondition += STATE_COL + " = " + tasksStates[i].getValue();
                if(i < tasksStates.length - 1)
                    whereCondition += " OR ";
            }

            selectStatement.setString(1, whereCondition);
            selectStatement.execute();

            ResultSet rs = selectStatement.getResultSet();
            List<SapsImage> imageDatas = extractImageTaskFrom(rs);
            rs.close();
            return imageDatas;
        } catch (SQLException e) {
            throw new CatalogException("Error while getting task by state");
        } finally {
            close(selectStatement, connection);
        }
    }

    private static SapsUser extractSapsUserFrom(ResultSet rs) throws CatalogException {
        SapsUser sebalUser = null;
        try {
            sebalUser = new SapsUser(rs.getString(USER_EMAIL_COL), rs.getString(USER_NAME_COL),
                    rs.getString(USER_PASSWORD_COL), rs.getBoolean(USER_STATE_COL), rs.getBoolean(USER_NOTIFY_COL),
                    rs.getBoolean(ADMIN_ROLE_COL));
        } catch (SQLException e) {
            throw new CatalogException("Error while extract user");
        }

        return sebalUser;
    }

    private static List<SapsImage> extractImageTaskFrom(ResultSet rs) throws CatalogException {
        List<SapsImage> imageTasks = new ArrayList<>();
        while (true) {
            try {
                if (!rs.next()) break;
                imageTasks.add(new SapsImage(rs.getString(TASK_ID_COL), rs.getString(DATASET_COL), rs.getString(REGION_COL),
                        rs.getDate(IMAGE_DATE_COL), ImageTaskState.getStateFromStr(rs.getString(STATE_COL)),
                        rs.getString(ARREBOL_JOB_ID), rs.getString(FEDERATION_MEMBER_COL), rs.getInt(PRIORITY_COL),
                        rs.getString(USER_EMAIL_COL), rs.getString(INPUTDOWNLOADING_TAG),
                        rs.getString(INPUTDOWNLOADING_DIGEST), rs.getString(PREPROCESSING_TAG),
                        rs.getString(PREPROCESSING_DIGEST), rs.getString(PROCESSING_TAG), rs.getString(PROCESSING_DIGEST),
                        rs.getTimestamp(CREATION_TIME_COL), rs.getTimestamp(UPDATED_TIME_COL),
                        rs.getString(IMAGE_STATUS_COL), rs.getString(ERROR_MSG_COL)));
            } catch (SQLException e) {
                throw new CatalogException("Error while extract task");
            }
        }
        return imageTasks;
    }

    private static final String SELECT_TASK_SQL = "SELECT * FROM " + IMAGE_TABLE_NAME + " WHERE " + TASK_ID_COL
            + " = ?";

    @Override
    public SapsImage getTaskById(String taskId) throws CatalogException, TaskNotFoundException {
        if (taskId == null) {
            LOGGER.error("Invalid image task " + taskId);
            throw new IllegalArgumentException("Invalid image task " + taskId);
        }
        PreparedStatement selectStatement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            selectStatement = connection.prepareStatement(SELECT_TASK_SQL);
            selectStatement.setString(1, taskId);
            selectStatement.setQueryTimeout(300);

            selectStatement.execute();

            ResultSet rs = selectStatement.getResultSet();
            List<SapsImage> imageDatas = extractImageTaskFrom(rs);

            if (imageDatas.size() == 0)
                throw new TaskNotFoundException("There is no task with id");

            rs.close();
            return imageDatas.get(0);
        } catch (SQLException e) {
            throw new CatalogException("Error while getting task by id");
        } finally {
            close(selectStatement, connection);
        }
    }

    private static final String REMOVE_STATE_SQL = "DELETE FROM " + STATES_TABLE_NAME + " WHERE " + TASK_ID_COL
            + " = ? AND " + STATE_COL + " = ? AND " + UPDATED_TIME_COL + " = ?";

    @Override
    public void removeStateChangeTime(String taskId, ImageTaskState state, Timestamp timestamp) throws CatalogException {
        LOGGER.info("Removing task " + taskId + " state " + state.getValue() + " with timestamp " + timestamp);
        if (taskId == null || taskId.isEmpty() || state == null) {
            LOGGER.error("Invalid task " + taskId + " or state " + state.getValue());
            throw new IllegalArgumentException("Invalid task " + taskId);
        }

        PreparedStatement removeStatement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            removeStatement = connection.prepareStatement(REMOVE_STATE_SQL);
            removeStatement.setString(1, taskId);
            removeStatement.setString(2, state.getValue());
            removeStatement.setTimestamp(3, timestamp);
            removeStatement.setQueryTimeout(300);

            removeStatement.execute();
        } catch (SQLException e) {
            throw new CatalogException("Error while removes state change time");
        } finally {
            close(removeStatement, connection);
        }
    }

    private final String SELECT_TASKS_BY_FILTERS_QUERY = "SELECT * FROM " + IMAGE_TABLE_NAME + " WHERE " + STATE_COL
            + " = ? AND " + REGION_COL + " = ? AND " + IMAGE_DATE_COL + " BETWEEN ? AND ? AND " + PREPROCESSING_TAG
            + " = ? AND " + INPUTDOWNLOADING_TAG + " = ? AND " + PROCESSING_TAG + " = ?";

    @Override
    public List<SapsImage> filterTasks(ImageTaskState state, String region, Date initDate, Date endDate, String inputGathering,
                                       String preprocessingTag, String processingTag) throws CatalogException {
        PreparedStatement queryStatement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            queryStatement = connection.prepareStatement(SELECT_TASKS_BY_FILTERS_QUERY);
            queryStatement.setString(1, state.getValue());
            queryStatement.setString(2, region);
            queryStatement.setDate(3, javaDateToSqlDate(initDate));
            queryStatement.setDate(4, javaDateToSqlDate(endDate));
            queryStatement.setString(5, preprocessingTag);
            queryStatement.setString(6, inputGathering);
            queryStatement.setString(7, processingTag);
            queryStatement.setQueryTimeout(300);

            ResultSet result = queryStatement.executeQuery();
            return extractImageTaskFrom(result);
        } catch (SQLException e) {
            throw new CatalogException("Error while getting tasks by filters");
        } finally {
            close(queryStatement, connection);
        }
    }

}
