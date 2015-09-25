package org.fogbowcloud.scheduler.core.model;

public class Task {

	private boolean isFinished;
	
	public String getRequirements() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Task clone() {
		// TODO Auto-generated method stub
		return new Task();
	}

	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void finish(){
		this.isFinished = true;
	}

	public boolean isFinished() {
		return this.isFinished;
	}
}
