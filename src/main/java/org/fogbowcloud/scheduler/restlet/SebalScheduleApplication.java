package org.fogbowcloud.scheduler.restlet;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.SebalJob;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.util.AppPropertiesConstants;
import org.fogbowcloud.scheduler.restlet.resource.ImageResource;
import org.fogbowcloud.scheduler.restlet.resource.SebalImagensResource;
import org.fogbowcloud.scheduler.restlet.resource.TaskResource;
import org.fogbowcloud.sebal.ImageData;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.swift.SwiftClient;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.restlet.service.ConnectorService;

public class SebalScheduleApplication extends Application {
	
	private SebalJob job;
	private Properties properties;
	private ImageDataStore imageDataStore;
	private SwiftClient swiftClient;
	
	public SebalScheduleApplication(SebalJob job, ImageDataStore imageDataStore, Properties properties){
		this.job = job;
		this.properties = properties;
		this.imageDataStore = imageDataStore;
		this.swiftClient = new SwiftClient(properties);
	}
	

	public void startServer() throws Exception {
		
		ConnectorService corsService = new ConnectorService();         
		
		this.getServices().add(corsService);
		
		Component c = new Component();
		int port = Integer.parseInt(properties.getProperty(AppPropertiesConstants.REST_SERVER_PORT));
		c.getServers().add(Protocol.HTTP, port);
		c.getDefaultHost().attach(this); 
		c.start();

	}

	@Override
	public Restlet createInboundRoot() {
		
		Router router = new Router(getContext());
		router.attach("/sebal-scheduler/image", ImageResource.class);
		router.attach("/sebal-scheduler/image/{imgName}", ImageResource.class);
		router.attach("/sebal-scheduler/task/{taskId}", TaskResource.class);
		router.attach("/sebal-scheduler/task/{taskId}/{varName}", TaskResource.class);
		router.attach("/sebal-scheduler/fetcher/image", SebalImagensResource.class);
		router.attach("/sebal-scheduler/fetcher/image/{imageName}/{varName}", SebalImagensResource.class);
		router.attach("/sebal-scheduler/fetcher/filter/{filter}", SebalImagensResource.class);
		
		return router;
	}
	
	public List<ImageData> getAllImages() throws SQLException{
		return imageDataStore.getAllImages();
	}
	
	public List<ImageData> getImagesByFilters(ImageState state, String name, long periodInit, long periodEnd) throws SQLException{
		return imageDataStore.getImagesByFilter(state, name, periodInit, periodEnd);
	}
	
	public Map<Task, TaskState> getAllTaskByImage(String imageName){
		
		Map<Task, TaskState> tasks = new HashMap<Task, TaskState>();
		for(Task t : job.getTasksOfImageByState(imageName, TaskState.READY)){
			tasks.put(t, TaskState.READY);
		}
		for(Task t : job.getTasksOfImageByState(imageName, TaskState.RUNNING)){
			tasks.put(t, TaskState.RUNNING);
		}
		for(Task t : job.getTasksOfImageByState(imageName, TaskState.COMPLETED)){
			tasks.put(t, TaskState.COMPLETED);
		}
		for(Task t : job.getTasksOfImageByState(imageName, TaskState.FAILED)){
			tasks.put(t, TaskState.FAILED);
		}
		
		return tasks;
	}
	
	public List<ImageData> getFetchedImages() throws Exception {
		try {
			return imageDataStore.getIn(ImageState.FETCHED);
		} catch (SQLException e) {
			throw new Exception("Error getting fetched images.", e);
		}
	}
	
	public byte[] getImageFromSwift(String imageName, String imageVar){
		
		String containerName = properties.getProperty(AppPropertiesConstants.SWIFT_CONTAINER_NAME);
		//String pseudFolder = properties.getProperty(AppPropertiesConstants.SWIFT_PSEUD_FOLDER_PREFIX)+imageName+"/";
		String pseudFolder = properties.getProperty(AppPropertiesConstants.SWIFT_PSEUD_FOLDER_PREFIX);
		String fileName = imageName+imageVar;
		
		return swiftClient.downloadFile(containerName, fileName, pseudFolder);
	}
	
	public Task getTaskById(String taskId){
		return job.getCompletedTask(taskId);
	}
	
	public List<Task> getAllCompletedTasks() {
		return job.getTasksByState(TaskState.COMPLETED);
		
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	//This method is only for test propose 
	public static void main(String[] args) throws Exception{
		
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream("sebal.conf");
		properties.load(input);
		
		String imageStoreIP = "loclahost";
		String imageStorePort = "8080";
		
		ImageDataStore imageStore = new JDBCImageDataStore(properties, imageStoreIP, imageStorePort);
		
		SebalScheduleApplication sebalScheduleApplication = new SebalScheduleApplication(null, imageStore, properties);
		sebalScheduleApplication.startServer();
	}

}
