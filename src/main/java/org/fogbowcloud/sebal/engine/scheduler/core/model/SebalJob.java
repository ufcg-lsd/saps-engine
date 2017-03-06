package org.fogbowcloud.sebal.engine.scheduler.core.model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.core.model.Job;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.SebalTasks;

public class SebalJob extends Job {
	
	private static final long serialVersionUID = -6111900503095749695L;
	
	private final String UUID = "";
	
	private ImageDataStore imageStore;
	private Map<String, Task> taskList = new HashMap<String, Task>();
	private Map<String, ImageState> pendingUpdates = new HashMap<String, ImageState>();
	
	public static final Logger LOGGER = Logger.getLogger(SebalJob.class);
	
	public SebalJob(ImageDataStore imageStore) {
		super(new ArrayList<Task>());
		this.imageStore = imageStore;
		
	}

	@Override
	public void finish(Task task) {
	}

	protected void udpateDB(String imageName, ImageState imageState) {
		LOGGER.debug("Updating image " + imageName + " to state " + imageState.getValue());
		//TODO: Review this try/catch section
		//****	Each DB interaction could be dealt separately in this case?
		try {
			imageStore.updateImageState(imageName, imageState);
			
			ImageData imageData = imageStore.getImage(imageName);			
			imageStore.addStateStamp(imageName, imageState, imageData.getUpdateTime());

			// updating previous images not updated yet because of any connection problem
			for (String pendingImage : new ArrayList<String>(getPendingUpdates().keySet())) {
				imageStore.updateImageState(pendingImage, getPendingUpdates().get(pendingImage));
				
				ImageData pendingImageData = imageStore.getImage(pendingImage);
				imageStore.addStateStamp(pendingImage, getPendingUpdates().get(pendingImage), pendingImageData.getUpdateTime());
				getPendingUpdates().remove(pendingImage);
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image " + imageName + " to state "
					+ imageState.getValue(), e);
			LOGGER.debug("Adding image " + imageName + " to pendingUpdates.");
			getPendingUpdates().put(imageName, imageState);
		}
	}

	protected List<Task> filterTaskByPhase(List<Task> tasks, String taskPhase) {
		List<Task> filteredTasks = new ArrayList<Task>();
		for (Task task : tasks) {
			if (taskPhase.equals(task.getMetadata(SebalTasks.METADATA_PHASE))) {
				filteredTasks.add(task);
			}
		}
		return filteredTasks;
	}

	@Override
	public void fail(Task task) {
		LOGGER.debug("Task " + task.getId() + " FAILED.");
	}

	protected Map<String, ImageState> getPendingUpdates(){
		return this.pendingUpdates;
	}
	
	public List<Task> getReadyOrRunningTasks(String imageName) {
		List<Task> allTasks = new ArrayList<Task>();
		
		for(Task task : this.taskList.values()) {
			if(!task.isFinished() && !task.isFailed()) {
				allTasks.add(task);
			}
		}
		
		List<Task> imageTasks = new LinkedList<Task>();		
		for (Task task : allTasks) {
			String taskImageName = task.getMetadata(SebalTasks.METADATA_IMAGE_NAME);
			if (taskImageName.equals(imageName)) {
				imageTasks.add(task);
			}
		}
		return imageTasks;
	}
	
	public Task getCompletedTask(String taskId){
		if(taskList.get(taskId).isFinished()) {
				return taskList.get(taskId);
		}
		return null;
	}
	
	public List<Task> getAllCompletedTasks() {
		List<Task> allTasks = new ArrayList<Task>();
		
		for (Task task : this.taskList.values()) {
			if(task.isFinished()) {
				allTasks.add(task);
			}
		}
		return allTasks;
	}
	
	@Override
	public String getId() {
		return this.UUID;
	}
}
