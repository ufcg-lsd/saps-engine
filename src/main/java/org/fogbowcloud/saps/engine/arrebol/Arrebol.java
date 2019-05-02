package org.fogbowcloud.saps.engine.arrebol;

import java.sql.SQLException;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.database.JDBCJobDataStore;
import org.fogbowcloud.saps.engine.core.job.SapsJob;
import org.fogbowcloud.saps.engine.exceptions.SubmitJobException;

public class Arrebol {
	
	private final ArrebolRequestsHelper arrebolRequestHelper;
	private final JDBCJobDataStore jobDataStore;
	private final Properties properties;
	
	public Arrebol(Properties properties) throws SQLException {
		this.properties = properties;
		this.arrebolRequestHelper = new ArrebolRequestsHelper(properties);
		this.jobDataStore = new JDBCJobDataStore(properties);
	}
	
	public String addJob(SapsJob job) throws Exception, SubmitJobException {
		return arrebolRequestHelper.submitJobToExecution(job);
	}
	
	
}
