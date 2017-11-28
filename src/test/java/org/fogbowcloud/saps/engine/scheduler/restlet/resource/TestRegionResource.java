package org.fogbowcloud.saps.engine.scheduler.restlet.resource;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.scheduler.restlet.DatabaseApplication;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestRegionResource {

	private Properties properties;
	private DatabaseApplication DBA;

	@Before
	public void setUp() throws Exception {
		this.properties = new Properties();

		this.properties.put("saps_export_path", "src/test/resources");
		this.properties.put("saps_container_linked_path", "/home/ubuntu/");
		this.properties.put("datastore_url_prefix", "jdbc:h2:mem:testdb");
		this.properties.put("datastore_username", "testuser");
		this.properties.put("datastore_password", "testuser");
		this.properties.put("datastore_driver", "org.h2.Driver");
		this.properties.put("datastore_name", "testdb");

		this.properties.put("admin_email", "testuser");
		this.properties.put("admin_user", "testuser");
		this.properties.put("admin_password", "testuser");

		this.properties.put("submission_rest_server_port", "8000");

		this.properties.put("usgs_login_url", "fakelogin");
		this.properties.put("usgs_json_url", "fakeurl");
		this.properties.put("usgs_username", "username");
		this.properties.put("usgs_password", "password");
		this.properties.put("usgs_api_key_period", "100000");

		this.DBA = Mockito.spy(new DatabaseApplication(this.properties));
		this.DBA.startServer();
	}

	@After
	public void cleanUp() throws Exception {
		this.DBA.stopServer();
	}

	@Test
	public void testGetNumberImagesProcessedByRegion()
			throws SQLException, ClientProtocolException, IOException, JSONException {
		
		String userEmail = properties.getProperty("admin_email");
		SapsUser user = this.DBA.getUser(userEmail);
		if (user == null) {
			String userName = properties.getProperty("admin_user");
			String userPass = DigestUtils.md5Hex(properties.getProperty("admin_password"));

			this.DBA.createUser(userEmail, userName, userPass, true, false, true);

			this.DBA.createUser("anonymous", "anonymous", DigestUtils.md5Hex("pass"), true, false,
					false);
		}

		ImageDataStore imageDB = new JDBCImageDataStore(this.properties);

		Date date = new Date(10000854);
		String federationMember = "fake-fed-member";

		List<ImageTask> images = new ArrayList<ImageTask>();
		images.add(new ImageTask("task-id-1", "LT5", "215066", date, "link1",
				ImageTaskState.ARCHIVED, federationMember, 0, "NE", "NE", "pre_processing", "NE",
				"NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", ""));
		images.add(new ImageTask("task-id-2", "LT5", "215067", date, "link1",
				ImageTaskState.ARCHIVED, federationMember, 0, "NE", "NE", "pre_processing", "NE",
				"NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", ""));
		images.add(new ImageTask("task-id-3", "LT5", "215066", date, "link1",
				ImageTaskState.ARCHIVED, federationMember, 0, "NE", "NE", "pre_processing", "NE",
				"NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", ""));

		HashMap<String, Integer> expectedFrequency = new HashMap<String, Integer>();
		expectedFrequency.put("215066", 2);
		expectedFrequency.put("215067", 1);

		imageDB.addImageTask(images.get(0));
		imageDB.addImageTask(images.get(1));
		imageDB.addImageTask(images.get(2));

		HttpGet get = new HttpGet("http://localhost:8000/regions/details");
		get.addHeader(UserResource.REQUEST_ATTR_USER_EMAIL, "testuser");
		get.addHeader(UserResource.REQUEST_ATTR_USERPASS, "testuser");
		HttpClient client = HttpClients.createMinimal();

		HttpResponse response = client.execute(get);
		String responseStr = EntityUtils.toString(response.getEntity(),
				String.valueOf(Charsets.UTF_8));

		JSONArray responseJson = new JSONArray(responseStr);

		HashMap<String, Integer> actualFrequency = new HashMap<String, Integer>();
		for (int i = 0; i < responseJson.length(); i++) {
			JSONObject regionCount = responseJson.getJSONObject(i);
			actualFrequency.put(regionCount.getString("region"), regionCount.getInt("count"));
		}

		Assert.assertEquals(expectedFrequency, actualFrequency);
	}

}
