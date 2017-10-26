package org.fogbowcloud.saps.engine.core.preprocessor;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTag;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTagUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestPreProcessorImpl {

	private Properties properties;
	private PreProcessorImpl preProcessor;
	private List<ImageTask> imageTasks = new ArrayList<ImageTask>();

	@Before
	public void setUp() throws Exception {
		this.properties = new Properties();
		this.properties.put(SapsPropertiesConstants.SAPS_EXPORT_PATH, "/local/exports/");
		this.properties.put(SapsPropertiesConstants.SAPS_CONTAINER_LINKED_PATH, "/home/ubuntu/");
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);

		Date date = new Date(10000854);
		String federationMember = "fake-fed-member";

		ImageTask taskOne = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.CREATED, federationMember, 0, "NE", "NE", "pre_processing", "NE",
				"NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "");

		this.imageTasks.add(taskOne);
		int imagesLimit = 1;
		Mockito.doReturn(this.imageTasks).when(imageStore).getIn(ImageTaskState.DOWNLOADED,
				imagesLimit);

		this.preProcessor = Mockito.spy(new PreProcessorImpl(this.properties, imageStore));
	}

	@Test
	public void testPreProcessImage() throws Exception {

		ExecutionScriptTag execScriptTag = new ExecutionScriptTag(
				this.imageTasks.get(0).getInputPreprocessingTag(), "fogbow/preprocessor",
				"preprocessor", ExecutionScriptTagUtil.PRE_PROCESSING);
		
		this.preProcessor.preProcessImage(this.imageTasks.get(0));

		Mockito.verify(this.preProcessor).getDockerImage(Mockito.eq(execScriptTag));
		Mockito.doNothing().when(this.preProcessor).getDockerImage(execScriptTag);
		
		String hostPath = this.properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH)
				+ this.imageTasks.get(0).getTaskId() + "/data/preprocessing";
		String containerPath = this.properties
				.getProperty(SapsPropertiesConstants.SAPS_CONTAINER_LINKED_PATH);

		//Mockito.verify(this.preProcessor).raiseContainer(execScriptTag, this.imageTasks.get(0),
				//hostPath, containerPath);

		String containerId = new String("fake-container-id");
		Mockito.doReturn(containerId).when(this.preProcessor).raiseContainer(execScriptTag,
				this.imageTasks.get(0), hostPath, containerPath);

		String commandToRun = SapsPropertiesConstants.DEFAULT_PREPROCESSOR_CONTAINER_SCRIPT;

		//Mockito.verify(this.preProcessor).executeContainer(containerId, commandToRun,
		//		this.imageTasks.get(0));
		Mockito.doNothing().when(this.preProcessor).executeContainer(containerId, commandToRun,
				this.imageTasks.get(0));

		

	}

}
