package org.fogbowcloud.saps.notifier;

import org.fogbowcloud.saps.engine.core.model.ImageTaskState;

public class Ward {

	private final String imageName;
	private final ImageTaskState targetState;
	private final String submissionId;
	private final String taskId;
	private final String email;

	public Ward(String imageName, ImageTaskState targetState, String submissionId, String taskId,
			String email) {
		this.imageName = imageName;
		this.targetState = targetState;
		this.email = email;
		this.submissionId = submissionId;
		this.taskId = taskId;
	}

	@Override
	public String toString() {
		return "Ward [imageName=" + imageName + ", targetState=" + targetState + ", submissionId="
				+ submissionId + ", taskId=" + taskId + ", email=" + email + "]";
	}

	public String getSubmissionId() {
		return submissionId;
	}
	
	public String getTaskId() {
		return taskId;
	}

	public String getEmail() {
		return email;
	}

	public String getImageName() {
		return imageName;
	}

	public ImageTaskState getTargetState() {
		return targetState;
	}

}
