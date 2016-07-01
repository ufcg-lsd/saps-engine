package org.fogbowcloud.scheduler.core.model;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.sebal.ImageDataStore;
import org.fogbowcloud.sebal.ImageState;
import org.fogbowcloud.sebal.SebalTasks;
import org.junit.Before;
import org.junit.Test;

public class TestSebalJob {
	
	private static final String FAKE_TASK_ID = "taskId";
	private static final String IMAGE_1_NAME = "image1Name";
	SebalJob job;
	ImageDataStore dstore;
	
	
	@Before
	public void setUp(){
		dstore = mock(ImageDataStore.class);
		job = spy(new SebalJob(dstore));
	}
	
	@Test
	public void testFinishPhaseF1(){
		Task task = mock(TaskImpl.class);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(SebalTasks.F1_PHASE).when(task).getMetadata(SebalTasks.METADATA_PHASE);
		doReturn(IMAGE_1_NAME).when(task).getMetadata(SebalTasks.METADATA_IMAGE_NAME);
		List<Task> imageTaskForImage = new ArrayList<Task>();
		imageTaskForImage.add(task);
		doReturn(imageTaskForImage).when(job).getTasksOfImageByState(IMAGE_1_NAME, TaskState.READY, TaskState.RUNNING);
		job.finish(task);
		verify(job).filterTaskByPhase(imageTaskForImage, SebalTasks.F1_PHASE);
	}
	
	@Test
	public void testFinishPhaseF1TasksFinished(){
		Task task = mock(TaskImpl.class);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(SebalTasks.F1_PHASE).when(task).getMetadata(SebalTasks.METADATA_PHASE);
		doReturn(IMAGE_1_NAME).when(task).getMetadata(SebalTasks.METADATA_IMAGE_NAME);
		List<Task> imageTaskForImage = new ArrayList<Task>();
		doReturn(imageTaskForImage).when(job).getTasksOfImageByState(IMAGE_1_NAME, TaskState.READY, TaskState.RUNNING);
		//doNothing().when(job).udpateDB(IMAGE_1_NAME, ImageState.READY_FOR_PHASE_C);
		job.finish(task);
		//verify(job).udpateDB(IMAGE_1_NAME, ImageState.READY_FOR_PHASE_C);
	}
	
	@Test
	public void testFinishPhaseC(){
		Task task = mock(TaskImpl.class);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(SebalTasks.C_PHASE).when(task).getMetadata(SebalTasks.METADATA_PHASE);
		doReturn(IMAGE_1_NAME).when(task).getMetadata(SebalTasks.METADATA_IMAGE_NAME);
		List<Task> imageTaskForImage = new ArrayList<Task>();
		imageTaskForImage.add(task);
		doReturn(imageTaskForImage).when(job).getTasksOfImageByState(IMAGE_1_NAME, TaskState.READY, TaskState.RUNNING);
		job.finish(task);
		verify(job).filterTaskByPhase(imageTaskForImage, SebalTasks.C_PHASE);
	}
	
	@Test
	public void testFinishPhaseCTasksFinished(){
		Task task = mock(TaskImpl.class);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(SebalTasks.C_PHASE).when(task).getMetadata(SebalTasks.METADATA_PHASE);
		doReturn(IMAGE_1_NAME).when(task).getMetadata(SebalTasks.METADATA_IMAGE_NAME);
		List<Task> imageTaskForImage = new ArrayList<Task>();
		doReturn(imageTaskForImage).when(job).getTasksOfImageByState(IMAGE_1_NAME, TaskState.READY, TaskState.RUNNING);
		//doNothing().when(job).udpateDB(IMAGE_1_NAME, ImageState.READY_FOR_PHASE_F2);
		job.finish(task);
		//verify(job).udpateDB(IMAGE_1_NAME, ImageState.READY_FOR_PHASE_F2);
	}
	

	@Test
	public void testFinishPhaseF2(){
		Task task = mock(TaskImpl.class);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(SebalTasks.F2_PHASE).when(task).getMetadata(SebalTasks.METADATA_PHASE);
		doReturn(IMAGE_1_NAME).when(task).getMetadata(SebalTasks.METADATA_IMAGE_NAME);
		List<Task> imageTaskForImage = new ArrayList<Task>();
		imageTaskForImage.add(task);
		doReturn(imageTaskForImage).when(job).getTasksOfImageByState(IMAGE_1_NAME, TaskState.READY, TaskState.RUNNING);
		job.finish(task);
		verify(job).filterTaskByPhase(imageTaskForImage, SebalTasks.F2_PHASE);
	}
	
	
	@Test
	public void testFinishPhaseF2TasksFinished(){
		Task task = mock(TaskImpl.class);
		doReturn(FAKE_TASK_ID).when(task).getId();
		doReturn(SebalTasks.F2_PHASE).when(task).getMetadata(SebalTasks.METADATA_PHASE);
		doReturn(IMAGE_1_NAME).when(task).getMetadata(SebalTasks.METADATA_IMAGE_NAME);
		List<Task> imageTaskForImage = new ArrayList<Task>();
		doReturn(imageTaskForImage).when(job).getTasksOfImageByState(IMAGE_1_NAME, TaskState.READY, TaskState.RUNNING);
		doNothing().when(job).udpateDB(IMAGE_1_NAME, ImageState.FINISHED);
		job.finish(task);
		verify(job).udpateDB(IMAGE_1_NAME, ImageState.FINISHED);
	}
	
