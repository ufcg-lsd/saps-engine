package org.fogbowcloud.saps.engine.core.dispatcher.email;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dispatcher.email.keystone.KeystoneV3IdentityRequestHelper;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.DatabaseApplication;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ProcessedImagesEmailBuilder implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(ProcessedImagesEmailBuilder.class);

	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
	private static final String UNAVAILABLE = "UNAVAILABLE";
	private static final String TEMP_DIR_URL = "%s?temp_url_sig=%s&temp_url_expires=%s";
	private static final String TASK_ID = "taskId";
	private static final String REGION = "region";
	private static final String COLLECTION_TIER_NAME = "collectionTierName";
	private static final String IMAGE_DATE = "imageDate";
	private static final String NAME = "name";
	private static final String URL = "url";
	private static final String INPUTDOWNLOADING = "inputdownloading";
	private static final String PREPROCESSING = "preprocessing";
	private static final String PROCESSING = "processing";
	private static final String STATUS = "status";

	private DatabaseApplication application;
	private Properties properties;
	private String userEmail;
	private List<String> images;

	public ProcessedImagesEmailBuilder(DatabaseApplication databaseApplication, Properties properties, String userEmail,
			List<String> images) {
		this.application = databaseApplication;
		this.properties = properties;
		this.userEmail = userEmail;
		this.images = images;
	}

	@Override
	public void run() {
		StringBuilder builder = new StringBuilder();
		builder.append("Creating email for user [" + userEmail + " with images:\n");
		for (String str : images) {
			builder.append(str).append("\n");
		}
		LOGGER.info(builder.toString());

		StringBuilder errorBuilder = new StringBuilder();
		JSONArray tasklist = generateAllTasksJsons(errorBuilder);
		sendTaskEmail(errorBuilder, tasklist);
		sendErrorEmail(errorBuilder);
	}

	private JSONArray generateAllTasksJsons(StringBuilder errorBuilder) {
		JSONArray tasklist = new JSONArray();
		for (String taskId : images) {
			try {
				tasklist.put(generateTaskEmailJson(taskId));
			} catch (IOException | URISyntaxException | JSONException e) {
				LOGGER.error("Failed to generate task email json from task [" + taskId + "]", e);
				errorBuilder.append("Failed to generate task email json from task [" + taskId + "]").append("\n")
						.append(ExceptionUtils.getStackTrace(e)).append("\n");
				try {
					JSONObject task = new JSONObject();
					task.put(TASK_ID, taskId);
					task.put(STATUS, UNAVAILABLE);
					tasklist.put(task);
				} catch (JSONException je) {
					LOGGER.error("Failed to create UNAVAILABLE task json.", je);
				}
			}
		}
		return tasklist;
	}

	private void sendTaskEmail(StringBuilder errorBuilder, JSONArray tasklist) {
		try {
			GoogleMail.Send(properties.getProperty(SapsPropertiesConstants.NO_REPLY_EMAIL),
					properties.getProperty(SapsPropertiesConstants.NO_REPLY_PASS), userEmail, "[SAPS] Filter results",
					tasklist.toString(2));
		} catch (MessagingException | JSONException e) {
			LOGGER.error("Failed to send email with images download links.", e);
			errorBuilder.append("Failed to send email with images download links.").append("\n")
					.append(ExceptionUtils.getStackTrace(e)).append("\n");
		}
	}

	private void sendErrorEmail(StringBuilder errorBuilder) {
		if (!errorBuilder.toString().isEmpty()) {
			try {
				GoogleMail.Send(properties.getProperty(SapsPropertiesConstants.NO_REPLY_EMAIL),
						properties.getProperty(SapsPropertiesConstants.NO_REPLY_PASS), "sebal.no.reply@gmail.com",
						"[SAPS] Errors during image temporary link creation", errorBuilder.toString());
			} catch (MessagingException e) {
				LOGGER.error("Failed to send email with errors to admins.", e);
			}
		}
	}

	private String toHexString(byte[] bytes) {
		Formatter formatter = new Formatter();

		for (byte b : bytes) {
			formatter.format("%02x", b);
		}

		String hexString = formatter.toString();
		formatter.close();

		return hexString;
	}

	private String calculateRFC2104HMAC(String data, String key)
			throws NoSuchAlgorithmException, InvalidKeyException {
		SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
		Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
		mac.init(signingKey);
		return toHexString(mac.doFinal(data.getBytes()));
	}

	private String generateTempURL(String swiftPath, String container, String filePath, String key)
			throws NoSuchAlgorithmException, InvalidKeyException {
		String path = swiftPath + File.separator + container + File.separator + filePath;

		Formatter objectStoreFormatter = new Formatter();
		objectStoreFormatter.format("%s\n%s\n%s", "GET", Long.MAX_VALUE, path);
		String signature = calculateRFC2104HMAC(objectStoreFormatter.toString(), key);
		objectStoreFormatter.close();

		objectStoreFormatter = new Formatter();
		objectStoreFormatter.format(TEMP_DIR_URL, path, signature, Long.MAX_VALUE);
		String res = objectStoreFormatter.toString();
		objectStoreFormatter.close();

		return res;
	}

	private JSONObject generateTaskEmailJson(String taskId)
		throws JSONException, IOException, URISyntaxException {
		SapsImage task = application.getTask(taskId);
		LOGGER.info("Task [" + taskId + "] : " + task);

		LOGGER.info("Creating JSON representation for task [" + taskId + "]");

		JSONObject result = new JSONObject();

		result.put(TASK_ID, task.getTaskId());
		result.put(REGION, task.getRegion());
		result.put(COLLECTION_TIER_NAME, task.getCollectionTierName());
		result.put(IMAGE_DATE, task.getImageDate());

		prepareTaskDir(result, task, INPUTDOWNLOADING);
		prepareTaskDir(result, task, PREPROCESSING);
		prepareTaskDir(result, task, PROCESSING);

		return result;
	}

	private void prepareTaskDir(JSONObject result, SapsImage task, String directory)
		throws JSONException, IOException, URISyntaxException {

		String objectStoreHost = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_HOST);
		String objectStorePath = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_PATH);
		String objectStoreContainer = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_CONTAINER);
		String objectStoreKey = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_KEY);
		String swiftPrefixDir = properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR);

		List<String> files = getTaskFilesFromObjectStore(objectStoreHost, objectStorePath, objectStoreContainer,
				swiftPrefixDir, directory, task);

		JSONArray dirFileList = new JSONArray();
		for (String str : files) {
			File f = new File(str);
			String fileName = f.getName();
			try {
				JSONObject fileObject = new JSONObject();
				fileObject.put(NAME, fileName);
				fileObject.put(URL,
						"https://" + objectStoreHost
								+ generateTempURL(objectStorePath, objectStoreContainer, str, objectStoreKey)
								+ "&filename=" + fileName);
				dirFileList.put(fileObject);
			} catch (NoSuchAlgorithmException | InvalidKeyException e) {
				LOGGER.error("Failed to generate download link for file " + str, e);
				try {
					JSONObject fileObject = new JSONObject();
					fileObject.put(NAME, fileName);
					fileObject.put(URL, UNAVAILABLE);
					dirFileList.put(fileObject);
				} catch (JSONException e1) {
					LOGGER.error("Failed to create UNAVAILABLE temp url json.", e);
				}
			}
		}
		result.put(directory, dirFileList);

	}

	private List<String> getTaskFilesFromObjectStore(String objectStoreHost, String objectStorePath,
			String objectStoreContainer, String swiftPrefixDir, String taskDir, SapsImage task)
		throws IOException, JSONException, URISyntaxException {

		String accessId = getKeystoneAccessId();

		HttpClient client = HttpClients.createDefault();
		HttpGet httpget = prepObjectStoreRequest(objectStoreHost, objectStorePath, objectStoreContainer,
				swiftPrefixDir, taskDir, task, accessId);
		HttpResponse response = client.execute(httpget);

		return Arrays.asList(EntityUtils.toString(response.getEntity()).split("\n"));
	}

	private String getKeystoneAccessId() throws IOException, JSONException {

		Map<String, String> credentials = new HashMap<>();
		credentials.put(KeystoneV3IdentityRequestHelper.AUTH_URL, properties.getProperty(SapsPropertiesConstants.SWIFT_AUTH_URL));
		credentials.put(KeystoneV3IdentityRequestHelper.PROJECT_ID, properties.getProperty(SapsPropertiesConstants.SWIFT_PROJECT_ID));
		credentials.put(KeystoneV3IdentityRequestHelper.USER_ID, properties.getProperty(SapsPropertiesConstants.SWIFT_USER_ID));
		credentials.put(KeystoneV3IdentityRequestHelper.PASSWORD, properties.getProperty(SapsPropertiesConstants.SWIFT_PASSWORD));
		return KeystoneV3IdentityRequestHelper.createAccessId(credentials);
	}

	private HttpGet prepObjectStoreRequest(String objectStoreHost, String objectStorePath, String objectStoreContainer,
			String swiftPrefixDir, String taskDir, SapsImage task, String accessId) throws URISyntaxException {
		String uriParameter = swiftPrefixDir + File.separator + task.getTaskId() + File.separator + taskDir
				+ File.separator;

		LOGGER.info("Build URI: objectStorehost [" + objectStoreHost + "], objectStorePath [" + objectStorePath
				+ "], objectStoreContainer [" + objectStoreContainer + "], parameter [" + uriParameter + "] token ["
				+ accessId + "] and task [" + task + "]");

		URI uri = new URIBuilder().setScheme("https").setHost(objectStoreHost)
				.setPath(objectStorePath + "/" + objectStoreContainer).addParameter("path", uriParameter).build();

		LOGGER.info("Getting list of files for task [" + task.getTaskId() + "] from " + uri);

		HttpGet httpget = new HttpGet(uri);
		httpget.addHeader("X-Auth-Token", accessId);

		return httpget;
	}
}
