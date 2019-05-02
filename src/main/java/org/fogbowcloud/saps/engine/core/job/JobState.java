package org.fogbowcloud.saps.engine.core.job;

public enum JobState {
	SUBMITTED("Submitted"), FAILED("Failed"), CREATED("Created"), FINISHED("Finished"), RUNNING("Running");;

	private String desc;

	JobState(String desc) {
		this.desc = desc;
	}

	public String value() {
		return this.desc;
	}

	public static JobState create(String desc) throws Exception {
		for (JobState ts : values()) {
			if (ts.value().equals(desc)) {
				return ts;
			}
		}
		throw new Exception("Invalid job state");
	}
}