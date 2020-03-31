package org.fogbowcloud.saps.engine.core.scheduler.executor;

import java.util.List;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.dtos.JobResponseDTO;
import org.fogbowcloud.saps.engine.core.model.SapsJob;

public interface JobExecutionService {

    String submit(SapsJob job) throws Exception;

    long getWaitingJobs() throws Exception;

    JobResponseDTO getJobById(String jobId) throws Exception;

    List<JobResponseDTO> getJobByLabel(String label) throws Exception;

}