package org.fogbowcloud.saps.engine.arrebol;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import org.apache.http.Header;

import com.google.gson.JsonObject;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.infrastructure.http.HttpWrapper;
import org.fogbowcloud.saps.engine.core.dto.JobDTO;
import org.fogbowcloud.saps.engine.core.job.Job;
import org.fogbowcloud.saps.engine.core.job.SapsJob;
import org.fogbowcloud.saps.engine.exceptions.GetJobException;
import org.fogbowcloud.saps.engine.exceptions.SubmitJobException;

import java.util.LinkedList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// TODO implement tests
public class ArrebolRequestsHelper {

	// TODO review this names
	private final Properties properties;
	private final String arrebolBaseUrl;
	private final Gson gson;

	private static final Logger LOGGER = Logger.getLogger(ArrebolRequestsHelper.class);

	public ArrebolRequestsHelper(Properties properties) {
		this.properties = properties;
		this.arrebolBaseUrl = this.properties.getProperty(SapsPropertiesConstants.ARREBOL_BASE_URL);
		this.gson = new Gson();
	}

	public String submitJobToExecution(SapsJob job) throws Exception, SubmitJobException {
		StringEntity requestBody;

		try {
			requestBody = makeJSONBody(job);
		} catch (UnsupportedEncodingException e) {
			throw new Exception("Job with id [" + job.getId() + "] is not well formed to built JSON.");
		}

		final String jobEndpoint = this.arrebolBaseUrl + "/job";

		String jobIdArrebol;
		final String JSON_KEY_JOB_ID_ARREBOL = "id";

		try {
			final String jsonResponse = HttpWrapper.doRequest(HttpPost.METHOD_NAME, jobEndpoint,
                    new LinkedList<Header>(), requestBody);

			JsonObject jobResponse = this.gson.fromJson(jsonResponse, JsonObject.class);

			jobIdArrebol = jobResponse.get(JSON_KEY_JOB_ID_ARREBOL).getAsString();

			LOGGER.info("Job [" + job.getId() + "] was submitted with success to Arrebol.");

		} catch (Exception e) {
			throw new SubmitJobException("Submit Job to Arrebol has FAILED: " + e.getMessage(), e);
		}

		return jobIdArrebol;
	}

	public JobDTO getJob(String jobArrebolId) throws GetJobException {
		final String endpoint = this.arrebolBaseUrl + "/job/" + jobArrebolId;

		String jsonResponse;
		try {
			jsonResponse = HttpWrapper.doRequest(HttpGet.METHOD_NAME, endpoint, null);
		} catch (Exception e) {
			throw new GetJobException("Get Job from Arrebol has FAILED: " + e.getMessage(), e);
		}

		return this.gson.fromJson(jsonResponse, JobDTO.class);
	}

	public String getJobJSON(String jobArrebolId) throws GetJobException {
		final String endpoint = this.arrebolBaseUrl + "/job/" + jobArrebolId;

		String jsonResponse;
		try {
			jsonResponse = HttpWrapper.doRequest(HttpGet.METHOD_NAME, endpoint, null);
		} catch (Exception e) {
			throw new GetJobException("Get Job from Arrebol has FAILED: " + e.getMessage(), e);
		}

		return jsonResponse;
	}

	public StringEntity makeJSONBody(SapsJob job) throws UnsupportedEncodingException {
		LOGGER.info("Building JSON body of Job : [" + job.getId() + "]");

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JobDTO jobDTO = new JobDTO(job);
		String json = gson.toJson(jobDTO);

		return new StringEntity(json);
	}
}