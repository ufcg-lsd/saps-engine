package org.fogbowcloud.scheduler.core.model;

public class JDFJob extends Job{

	public String jobId = "0";
	
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	
	
	@Override
	public void run(Task task) {
		tasksReady.remove(task);
		tasksRunning.add(task);
		
	}

	@Override
	public void finish(Task task) {
		tasksRunning.remove(task);
		tasksCompleted.add(task);
	}

	@Override
	public void fail(Task task) {
		tasksRunning.remove(task);
		tasksFailed.add(task);		
	}


	public String getId() {
		return jobId;
	}

}
