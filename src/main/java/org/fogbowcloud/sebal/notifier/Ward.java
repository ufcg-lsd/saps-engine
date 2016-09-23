package org.fogbowcloud.sebal.notifier;

import org.fogbowcloud.sebal.engine.sebal.ImageState;

public class Ward {

	private final String imageName;
	private final ImageState targetState;
	private final String jobId;
	private final String email;

	public Ward(String imageName, ImageState targetState, String jobId,
			String email) {
		this.imageName = imageName;
		this.targetState = targetState;
		this.email = email;
		this.jobId = jobId;
	}

	@Override
	public String toString() {
		return "Ward [imageName=" + imageName + ", targetState="
				+ targetState + ", jobId=" + jobId + ", email=" + email
				+ "]";
	}

	public String getJobId() {
		return jobId;
	}

	public String getEmail() {
		return email;
	}

	public String getImageName() {
		return imageName;
	}

	public ImageState getTargetState() {
		return targetState;
	}

}
