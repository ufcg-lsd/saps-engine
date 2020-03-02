package org.fogbowcloud.saps.engine.core.scheduler.executor;

import java.util.List;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.dtos.JobResponseDTO;
import org.fogbowcloud.saps.engine.core.model.SapsJob;

public interface JobExecutionService {

    public String submit(SapsJob job) throws Exception;

    public long getWaitingJobs() throws Exception;

    public JobResponseDTO getJob(String jobId) throws Exception;

    public List<JobResponseDTO> getJobByLabel(String label) throws Exception;

}