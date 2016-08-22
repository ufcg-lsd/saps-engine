package org.fogbowcloud.sebal.engine.scheduler.core.model;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.scheduler.core.model.Job;
import org.fogbowcloud.blowout.scheduler.core.model.Task;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
import org.fogbowcloud.sebal.engine.sebal.SebalTasks;

public class SebalJob extends Job {

	private ImageDataStore imageStore;
	private Map<String, Task> taskList = new HashMap<String, Task>();
	private Map<String, ImageState> pendingUpdates = new HashMap<String, ImageState>();
	
	public static final Logger LOGGER = Logger.getLogger(SebalJob.class);
	
	public SebalJob(ImageDataStore imageStore) {
		this.imageStore = imageStore;
	}

	@Override
	public void finish(Task task) {
		LOGGER.debug("Moving task " + task.getId() + " from RUNNING to COMPLETED.");
		task.finish();
		
		// check if all R task already ran for the image
		if (task.getMetadata(SebalTasks.METADATA_PHASE).equals(SebalTasks.R_SCRIPT_PHASE)){
			List<Task> readyOrRunningTasks = getReadyOrRunningTasks(
					task.getMetadata(SebalTasks.METADATA_IMAGE_NAME));
			
			List<Task> rTasks = filterTaskByPhase(readyOrRunningTasks, SebalTasks.R_SCRIPT_PHASE);
			LOGGER.debug("There is " + rTasks.size() + " tasks of image "
					+ task.getMetadata(SebalTasks.METADATA_IMAGE_NAME) + " in R script phase.");
			if (rTasks == null || rTasks.isEmpty()) {
				udpateDB(task.getMetadata(SebalTasks.METADATA_IMAGE_NAME),
						ImageState.FINISHED);
			}
		}
	}

	protected void udpateDB(String imageName, ImageState imageState) {
		LOGGER.debug("Updating image " + imageName + " to state " + imageState.getValue());
		//TODO: Review this try/catch section
		//****	Each DB interaction could be dealt separately in this case?
		try {
			imageStore.updateImageState(imageName, imageState);
			imageStore.addStateStamp(imageName, imageState, new Date(Calendar
					.getInstance().getTimeInMillis()));

			// updating previous images not updated yet because of any connection problem
			for (String pendingImage : new ArrayList<String>(getPendingUpdates().keySet())) {
				imageStore.updateImageState(pendingImage, getPendingUpdates().get(pendingImage));
				imageStore.addStateStamp(pendingImage, getPendingUpdates().get(pendingImage), new Date(Calendar
						.getInstance().getTimeInMillis()));
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

//		Old:
//		for (TaskState taskState : taskStates) {
//			if (TaskState.READY.equals(taskState)) {
//				allTasks.addAll(tasksReady);
//			} else if (TaskState.RUNNING.equals(taskState)) {
//				allTasks.addAll(tasksRunning);
//			} else if (TaskState.FAILED.equals(taskState)) {
//				allTasks.addAll(tasksFailed);
//			} else if (TaskState.COMPLETED.equals(taskState)) {
//				allTasks.addAll(tasksCompleted);			
//			}			
//		}
		
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
}
