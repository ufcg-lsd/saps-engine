package org.fogbowcloud.saps.engine.core.scheduler.executor;

import java.io.IOException;
import java.util.List;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.dtos.JobResponseDTO;
import org.fogbowcloud.saps.engine.core.model.SapsJob;

public interface JobExecutionService {

    String submit(SapsJob job) throws IOException;

    long getWaitingJobs() throws IOException;

    JobResponseDTO getJobById(String jobId) throws IOException;

    List<JobResponseDTO> getJobByLabel(String label) throws IOException;

}