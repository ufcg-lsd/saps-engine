package org.fogbowcloud.scheduler.restlet;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.scheduler.core.model.JDFJob;
import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class TestJDFSchedulerAppClientSide {

	private static final String FAKE_TASK_ID = "TaskId";

	private static final String FAKE_ID = "FakeId";

	private static final String FAKE_JOB_ID = FAKE_ID;
	
	private static final String SCHED_PATH = "root";
	
	private static final String FAKE_FILEPATH = "fakeabsolutepathtojdffile";

	public String URI_JOB_REQUEST; 
	
	public String URI_TASK_REQUEST;


	public JDFSchedulerApplication jdfApp;
	Scheduler scheduler;

	public Properties properties;

	String jdfFile = "/fakeabsolutepathtojdffile";

	String fakeId = "/FakeId";
	
	String fakeRoot = "/root";
	
	String fakeName = "/fakeName";

	int serverPort;


	@Before
	public void setup() throws Exception {

		scheduler = mock(Scheduler.class);

		serverPort = getAvailablePort();

		Properties properties = new Properties();
		properties.setProperty(AppPropertiesConstants.REST_SERVER_PORT, Integer.toString(serverPort));

		URI_JOB_REQUEST  = "http://localhost:" + serverPort + "/" + "sebal-scheduler/job";
		
		URI_TASK_REQUEST = "http://localhost:" + serverPort + "/" + "sebal-scheduler/task/";


		jdfApp = spy(new JDFSchedulerApplication(scheduler, properties));

		jdfApp.startServer();
	}

	@Test
	public void testGetRequestContent() throws URISyntaxException, HttpException, IOException {

		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();

		JDFJob fakeJob = mock(JDFJob.class);

		doReturn(FAKE_JOB_ID).when(fakeJob).getId();

		Task fakeTask = mock(Task.class);
		doReturn(FAKE_TASK_ID).when(fakeTask).getId();

		ArrayList<Task> fakeTaskList = new ArrayList<Task>();

		fakeTaskList.add(fakeTask);

		jobList.add(fakeJob);

		doReturn(fakeTaskList).when(fakeJob).getByState(TaskState.READY);

		doReturn(jobList).when(scheduler).getJobs();
		HttpGet get = new HttpGet(URI_JOB_REQUEST);

		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		Assert.assertTrue(response.getFirstHeader("Content-type").getValue()
				.startsWith("text/plain"));

		response.getEntity().writeTo(System.out);

		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

	}

	@Test
	public void testGetSpecificJobContent() throws ClientProtocolException, IOException {

		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();

		JDFJob fakeJob = mock(JDFJob.class);

		doReturn(FAKE_JOB_ID).when(fakeJob).getId();

		Task fakeTask = mock(Task.class);
		doReturn(FAKE_TASK_ID).when(fakeTask).getId();

		ArrayList<Task> fakeTaskList = new ArrayList<Task>();

		ArrayList<Task> emptyList = new ArrayList<Task>();

		fakeTaskList.add(fakeTask);

		jobList.add(fakeJob);

		doReturn(fakeTaskList).when(fakeJob).getByState(TaskState.READY);

		doReturn(emptyList).when(fakeJob).getByState(TaskState.RUNNING);

		doReturn(emptyList).when(fakeJob).getByState(TaskState.FAILED);

		doReturn(emptyList).when(fakeJob).getByState(TaskState.COMPLETED);

		doReturn(fakeJob).when(scheduler).getJobById(FAKE_JOB_ID);
		HttpGet get = new HttpGet(URI_JOB_REQUEST+fakeId);

		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		Assert.assertTrue(response.getFirstHeader("Content-type").getValue()
				.startsWith("text/plain"));

		response.getEntity().writeTo(System.out);

		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());


	}

	@Test
	public void testPostNewJob() throws ClientProtocolException, IOException{

		Task fakeTask = mock(Task.class);

		ArrayList<Task> fakeTaskList = new ArrayList<Task>();

		fakeTaskList.add(fakeTask);

		doReturn(fakeTaskList).when(jdfApp).getTasksFromJDFFile(any(String.class), eq(jdfFile), eq(SCHED_PATH), eq(this.properties));


		HttpPost post = new HttpPost(URI_JOB_REQUEST + jdfFile+fakeRoot);

		doNothing().when(scheduler).addJob(any(JDFJob.class));

		HttpClient client = HttpClients.createMinimal();
		client.execute(post);	

		verify(scheduler).addJob(any(JDFJob.class));
	}
	
	@Test
	public void testPostNewNameJob() throws ClientProtocolException, IOException{

		Task fakeTask = mock(Task.class);

		ArrayList<Task> fakeTaskList = new ArrayList<Task>();

		fakeTaskList.add(fakeTask);

		doReturn(fakeTaskList).when(jdfApp).getTasksFromJDFFile(any(String.class), eq(FAKE_FILEPATH), eq(SCHED_PATH), eq(this.properties));


		HttpPost post = new HttpPost(URI_JOB_REQUEST + jdfFile+fakeRoot+fakeName);

		doNothing().when(scheduler).addJob(any(JDFJob.class));

		HttpClient client = HttpClients.createMinimal();
		client.execute(post);	

		verify(scheduler).addJob(any(JDFJob.class));
		
	}

	@Test
	public void testGetTaskContent() throws ClientProtocolException, IOException {
		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();

		JDFJob fakeJob = mock(JDFJob.class);

		doReturn(FAKE_JOB_ID).when(fakeJob).getId();

		Task fakeTask = mock(Task.class);
		
		HashMap<String, String> fakeMetadata  = new HashMap<String, String>();
		
		fakeMetadata.put("metadata1", "metadataInfo1");
		fakeMetadata.put("metadata2", "metadataInfo2");
		
		doReturn(fakeMetadata).when(fakeTask).getAllMetadata();
		doReturn(FAKE_TASK_ID).when(fakeTask).getId();

		jobList.add(fakeJob);

		doReturn(fakeTask).when(fakeJob).getTaskById(FAKE_TASK_ID);
		
		doReturn(TaskState.READY).when(fakeJob).getTaskState(FAKE_TASK_ID);

		doReturn(fakeJob).when(scheduler).getJobById(FAKE_JOB_ID);
		
		doReturn(jobList).when(scheduler).getJobs();
		
		HttpGet get = new HttpGet(URI_TASK_REQUEST+FAKE_TASK_ID);

		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		Assert.assertTrue(response.getFirstHeader("Content-type").getValue()
				.startsWith("text/plain"));

		response.getEntity().writeTo(System.out);

		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
		
		
	}

	@Test
	public void testGetRequestContentNoInfo() throws URISyntaxException, HttpException, IOException {

		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();

		doReturn(jobList).when(scheduler).getJobs();
		HttpGet get = new HttpGet(URI_JOB_REQUEST);

		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		Assert.assertTrue(response.getFirstHeader("Content-type").getValue()
				.startsWith("text/plain"));

		response.getEntity().writeTo(System.out);

		Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

	}
	
	@Test
	public void testGetSpecificNonExistingJobContent() throws ClientProtocolException, IOException {


		HttpGet get = new HttpGet(URI_JOB_REQUEST+fakeId);

		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		
		Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());

	}

	@Test
	public void testGetNonExistingTaskContent() throws ClientProtocolException, IOException {
		ArrayList<JDFJob> jobList = new ArrayList<JDFJob>();

		JDFJob fakeJob = mock(JDFJob.class);

		doReturn(FAKE_JOB_ID).when(fakeJob).getId();

		jobList.add(fakeJob);

		doReturn(fakeJob).when(scheduler).getJobById(FAKE_JOB_ID);
		
		doReturn(jobList).when(scheduler).getJobs();
		
		HttpGet get = new HttpGet(URI_TASK_REQUEST+FAKE_TASK_ID);

		HttpClient client = HttpClients.createMinimal();
		HttpResponse response = client.execute(get);

		Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
	}
	
	@After
	public void tearDown() throws Exception {
		jdfApp.stopServer();
	}

	/**
	 * Getting a available port on range 60000:61000
	 * @return
	 */
	public static int getAvailablePort() {		
		int initialP = 60000;
		int finalP = 61000;
		for (int i = initialP; i < finalP; i++) {
			int port = new Random().nextInt(finalP - initialP) + initialP;
			ServerSocket ss = null;
			DatagramSocket ds = null;
			try {
				ss = new ServerSocket(port);
				ss.setReuseAddress(true);
				ds = new DatagramSocket(port);
				ds.setReuseAddress(true);
				return port;
			} catch (IOException e) {
			} finally {
				if (ds != null) {
					ds.close();
				}
				if (ss != null) {
					try {
						ss.close();
					} catch (IOException e) {
						/* should not be thrown */
					}
				}
			}
		}		
		return -1;
	}
}
