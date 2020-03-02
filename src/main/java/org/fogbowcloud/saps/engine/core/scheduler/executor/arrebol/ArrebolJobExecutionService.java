package org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol;

import java.util.List;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.model.SapsJob;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.exceptions.GetJobException;
import org.fogbowcloud.saps.engine.core.scheduler.executor.JobExecutionService;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.dtos.JobRequestDTO;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.dtos.JobResponseDTO;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.request.ArrebolRequestsHelper;

public class ArrebolJobExecutionService implements JobExecutionService {

    private static final Logger LOGGER = Logger.getLogger(ArrebolJobExecutionService.class);
    private static final String DEFAULT_QUEUE = "default";
    private final ArrebolRequestsHelper requestsHelper;

    public ArrebolJobExecutionService(ArrebolRequestsHelper requestsHelper) {
        this.requestsHelper = requestsHelper;
    }

    @Override
    public String submit(SapsJob job) throws Exception {
        LOGGER.info("Submitting Saps Job [" + job.getName() + "] to Arrebol");
        JobRequestDTO jobRequestDTO = new JobRequestDTO(job);
        String id = requestsHelper.submitJobToExecution(DEFAULT_QUEUE, jobRequestDTO);
        return id;
    }

    @Override
    public long getWaitingJobs() throws Exception {
        return requestsHelper.getQueue(DEFAULT_QUEUE).getWaitingJobs();
    }

    @Override
    public JobResponseDTO getJob(String jobId) throws GetJobException {
        LOGGER.info("Getting Job [" + jobId + "] from Arrebol");
        return requestsHelper.getJob(DEFAULT_QUEUE, jobId);
    }

    @Override
    public List<JobResponseDTO> getJobByLabel(String label) throws Exception {
        //FIXME Not yet available in the Arrebol Api
        return null;
    }

}
