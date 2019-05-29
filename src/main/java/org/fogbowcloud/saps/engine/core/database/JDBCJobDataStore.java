package org.fogbowcloud.saps.engine.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.job.SapsJob;
import org.json.JSONException;
import org.json.JSONObject;

public class JDBCJobDataStore implements JobDataStore{

	private static final String JOBS_TABLE_NAME = "saps_jobs";
	private static final String JOB_ID = "job_id";
	private static final String JOB_JSON = "job_json";
	private static final String JOB_OWNER = "job_owner";

	private static final String CREATE_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS " + JOBS_TABLE_NAME + "("
			+ JOB_ID + " VARCHAR(255) PRIMARY KEY, "
			+ JOB_OWNER + " VARCHAR(255), "
			+ JOB_JSON + " TEXT)";

	private static final String INSERT_JOB_TABLE_SQL = "INSERT INTO " + JOBS_TABLE_NAME
			+ " VALUES(?, ?, ?)";

	private static final String UPDATE_JOB_TABLE_SQL = "UPDATE " + JOBS_TABLE_NAME
			+ " SET " + JOB_ID + " = ?, " + JOB_OWNER + " = ?, " + JOB_JSON + " = ? WHERE " + JOB_ID + " = ?";

	private static final String GET_ALL_JOB = "SELECT * FROM " + JOBS_TABLE_NAME;
	private static final String GET_JOB_BY_OWNER = GET_ALL_JOB + " WHERE " + JOB_OWNER + " = ? ";
	private static final String GET_JOB_BY_JOB_ID = GET_ALL_JOB + " WHERE " + JOB_ID + " = ? AND " + JOB_OWNER + " = ?";

	private static final String DELETE_ALL_JOB_TABLE_SQL = "DELETE FROM " + JOBS_TABLE_NAME;
	private static final String DELETE_BY_OWNER = "DELETE FROM " + JOBS_TABLE_NAME + " WHERE " + JOB_OWNER
			+ " = ? ";
	private static final String DELETE_BY_JOB_ID_SQL = "DELETE FROM " + JOBS_TABLE_NAME + " WHERE "
			+ JOB_ID + " = ? AND " + JOB_OWNER + " = ?";

	private static final Logger LOGGER = Logger.getLogger(JDBCJobDataStore.class);
	private static final String ERROR_WHILE_INITIALIZING_THE_DATA_STORE = "Error while initializing the Job DataStore.";

	private BasicDataSource connectionPool;
	
	public JDBCJobDataStore(Properties properties) throws SQLException {
		if (properties == null) {
			throw new IllegalArgumentException("Properties arg must not be null.");
		}

		String imageStoreIP = properties.getProperty(DATASTORE_IP);
		String imageStorePort = properties.getProperty(DATASTORE_PORT);
		String imageStoreURLPrefix = properties.getProperty(DATASTORE_URL_PREFIX);
		String dbUserName = properties.getProperty(DATASTORE_USERNAME);
		String dbUserPass = properties.getProperty(DATASTORE_PASSWORD);
		String dbDriver = properties.getProperty(DATASTORE_DRIVER);
		String dbName = properties.getProperty(DATASTORE_NAME);

		LOGGER.info("Imagestore " + imageStoreIP + ":" + imageStorePort);
		init(imageStoreIP, imageStorePort, imageStoreURLPrefix, dbUserName, dbUserPass, dbDriver,
				dbName);
	}

	public JDBCJobDataStore(String imageStoreURLPrefix, String imageStoreIP,
			String imageStorePort, String dbUserName, String dbUserPass, String dbDriver,
			String dbName) throws SQLException {

		init(imageStoreIP, imageStorePort, imageStoreURLPrefix, dbUserName, dbUserPass, dbDriver,
				dbName);
	}

	private void init(String imageStoreIP, String imageStorePort, String imageStoreURLPrefix,
			String dbUserName, String dbUserPass, String dbDriver, String dbName)
			throws SQLException {
		connectionPool = createConnectionPool(imageStoreURLPrefix, imageStoreIP, imageStorePort,
				dbUserName, dbUserPass, dbDriver, dbName);
		createTable();
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
	
	private void createTable() {
		Connection connection = null;
		Statement statement = null;

		try {
			connection = getConnection();
			statement = connection.createStatement();
			statement.execute(CREATE_TABLE_STATEMENT);
			
			statement.close();
		} catch (SQLException e) {
			LOGGER.error("Error while initializing DataStore", e);
		} finally {
			close(statement, connection);
		}
	}

	public boolean insert(SapsJob job) {
		LOGGER.debug("Inserting job [" + job.getId() + "] with owner [" + job.getOwner() + "]");

		if (job.getId() == null || job.getId().isEmpty()
				|| job.getOwner() == null || job.getOwner().isEmpty()) {
			LOGGER.warn("Job Id and owner must not be null.");
			return false;
		}

		PreparedStatement preparedStatement = null;
		Connection connection = null;
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			preparedStatement = connection.prepareStatement(INSERT_JOB_TABLE_SQL);
			preparedStatement.setString(1, job.getId());
			preparedStatement.setString(2, job.getOwner());
			preparedStatement.setString(3, job.toJSON().toString());

			preparedStatement.execute();
			connection.commit();
			return true;
		} catch (SQLException e) {
			LOGGER.error("Couldn't execute statement : " + INSERT_JOB_TABLE_SQL, e);
			try {
				if (connection != null) {
					connection.rollback();
				}
			} catch (SQLException e1) {
				LOGGER.error("Couldn't rollback transaction.", e1);
			}
			return false;
		} finally {
			close(preparedStatement, connection);
		}
	}

	public boolean update(SapsJob job) {
		LOGGER.debug("Updating job [" + job.getId() + "] from owner [" + job.getOwner() + "]");

		if (job.getId() == null || job.getId().isEmpty()
				|| job.getOwner() == null || job.getOwner().isEmpty()) {
			LOGGER.warn("Job Id and owner must not be null.");
			return false;
		}

		PreparedStatement preparedStatement = null;
		Connection connection = null;
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			preparedStatement = connection.prepareStatement(UPDATE_JOB_TABLE_SQL);
			preparedStatement.setString(1, job.getId());
			preparedStatement.setString(2, job.getOwner());
			preparedStatement.setString(3, job.toJSON().toString());
			preparedStatement.setString(4, job.getId());
			
			preparedStatement.execute();
			connection.commit();
			return true;
		} catch (SQLException e) {
			LOGGER.error("Couldn't execute statement : " + UPDATE_JOB_TABLE_SQL, e);
			try {
				if (connection != null) {
					connection.rollback();
				}
			} catch (SQLException e1) {
				LOGGER.error("Couldn't rollback transaction.", e1);
			}
			return false;
		} finally {
			close(preparedStatement, connection);
		}
	}

