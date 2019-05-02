package org.fogbowcloud.saps.engine.core.task;

public enum TaskState {

	READY("Ready"), RUNNING("Running"), FINISHED("Finished"), COMPLETED("Completed"), FAILED("Failed"),
	NOT_CREATED("Not Created"), TIMEDOUT("Timedout");

	private String desc;

	TaskState(String desc) {
		this.desc = desc;
	}

	public String getDesc() {
		return this.desc;
	}

	public static TaskState getTaskStateFromDesc(String desc) {
		for (TaskState ts : values()) {
			if (ts.getDesc().equals(desc)) {
				return ts;
			}
		}
		return null;
	}
}