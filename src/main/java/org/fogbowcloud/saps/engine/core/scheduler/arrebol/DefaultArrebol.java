package org.fogbowcloud.saps.engine.arrebol;

import java.sql.SQLException;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.dto.JobResponseDTO;
import org.fogbowcloud.saps.engine.core.job.SapsJob;
import org.fogbowcloud.saps.engine.exceptions.GetJobException;
import org.fogbowcloud.saps.engine.exceptions.SubmitJobException;

public class Arrebol {
	
	private final ArrebolRequestsHelper arrebolRequestHelper;
	//private final JDBCJobDataStore jobDataStore;
	private final Properties properties;
	private List<JobSubmitted> submittedJobID;

	public Arrebol(Properties properties) throws SQLException {
		this.properties = properties;
		this.arrebolRequestHelper = new ArrebolRequestsHelper(properties);
		//this.jobDataStore = new JDBCJobDataStore(properties);
		this.submittedJobID = new LinkedList<JobSubmitted>();
	}
	
	public String addJob(SapsJob job) throws Exception, SubmitJobException {
		return arrebolRequestHelper.submitJobToExecution(job);
	}

	public void removeJob(JobSubmitted job){
		submittedJobID.remove(job);
	}
	
	public void addJobInList(JobSubmitted newJob){
		submittedJobID.add(newJob);
	}

	public List<JobSubmitted> returnAllJobsSubmitted(){
		return submittedJobID;
	}

	public JobResponseDTO checkStatusJob(String jobId) throws GetJobException{
		return arrebolRequestHelper.getJob(jobId);
	}

	public String checkStatusJobString(String jobId) throws GetJobException{
		return arrebolRequestHelper.getJobJSON(jobId);
	}
}