	public List<SapsJob> getAll() {
		LOGGER.debug("Getting all instances id with related orders.");
		return executeQueryStatement(GET_ALL_JOB);
	}

	public List<SapsJob> getAllByOwner(String owner) {
		LOGGER.debug("Getting all jobs to owner [" + owner + "]");
		return executeQueryStatement(GET_JOB_BY_OWNER, owner);
	}

	public SapsJob getByJobId(String jobId, String owner) {
		LOGGER.debug("Getting jobs by job ID [" + jobId + "]");

		List<SapsJob> jdfJobList = executeQueryStatement(GET_JOB_BY_JOB_ID, jobId, owner);
		if (jdfJobList != null && !jdfJobList.isEmpty()) {
			return jdfJobList.get(0);
		}
		return null;
	}

	public boolean deleteAll() {
		LOGGER.debug("Deleting all jobs.");

		Statement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.createStatement();

			boolean result = statement.execute(DELETE_ALL_JOB_TABLE_SQL);
			conn.commit();
			return result;
		} catch (SQLException e) {
			LOGGER.error("Couldn't delete all registres on " + JOBS_TABLE_NAME, e);
			return false;
		} finally {
			close(statement, conn);
		}
	}

	public boolean deleteAllFromOwner(String owner) {
		LOGGER.debug("Deleting all job from owner [" + owner + "].");

		PreparedStatement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(DELETE_BY_OWNER);
			statement.setString(1, owner);
			boolean result = statement.execute();
			conn.commit();
			return result;

		} catch (SQLException e) {
			LOGGER.error("Couldn't delete all registres from owner [" + owner + "] on " + JOBS_TABLE_NAME, e);
			return false;
		} finally {
			close(statement, conn);
		}
	}

	public boolean deleteByJobId(String jobId, String owner) {
		LOGGER.debug("Deleting all jobs with id [" + jobId + "]");

		PreparedStatement statement = null;
		Connection conn = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(DELETE_BY_JOB_ID_SQL);
			statement.setString(1, jobId);
			statement.setString(2, owner);
			//			conn.commit(); // database on autocommit
			return statement.execute();
		} catch (SQLException e) {
			LOGGER.error("Couldn't delete registres on " + JOBS_TABLE_NAME + " with Job id ["
					+ jobId + "]", e);
			return false;
		} finally {
			close(statement, conn);
		}
	}

	public SapsJob getObjFromDataStoreResult(ResultSet rs) throws Exception {
		SapsJob job = SapsJob.fromJSON(new JSONObject(rs.getString(JOB_JSON)));
		return job;
	}

	protected List<SapsJob> executeQueryStatement(String queryStatement, String... params) {
        PreparedStatement preparedStatement = null;
        Connection conn = null;
        List<SapsJob> dataList = new ArrayList<>();

        try {
            conn = getConnection();
            preparedStatement = conn.prepareStatement(queryStatement);

            if (params != null && params.length > 0) {
                for (int index = 0; index < params.length; index++) {
                    preparedStatement.setString(index + 1, params[index]);
                }
            }

            ResultSet rs = preparedStatement.executeQuery();

            if (rs != null) {
                try {
                    while (rs.next()) {
                        SapsJob data = getObjFromDataStoreResult(rs);
                        dataList.add(data);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error while mounting token from DB.", e);
                }
            }

        } catch (SQLException e) {
            LOGGER.error("Couldn't get data from DB.", e);
            return new ArrayList<>();
        } finally {
            close(preparedStatement, conn);
        }
        LOGGER.debug("There are " + dataList.size() + " rows at DB to this query (" + preparedStatement.toString() + ").");
        return dataList;
    }
	
	public Connection getConnection() throws SQLException {

		try {
			return connectionPool.getConnection();
		} catch (SQLException e) {
			LOGGER.error("Error while getting a new connection from the connection pool", e);
			throw e;
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
	
}