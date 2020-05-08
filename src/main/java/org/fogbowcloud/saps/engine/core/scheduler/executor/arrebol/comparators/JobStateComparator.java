package org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.comparators;

import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.dtos.JobResponseDTO;

import java.util.Comparator;

public class JobStateComparator implements Comparator<JobResponseDTO> {

    private static class StateWeight {
        static final Integer SUBMITTED = 1;
        static final Integer QUEUED = 2;
        static final Integer RUNNING = 3;
        static final Integer FAILED = 4;
        static final Integer FINISHED = 5;
    }

    @Override
    public int compare(JobResponseDTO j1, JobResponseDTO j2) {
        return getStateWeight(j1.getJobState()) - getStateWeight(j2.getJobState());
    }

    private Integer getStateWeight(String state) {
        switch (state){
            case "SUBMITTED":
                return StateWeight.SUBMITTED;
            case "QUEUED":
                return StateWeight.QUEUED;
            case "RUNNING":
                return StateWeight.RUNNING;
            case "FAILED":
                return StateWeight.FAILED;
            case "FINISHED":
                return StateWeight.FINISHED;
            default:
                return -1;
        }
    }
}
