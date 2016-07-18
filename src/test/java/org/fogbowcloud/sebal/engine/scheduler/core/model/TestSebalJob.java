package org.fogbowcloud.sebal.engine.scheduler.core.model;

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

import org.fogbowcloud.blowout.scheduler.core.model.Task;
import org.fogbowcloud.sebal.engine.scheduler.core.model.SebalJob;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;
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
		
		List<Task> taskList = new ArrayList<Task>();
		taskList.add(task1);
		
		// FIXME: add tests to R tasks
	}
	
}
