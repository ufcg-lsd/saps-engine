package org.fogbowcloud.scheduler.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Order;
import org.h2.jdbcx.JdbcConnectionPool;

public class DataStore {
	

	private static final Logger LOGGER = Logger.getLogger(DataStore.class);

	protected final String ORDER_TABLE_NAME = "orders";
	
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
			statement.execute("CREATE TABLE IF NOT EXISTS " + ORDER_TABLE_NAME
					+ "(requestid VARCHAR(255) PRIMARY KEY)");
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
	
	public void update(List<Order> orders){
		
	}
	
	public List<Order> getOrders() {
		return null;
	}
}
