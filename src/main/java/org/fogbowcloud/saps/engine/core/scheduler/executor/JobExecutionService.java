package org.fogbowcloud.saps.engine.core.scheduler.executor;

import org.fogbowcloud.saps.engine.core.model.SapsJob;

public interface JobExecutionService {

    public String submit(SapsJob job);

    public void getStatus(String jobId);

}