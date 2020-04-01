package org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.request;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.google.gson.JsonObject;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.dtos.JobRequestDTO;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.dtos.JobResponseDTO;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.exceptions.ArrebolConnectException;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.models.ArrebolQueue;
import org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.http.HttpWrapper;

import java.util.LinkedList;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// TODO implement tests
public class ArrebolRequestsHelper {

	private static final Logger LOGGER = Logger.getLogger(ArrebolRequestsHelper.class);
	private final String arrebolBaseUrl;
	private final Gson gson;

	private static class Endpoint {
		static final String QUEUES = "%s/queues";
		static final String QUEUE = QUEUES + "/%s";
		static final String JOBS = QUEUE + "/jobs";
		static final String JOB = JOBS + "/%s";
	}
	private static class JsonKey {
		static final String JOB_ID = "id";
	}


	public ArrebolRequestsHelper(String arrebolBaseUrl) {
		if(Objects.isNull(arrebolBaseUrl) || arrebolBaseUrl.isEmpty()) {
			throw new IllegalArgumentException("Arrebol Base Url cannot be null or empty");
		}
		this.arrebolBaseUrl = arrebolBaseUrl;
		this.gson = new GsonBuilder().create();
	}

	public String submitJobToExecution(String queueId, JobRequestDTO job) throws IOException {
		StringEntity requestBody;

		try {
			requestBody = makeJSONBody(job);
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedEncodingException("Job [" + job.getLabel() + "] is not well formed to built JSON.");
		}

		final String endpoint = String.format(Endpoint.JOBS, this.arrebolBaseUrl, queueId);
		
		String jobId;

		try {
			final String jsonResponse = HttpWrapper.doRequest(HttpPost.METHOD_NAME, endpoint,
					new LinkedList<>(), requestBody);

			JsonObject jobResponse = this.gson.fromJson(jsonResponse, JsonObject.class);

			jobId = jobResponse.get(JsonKey.JOB_ID).getAsString();

			LOGGER.info("Job was submitted with success to Arrebol.");
		} catch (IOException e) {
			throw new IOException("Submit Job to Arrebol has FAILED: " + e.getMessage(), e);
		}

		return jobId;
	}

	public JobResponseDTO getJob(String queueId, String jobId) throws IOException {
		final String endpoint = String.format(Endpoint.JOB, this.arrebolBaseUrl, queueId, jobId);

		JobResponseDTO jobResponse;
		try {
			String jsonResponse = HttpWrapper.doRequest(HttpGet.METHOD_NAME, endpoint);
			jobResponse = gson.fromJson(jsonResponse, JobResponseDTO.class);
		} catch (Exception e) {
			throw new IOException("Get Job from Arrebol has FAILED: " + e.getMessage(), e);
		}
		return jobResponse;
	}

	private StringEntity makeJSONBody(JobRequestDTO job) throws UnsupportedEncodingException {
		LOGGER.info("Building JSON body of Job ...");

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(job);

		LOGGER.debug("JSON body: " + json);

		return new StringEntity(json);
	}

	public ArrebolQueue getQueue(String queueId) throws IOException {
		final String endpoint = String.format(Endpoint.QUEUE, arrebolBaseUrl, queueId);

		ArrebolQueue queue;
		try {
			final String jsonResponse = HttpWrapper.doRequest(HttpGet.METHOD_NAME, endpoint, null);
			queue = this.gson.fromJson(jsonResponse, ArrebolQueue.class);
		} catch (IOException e) {
			throw new IOException("Get waiting jobs from Arrebol Queue [" + queueId + "] has FAILED: " + e.getMessage(), e);
		}

		return queue;
	}
}