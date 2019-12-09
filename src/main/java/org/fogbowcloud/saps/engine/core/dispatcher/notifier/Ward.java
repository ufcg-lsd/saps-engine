package org.fogbowcloud.saps.engine.core.dispatcher.notifier;

import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;

public class Ward {

	private final String submissionId;
	private final String taskId;
	private final ImageTaskState targetState;
	private final String email;

	public Ward(String submissionId, String taskId, ImageTaskState targetState, String email) {
		this.submissionId = submissionId;
		this.taskId = taskId;
		this.targetState = targetState;
		this.email = email;
	}

	@Override
	public String toString() {
		return "Ward [submissionId=" + submissionId + ", taskId=" + taskId + ", targetState="
				+ targetState + ", email=" + email + "]";
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
	public ImageTaskState getTargetState() {
		return targetState;
	}

}
