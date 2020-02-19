package org.fogbowcloud.saps.engine.core.scheduler.arrebol.retry;

import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.dtos.JobResponseDTO;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.Arrebol;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetJobException;

public class GetJobByIdRetry implements ArrebolRetry<JobResponseDTO> {

	private Arrebol arrebol;
	private String jobId;

	public GetJobByIdRetry(Arrebol arrebol, String jobId) {
		this.arrebol = arrebol;
		this.jobId = jobId;
	}

	@Override
	public JobResponseDTO run() throws GetJobException {
		return arrebol.checkStatusJobById(jobId);
	}
}
