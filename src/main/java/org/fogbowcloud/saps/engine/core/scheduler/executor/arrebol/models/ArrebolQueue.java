package org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class ArrebolQueue {

    public String id;

    public String name;

    @JsonProperty("waiting_jobs")
    @SerializedName("waiting_jobs")
    private long waitingJobs;

    @JsonProperty("nodes")
    @SerializedName("worker_pools")
    private Integer workerPools;

    @JsonProperty("workers")
    @SerializedName("pools_size")
    private Integer poolsSize;

    public ArrebolQueue(String id, String name, long waitingJobs, int workerPools, int poolsSize) {
        this.id = id;
        this.name = name;
        this.waitingJobs = waitingJobs;
        this.workerPools = workerPools;
        this.poolsSize = poolsSize;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getWaitingJobs() {
        return waitingJobs;
    }
}