	@Test
	public void testUpdateDBOnlyOneValue() throws SQLException{
		String fakeImageName = "fakeimagename";
		doNothing().when(dstore).updateImageState(fakeImageName, ImageState.FINISHED);
		Map<String, ImageState> fakePendingMap = new HashMap<String, ImageState>();
		doReturn(fakePendingMap).when(job).getPendingUpdates();
		
		job.udpateDB(fakeImageName, ImageState.FINISHED);
		verify(dstore).updateImageState(fakeImageName, ImageState.FINISHED);
	}
	
	@Test
	public void testUpdateDBWithPendingRequests() throws SQLException{
		String fakeImageName = "fakeimagename";
		doNothing().when(dstore).updateImageState(fakeImageName, ImageState.FINISHED);
		Map<String, ImageState> fakePendingMap = new HashMap<String, ImageState>();
		
		String pendingImageName = "pendindImage";
		doNothing().when(dstore).updateImageState(pendingImageName, ImageState.FINISHED);
		fakePendingMap.put(pendingImageName, ImageState.FINISHED);
		doReturn(fakePendingMap).when(job).getPendingUpdates();
		
		job.udpateDB(fakeImageName, ImageState.FINISHED);
		verify(dstore).updateImageState(fakeImageName, ImageState.FINISHED);
		verify(dstore).updateImageState(pendingImageName, ImageState.FINISHED);
	}
	
	@Test
	public void testUpdateDBExceptionOcccurs() throws SQLException{
		String fakeImageName = "fakeimagename";
		doThrow(new SQLException("Invalid format")).when(dstore).updateImageState(fakeImageName, ImageState.FINISHED);
		Map<String, ImageState> fakePendingMap = new HashMap<String, ImageState>();
		
		String pendingImageName = "pendindImage";
		doNothing().when(dstore).updateImageState(pendingImageName, ImageState.FINISHED);
		fakePendingMap.put(pendingImageName, ImageState.FINISHED);
		doReturn(fakePendingMap).when(job).getPendingUpdates();
		
		job.udpateDB(fakeImageName, ImageState.FINISHED);
		assert(job.getPendingUpdates().keySet().contains(fakeImageName));
		assert(job.getPendingUpdates().keySet().contains(pendingImageName));
	}

	@Test
	public void testFilterTaskByPhase(){
		Task task1 = mock(Task.class);
		doReturn(SebalTasks.F1_PHASE).when(task1).getMetadata(SebalTasks.METADATA_PHASE);
		doReturn("FakeId1").when(task1).getId();
		Task task2 = mock(Task.class);
		doReturn(SebalTasks.F1_PHASE).when(task2).getMetadata(SebalTasks.METADATA_PHASE);
		doReturn("FakeId2").when(task2).getId();
		Task task3 = mock(Task.class);
		doReturn(SebalTasks.C_PHASE).when(task3).getMetadata(SebalTasks.METADATA_PHASE);
		doReturn("FakeId3").when(task3).getId();
		Task task4 = mock(Task.class);
		doReturn(SebalTasks.F2_PHASE).when(task4).getMetadata(SebalTasks.METADATA_PHASE);
		doReturn("FakeId4").when(task4).getId();
		
		List<Task> taskList = new ArrayList<Task>();
		taskList.add(task1);
		taskList.add(task2);
		taskList.add(task3);
		taskList.add(task4);
		
		List<Task> f1List = (ArrayList<Task>) job.filterTaskByPhase(taskList, SebalTasks.F1_PHASE);
		assert(f1List.contains(task1));
		assert(f1List.contains(task2));
		assertEquals(2, f1List.size());
		
		List<Task> f2List = (ArrayList<Task>) job.filterTaskByPhase(taskList, SebalTasks.F2_PHASE);
		assert(f2List.contains(task4));
		assertEquals(1, f2List.size());
		
		List<Task> f3List = (ArrayList<Task>) job.filterTaskByPhase(taskList, SebalTasks.C_PHASE);
		assert(f3List.contains(task3));
		assertEquals(1, f3List.size());
	}
	
}
