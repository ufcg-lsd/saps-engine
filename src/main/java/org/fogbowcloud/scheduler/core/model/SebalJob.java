package org.fogbowcloud.scheduler.core.model;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.Scheduler;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.SebalTasks;


public class SebalJob extends Job {

	private ImageDataStore imageStore;
	
	public static final Logger LOGGER = Logger.getLogger(SebalJob.class);
	
	public SebalJob(ImageDataStore imageStore) {
		this.imageStore = imageStore;
	}

	@Override
	public void finish(Task task) {
		this.tasksRunning.remove(task);
		this.tasksCompleted.add(task);
		
		//TODO looking for same phase image executions and add next phase task
		if (task.getMetadata(SebalTasks.METADATA_PHASE).equals(SebalTasks.F1_PHASE)){
			
		}
	}

	@Override
	public void fail(Task task) {
		// TODO Auto-generated method stub
		this.tasksRunning.remove(task);
		this.tasksFailed.add(task);
	}

	@Override
	public void run(Task task) {
		// TODO Auto-generated method stub
		
	}

//	private static List<Task> getTasksFromImage(String imageName) {
//		List<Task> imageTasks = new LinkedList<Task>();
//		List<Task> allTasks = scheduler.getTasks();
//		for (Task task : allTasks) {
//			String taskImageName = task.getMetadata(SebalTasks.METADATA_IMAGE_NAME);
//			if (taskImageName.equals(imageName)) {
//				imageTasks.add(task);
//			}
//		}
//		return imageTasks;
//	}
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
