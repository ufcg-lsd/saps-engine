package org.fogbowcloud.saps.engine.core.scheduler.arrebol;

import java.sql.SQLException;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.dto.JobResponseDTO;
import org.fogbowcloud.saps.engine.core.job.SapsJob;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetJobException;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.SubmitJobException;

public class DefaultArrebol implements Arrebol{
	
	private final ArrebolRequestsHelper arrebolRequestHelper;
	//private final JDBCJobDataStore jobDataStore;
	private final Properties properties;
	private List<JobSubmitted> submittedJobID;

	public DefaultArrebol(Properties properties) throws SQLException {
		this.properties = properties;
		this.arrebolRequestHelper = new ArrebolRequestsHelper(properties);
		//this.jobDataStore = new JDBCJobDataStore(properties);
		this.submittedJobID = new LinkedList<JobSubmitted>();
	}

	@Override
	public String addJob(SapsJob job) throws Exception, SubmitJobException {
		return arrebolRequestHelper.submitJobToExecution(job);
	}

	@Override
	public void removeJob(JobSubmitted job){
		submittedJobID.remove(job);
	}
	
	@Override
	public void addJobInList(JobSubmitted newJob){
		submittedJobID.add(newJob);
	}

	@Override
	public List<JobSubmitted> returnAllJobsSubmitted(){
		return submittedJobID;
	}

	@Override
	public JobResponseDTO checkStatusJob(String jobId) throws GetJobException{
		return arrebolRequestHelper.getJob(jobId);
	}

	@Override
	public String checkStatusJobString(String jobId) throws GetJobException{
		return arrebolRequestHelper.getJobJSON(jobId);
	}

	@Override
	public int getCountSlotsInQueue() throws Exception, SubmitJobException {
		return arrebolRequestHelper.getCountSlotsInQueue();
	}
}
