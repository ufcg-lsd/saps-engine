package org.fogbowcloud.scheduler.core.model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.SebalTasks;


public class SebalJob extends Job {

	private ImageDataStore imageStore;
	private Map<String, ImageState> pendingUpdates = new HashMap<String, ImageState>();
	
	public static final Logger LOGGER = Logger.getLogger(SebalJob.class);
	
	public SebalJob(ImageDataStore imageStore) {
		this.imageStore = imageStore;
	}

	@Override
	public void finish(Task task) {
		task.finish();
		this.tasksRunning.remove(task);
		this.tasksCompleted.add(task);
		
		// check if all F1 tasks already ran for the image
		if (task.getMetadata(SebalTasks.METADATA_PHASE).equals(SebalTasks.F1_PHASE)){
			List<Task> readyOrRunningTasks = getTasksOfImageByState(
					task.getMetadata(SebalTasks.METADATA_IMAGE_NAME), TaskState.READY, TaskState.RUNNING);
			
			List<Task> f1Tasks = filterTaskByPhase(readyOrRunningTasks, SebalTasks.F1_PHASE);
			if (f1Tasks == null || f1Tasks.isEmpty()) {
				udpateDB(task.getMetadata(SebalTasks.METADATA_IMAGE_NAME),
						ImageState.READY_FOR_PHASE_C);
			}
			
			// check if all C tasks already ran for the image
		} else if (task.getMetadata(SebalTasks.METADATA_PHASE).equals(SebalTasks.C_PHASE)) {
			List<Task> readyOrRunningTasks = getTasksOfImageByState(
					task.getMetadata(SebalTasks.METADATA_IMAGE_NAME), TaskState.READY, TaskState.RUNNING);

			List<Task> cTasks = filterTaskByPhase(readyOrRunningTasks, SebalTasks.C_PHASE);
			if (cTasks == null || cTasks.isEmpty()) {
				udpateDB(task.getMetadata(SebalTasks.METADATA_IMAGE_NAME),
						ImageState.READY_FOR_PHASE_F2);
			}
			
			// check if all F2 tasks already ran for the image
		} else if (task.getMetadata(SebalTasks.METADATA_PHASE).equals(SebalTasks.F2_PHASE)) {
			List<Task> readyOrRunningTasks = getTasksOfImageByState(
					task.getMetadata(SebalTasks.METADATA_IMAGE_NAME), TaskState.READY, TaskState.RUNNING);

			List<Task> f2Tasks = filterTaskByPhase(readyOrRunningTasks, SebalTasks.F2_PHASE);
			if (f2Tasks == null || f2Tasks.isEmpty()) {
				udpateDB(task.getMetadata(SebalTasks.METADATA_IMAGE_NAME),
						ImageState.FINISHED);
			}
		}
	}

	private void udpateDB(String imageName, ImageState imageState) {
		try {
			imageStore.updateState(imageName, imageState);

			// updating previous images not updated yet because of any connection problem
			for (String pendingImage : new ArrayList<String>(pendingUpdates.keySet())) {
				imageStore.updateState(pendingImage, pendingUpdates.get(pendingImage));
				pendingUpdates.remove(pendingImage);
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image " + imageName + " to state "
					+ imageState.getValue());
			LOGGER.debug("Adding image " + imageName + " to pendingUpdates.");
			pendingUpdates.put(imageName, imageState);
		}
	}

	private List<Task> filterTaskByPhase(List<Task> tasks, String taskPhase) {
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
		this.tasksRunning.remove(task);
		this.tasksFailed.add(task);
		
		this.tasksReady.add(task.clone());
	}

	@Override
	public void run(Task task) {
		// TODO Auto-generated method stub		
		tasksReady.remove(task);
		tasksRunning.add(task);
		
	}

	private List<Task> getTasksOfImageByState(String imageName, TaskState... taskStates) {
		List<Task> allTasks = new ArrayList<Task>();
		
		for (TaskState taskState : taskStates) {
			if (TaskState.READY.equals(taskState)) {
				allTasks.addAll(tasksReady);
			} else if (TaskState.RUNNING.equals(taskState)) {
				allTasks.addAll(tasksRunning);
			} else if (TaskState.FAILED.equals(taskState)) {
				allTasks.addAll(tasksFailed);
			} else if (TaskState.COMPLETED.equals(taskState)) {
				allTasks.addAll(tasksCompleted);			
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
//
//	private static boolean allTasksFinished(Scheduler scheduler, List<Task> imageTasks,
//			String... phasesToBeFinished) {
//		List<String> phasesToBeFinishedList = Arrays.asList(phasesToBeFinished);
//		for (Task task : imageTasks) {
//			if (!scheduler.getTaskState(task.getId()).equals(Task.State.FINISHED)
//					&& phasesToBeFinishedList.contains(task.getMetadata(SebalTasks.METADATA_PHASE))) {
//				LOGGER.debug("Task " + task.getId() + " is in state "
//						+ scheduler.getTaskState(task.getId()));
//				return false;
//			}
//		}
//		return true;
//	}
}
