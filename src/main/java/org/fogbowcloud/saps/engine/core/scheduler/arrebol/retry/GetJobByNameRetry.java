package org.fogbowcloud.saps.engine.core.scheduler.retry.arrebol;

import java.util.List;

import org.fogbowcloud.saps.engine.core.dto.JobResponseDTO;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.Arrebol;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetJobException;

public class GetJobByNameRetry implements ArrebolRetry<List<JobResponseDTO>> {

	private Arrebol arrebol;
	private String jobName;

	public GetJobByNameRetry(Arrebol arrebol, String jobName) {
		this.arrebol = arrebol;
		this.jobName = jobName;
	}

	@Override
	public List<JobResponseDTO> run() throws GetJobException {
		return arrebol.checkStatusJobByName(jobName);
	}

}
