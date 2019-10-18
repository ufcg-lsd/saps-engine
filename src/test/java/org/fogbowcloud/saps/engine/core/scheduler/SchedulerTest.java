package org.fogbowcloud.saps.engine.core.scheduler;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.junit.Before;
import org.junit.Test;

public class SchedulerTest {

	Scheduler scheduler = new Scheduler();
	List<ImageTask> tasksL1Created = new ArrayList<ImageTask>();
	List<ImageTask> tasksL1Downloaded = new ArrayList<ImageTask>();
	List<ImageTask> tasksL1Preprocessed = new ArrayList<ImageTask>();
	
	@Before
	public void setUpL1Created() {
		ImageTask task01 = new ImageTask("1", "landsat_8", "217066", new Date(), ImageTaskState.CREATED, "", 5,
				"user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		ImageTask task02 = new ImageTask("2", "landsat_8", "217066", new Date(), ImageTaskState.CREATED, "", 4,
				"user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		ImageTask task03 = new ImageTask("3", "landsat_8", "217066", new Date(), ImageTaskState.CREATED, "", 3,
				"user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		
		ImageTask task04 = new ImageTask("4", "landsat_8", "217066", new Date(), ImageTaskState.CREATED, "", 3,
				"user2", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		ImageTask task05 = new ImageTask("5", "landsat_8", "217066", new Date(), ImageTaskState.CREATED, "", 5,
				"user2", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		
		ImageTask task06 = new ImageTask("6", "landsat_8", "217066", new Date(), ImageTaskState.CREATED, "", 2,
				"user3", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");

		tasksL1Created.add(task01);
		tasksL1Created.add(task02);
		tasksL1Created.add(task03);
		tasksL1Created.add(task04);
		tasksL1Created.add(task05);
		tasksL1Created.add(task06);
	}
	
	@Before
	public void setUpL1Downloaded() {
		ImageTask task01 = new ImageTask("1", "landsat_8", "217066", new Date(), ImageTaskState.DOWNLOADED, "", 5,
				"user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		ImageTask task02 = new ImageTask("2", "landsat_8", "217066", new Date(), ImageTaskState.DOWNLOADED, "", 4,
				"user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		ImageTask task03 = new ImageTask("3", "landsat_8", "217066", new Date(), ImageTaskState.DOWNLOADED, "", 3,
				"user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		
		ImageTask task04 = new ImageTask("4", "landsat_8", "217066", new Date(), ImageTaskState.DOWNLOADED, "", 3,
				"user2", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		ImageTask task05 = new ImageTask("5", "landsat_8", "217066", new Date(), ImageTaskState.DOWNLOADED, "", 5,
				"user2", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		
		ImageTask task06 = new ImageTask("6", "landsat_8", "217066", new Date(), ImageTaskState.DOWNLOADED, "", 2,
				"user3", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");

		tasksL1Downloaded.add(task01);
		tasksL1Downloaded.add(task02);
		tasksL1Downloaded.add(task03);
		tasksL1Downloaded.add(task04);
		tasksL1Downloaded.add(task05);
		tasksL1Downloaded.add(task06);
	}
	
	@Before
	public void setUpL1Preprocessed() {
		ImageTask task01 = new ImageTask("1", "landsat_8", "217066", new Date(), ImageTaskState.PREPROCESSED, "", 5,
				"user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		ImageTask task02 = new ImageTask("2", "landsat_8", "217066", new Date(), ImageTaskState.PREPROCESSED, "", 4,
				"user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		ImageTask task03 = new ImageTask("3", "landsat_8", "217066", new Date(), ImageTaskState.PREPROCESSED, "", 3,
				"user1", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		
		ImageTask task04 = new ImageTask("4", "landsat_8", "217066", new Date(), ImageTaskState.PREPROCESSED, "", 3,
				"user2", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		ImageTask task05 = new ImageTask("5", "landsat_8", "217066", new Date(), ImageTaskState.PREPROCESSED, "", 5,
				"user2", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");
		
		ImageTask task06 = new ImageTask("6", "landsat_8", "217066", new Date(), ImageTaskState.PREPROCESSED, "", 2,
				"user3", "nop", "nop", "aio", new Timestamp(1), new Timestamp(1), "", "");

		tasksL1Preprocessed.add(task01);
		tasksL1Preprocessed.add(task02);
		tasksL1Preprocessed.add(task03);
		tasksL1Preprocessed.add(task04);
		tasksL1Preprocessed.add(task05);
		tasksL1Preprocessed.add(task06);
	}
	
	@Test
	public void testMapUserToTasksWithTasksL1Created() {
		Map<String, List<ImageTask>> mapUserToTasks = scheduler.mapUsers2Tasks(tasksL1Created);
		
		assert (mapUserToTasks.get("user1").size() == 3);
		assert (mapUserToTasks.get("user2").size() == 2);
		assert (mapUserToTasks.get("user3").size() == 1);
		
	}
	
	@Test
	public void testMapUserToTasksWithTasksL1Downloaded() {
		Map<String, List<ImageTask>> mapUserToTasks = scheduler.mapUsers2Tasks(tasksL1Downloaded);
		
		assert (mapUserToTasks.get("user1").size() == 3);
		assert (mapUserToTasks.get("user2").size() == 2);
		assert (mapUserToTasks.get("user3").size() == 1);
		
	}
	
	@Test
	public void testMapUserToTasksWithTasksL1Preprocessed() {
		Map<String, List<ImageTask>> mapUserToTasks = scheduler.mapUsers2Tasks(tasksL1Preprocessed);
		
		assert (mapUserToTasks.get("user1").size() == 3);
		assert (mapUserToTasks.get("user2").size() == 2);
		assert (mapUserToTasks.get("user3").size() == 1);
		
	}
	
	@Test
	public void testSelectWithTasksL1Created() {
		Map<String, List<ImageTask>> mapUserToTasks = scheduler.mapUsers2Tasks(tasksL1Created);
		
		List<ImageTask> selectedTasks = scheduler.select(5, mapUserToTasks);
		assert (selectedTasks.size() == 5);
	}
	
	@Test
	public void test2SelectWithTasksL1Created() {
		Map<String, List<ImageTask>> mapUserToTasks = scheduler.mapUsers2Tasks(tasksL1Created);
		
		List<ImageTask> selectedTasks = scheduler.select(7, mapUserToTasks);
		assert (selectedTasks.size() == 6);
	}
	
	@Test
	public void testSelectWithTasksL1Downloaded() {
		Map<String, List<ImageTask>> mapUserToTasks = scheduler.mapUsers2Tasks(tasksL1Downloaded);
		
		List<ImageTask> selectedTasks = scheduler.select(5, mapUserToTasks);
		assert (selectedTasks.size() == 5);
	}
	
	@Test
	public void test2SelectWithTasksL1Downloaded() {
		Map<String, List<ImageTask>> mapUserToTasks = scheduler.mapUsers2Tasks(tasksL1Downloaded);
		
		List<ImageTask> selectedTasks = scheduler.select(7, mapUserToTasks);
		assert (selectedTasks.size() == 6);
	}
	
	@Test
	public void testSelectWithTasksL1Preprocessed() {
		Map<String, List<ImageTask>> mapUserToTasks = scheduler.mapUsers2Tasks(tasksL1Preprocessed);
		
		List<ImageTask> selectedTasks = scheduler.select(5, mapUserToTasks);
		assert (selectedTasks.size() == 5);
	}
	
	@Test
	public void test2SelectWithTasksL1Preprocessed() {
		Map<String, List<ImageTask>> mapUserToTasks = scheduler.mapUsers2Tasks(tasksL1Preprocessed);
		
		List<ImageTask> selectedTasks = scheduler.select(7, mapUserToTasks);
		assert (selectedTasks.size() == 6);
	}

}
