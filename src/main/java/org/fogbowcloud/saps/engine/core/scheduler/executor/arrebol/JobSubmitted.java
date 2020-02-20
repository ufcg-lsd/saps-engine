package org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol;

import org.fogbowcloud.saps.engine.core.model.SapsImage;

public class JobSubmitted {

	private String jobId;
	private SapsImage imageTask;

	public JobSubmitted(String jobId, SapsImage imageTask) {
		this.jobId = jobId;
		this.imageTask = imageTask;
	}

	public String getJobId() {
		return jobId;
	}

	public SapsImage getImageTask() {
		return imageTask;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		JobSubmitted otherJob = (JobSubmitted) o;

		if (this.getJobId() == otherJob.getJobId())
			return true;
		else
			return false;
	}

	@Override
	public String toString() {
		return jobId;
	}

}
