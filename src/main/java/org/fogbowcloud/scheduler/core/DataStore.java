package org.fogbowcloud.scheduler.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Order;
import org.h2.jdbcx.JdbcConnectionPool;

public class DataStore {


	private static final Logger LOGGER = Logger.getLogger(DataStore.class);

	protected static final String REQUEST_ID_TABLE_NAME = "request_Ids";

	protected static final String REQUEST_ID = "requestid";

	private String dataStoreURL;
	private JdbcConnectionPool cp;

	public DataStore(Properties properties) {
		this.dataStoreURL = properties.getProperty("accounting_datastore_url");

		Statement statement = null;
		Connection connection = null;
		try {
			LOGGER.debug("DatastoreURL: " + dataStoreURL);

			Class.forName("org.h2.Driver");
			this.cp = JdbcConnectionPool.create(dataStoreURL, "sa", "");

			connection = getConnection();
			statement = connection.createStatement();
			statement.execute("CREATE TABLE IF NOT EXISTS " + REQUEST_ID_TABLE_NAME
					+ "(" + REQUEST_ID+ " VARCHAR(255) PRIMARY KEY)");
			statement.close();

		} catch (Exception e) {
			LOGGER.error("Error while initializing the DataStore.", e);
		} finally {
			close(statement, connection);
		}
	}


	public Connection getConnection() throws SQLException {
		try {
			return cp.getConnection();
		} catch (SQLException e) {
			LOGGER.error("Error while getting a new connection from the connection pool.", e);
			throw e;
		}
	}

	private void close(Statement statement, Connection conn) {
		if (statement != null) {
			try {
				if (!statement.isClosed()) {
					statement.close();
				}
			} catch (SQLException e) {
				LOGGER.error("Couldn't close statement");
			}
		}

		if (conn != null) {
			try {
				if (!conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				LOGGER.error("Couldn't close connection");
			}
		}
	}



	private static final String INSERT_MEMBER_USAGE_SQL = "INSERT INTO " + REQUEST_ID_TABLE_NAME
			+ " VALUES(?)";

	private static final String DELETE_ALL_CONTENT_SQL = "DELETE FROM " + REQUEST_ID_TABLE_NAME;

	public boolean update(List<Order> orders){
		LOGGER.debug("Updating current list of Orders that were requested to provider");
		PreparedStatement deleteOldContent = null;
		PreparedStatement updateOrderList = null;
		Connection connection = null;

		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			deleteOldContent = connection.prepareStatement(DELETE_ALL_CONTENT_SQL);
			deleteOldContent.addBatch();
			if (hasBatchExecutionError(deleteOldContent.executeBatch())){
				connection.rollback();
				return false;
			}
			updateOrderList = prepare(connection, INSERT_MEMBER_USAGE_SQL);
			for (Order order : orders){
				updateOrderList.setString(1, order.getRequestId());
				updateOrderList.addBatch();
			}
			if (hasBatchExecutionError(updateOrderList.executeBatch())){
				connection.rollback();
				return false;
			}
			connection.commit();
			return true;
		} catch (SQLException e) {
			LOGGER.error("Couldn't store the current orders", e);
			try {
				if (connection != null) {
					connection.rollback();
				}
			} catch (SQLException e1) {
				LOGGER.error("Couldn't rollback transaction.", e1);
			}
			return false;
		} finally {
			close(updateOrderList, connection);
			close(deleteOldContent, connection);

		}
	}
	
	protected PreparedStatement prepare(Connection connection, String statement) throws SQLException {
		return connection.prepareStatement(statement);
	}

	private static final String SELECT_REQUEST_ID = "SELECT * FROM " + REQUEST_ID_TABLE_NAME;

	public List<String> getRequesId() {
		ArrayList<String> orders = new ArrayList<String>();
		Statement getRequestIdStatement = null;
		Connection connection = null;
		try {
			connection =  getConnection();
			getRequestIdStatement = connection.createStatement();
			getRequestIdStatement.execute(SELECT_REQUEST_ID);
			ResultSet result = getRequestIdStatement.getResultSet();
			while(result.next()){
				orders.add(result.getString(REQUEST_ID));
			}
			return orders;
		} catch (SQLException e){
			LOGGER.error("Couldn't recover request Ids from DB", e);	
			return null;
		} finally {
			close(getRequestIdStatement, connection);
		}
	}
	private boolean hasBatchExecutionError(int[] executeBatch) {
		for (int i : executeBatch) {
			if (i == PreparedStatement.EXECUTE_FAILED) {
				return true;
			}
		}
		return false;
	}


	public void dispose() {
		cp.dispose();
	}

}
