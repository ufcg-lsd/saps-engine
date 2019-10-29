package org.fogbowcloud.saps.engine.core.scheduler.retry.arrebol;

import java.util.List;

import org.fogbowcloud.saps.engine.core.dto.JobResponseDTO;
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
