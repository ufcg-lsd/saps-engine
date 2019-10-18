package org.fogbowcloud.saps.engine.core.scheduler.arrebol;

import org.fogbowcloud.saps.engine.core.model.ImageTask;

public class JobSubmitted {

    private String jobId;
    private ImageTask imageTask;

    public JobSubmitted(String jobId, ImageTask imageTask){
        this.jobId = jobId;
        this.imageTask = imageTask;
    }

    public String getJobId(){
        return jobId;
    }

    public ImageTask getImageTask(){
        return imageTask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        JobSubmitted otherJob = (JobSubmitted) o;

        if(this.getJobId() == otherJob.getJobId())
            return true;
        else
            return false;
    }
}
