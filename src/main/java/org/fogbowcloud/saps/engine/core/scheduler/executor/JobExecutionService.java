package org.fogbowcloud.saps.engine.core.scheduler.executor;

import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.dtos.JobResponseDTO;
import org.fogbowcloud.saps.engine.core.model.SapsJob;

public interface JobExecutionService {

    public String submit(SapsJob job) throws Exception;

    public long getWaitingJobs() throws Exception;

    public JobResponseDTO getStatus(String jobId) throws Exception;

}