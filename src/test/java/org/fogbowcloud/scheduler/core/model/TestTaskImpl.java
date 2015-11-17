package org.fogbowcloud.scheduler.core.model;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class TestTaskImpl {
	
	private static final String FAKE_METADATA_VALUE = "fakemetadatavalue";
	private static final String FAKE_METADATA = "fakemetadata";
	private static final String TIME_OUT_VALUE_EMPTY = "";
	private static final String FAKE_TASK_ID = "taskId";
	private static final String TIME_OUT_VALUE_GIBBERISH = "fbajsmnfsakl";
	private static final String TIME_OUT_VALUE_SMALL = "1";
	private static final String TIME_OUT_VALUE_BIG = "50000000000";
	String taskId;
	Specification spec;
	Task task;
	
	
	@Before
	public void setUp(){
		spec = mock(Specification.class);
		taskId = FAKE_TASK_ID;
		task = spy(new TaskImpl(taskId, spec));
	}
	
	@Test
	public void testCheckTimeOutedNotTimeOuted(){
		task.startedRunning();
		
		doReturn(TIME_OUT_VALUE_BIG).when(task).getMetadata(TaskImpl.METADATA_TASK_TIMEOUT);
		assertEquals(false, task.checkTimeOuted());
	}
	
	@Test
	public void testCheckTimeOutedTimeOuted() throws InterruptedException{
		task.startedRunning();
		Thread.sleep(5);
		doReturn(TIME_OUT_VALUE_SMALL).when(task).getMetadata(TaskImpl.METADATA_TASK_TIMEOUT);
		assertEquals(true, task.checkTimeOuted());
	}
	
	@Test
	public void testCheckTimeOutedBadlyFormated() throws InterruptedException{
		task.startedRunning();
		Thread.sleep(5);
		doReturn(TIME_OUT_VALUE_GIBBERISH).when(task).getMetadata(TaskImpl.METADATA_TASK_TIMEOUT);
		assertEquals(false, task.checkTimeOuted());
	}
	
	@Test
	public void testCheckTimeOutedNullTimeOut() throws InterruptedException{
		task.startedRunning();
		Thread.sleep(5);
		doReturn(null).when(task).getMetadata(TaskImpl.METADATA_TASK_TIMEOUT);
		assertEquals(false, task.checkTimeOuted());
	}
	
	@Test
	public void testCheckTimeOutedEmptyTimeOut() throws InterruptedException{
		task.startedRunning();
		Thread.sleep(5);
		doReturn(TIME_OUT_VALUE_EMPTY).when(task).getMetadata(TaskImpl.METADATA_TASK_TIMEOUT);
		assertEquals(false, task.checkTimeOuted());
	}

	@Test
	public void testClone(){
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put(FAKE_METADATA, FAKE_METADATA_VALUE);
		doReturn(metadata).when(task).getAllMetadata();
		List<Command> commands = new ArrayList<Command>();
		Command command = new Command("fakecomands", Command.Type.REMOTE);
		commands.add(command);
		doReturn(commands).when(task).getAllCommands();
		
		Task clonedTask = task.clone();
		
		assertEquals(commands, clonedTask.getAllCommands());
		assertEquals(metadata, clonedTask.getAllMetadata());
		assert(clonedTask.getId().contains("_clonedFrom_" + taskId));
	}
	
	@Test
	public void testGetCommandsByType(){
		List<Command> commands = new ArrayList<Command>();
		Command remoteCommand = new Command("fakeremotecomand", Command.Type.REMOTE);
		Command prologueCommand = new Command("fakeprologueCommand", Command.Type.PROLOGUE);
		Command epilogueCommand = new Command("fakeepilogueCommand", Command.Type.EPILOGUE);
		Command epilogueCommand2 = new Command("fakeepilogueCommand2", Command.Type.EPILOGUE);
		commands.add(remoteCommand);
		commands.add(epilogueCommand);
		commands.add(epilogueCommand2);
		commands.add(prologueCommand);
		doReturn(commands).when(task).getAllCommands();
		
		List<Command> remoteCommands = (ArrayList<Command>) task.getCommandsByType(Command.Type.REMOTE);
		assertEquals(1, remoteCommands.size());
		assert(remoteCommands.contains(remoteCommand));
		
		List<Command> epilogueCommands = (ArrayList<Command>) task.getCommandsByType(Command.Type.EPILOGUE);
		assertEquals(2, epilogueCommands.size());
		assert(epilogueCommands.contains(epilogueCommand));
		assert(epilogueCommands.contains(epilogueCommand2));
	}
	
}
