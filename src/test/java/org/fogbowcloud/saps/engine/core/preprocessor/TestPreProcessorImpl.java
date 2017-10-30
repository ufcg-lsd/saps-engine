package org.fogbowcloud.saps.engine.core.preprocessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Properties;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.scheduler.core.exception.SapsException;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTag;
import org.fogbowcloud.saps.engine.util.ExecutionScriptTagUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestPreProcessorImpl {

	private Properties properties;
	private PreProcessorImpl preProcessor;
	private ImageTask imageTask;

	@Before
	public void setUp() throws Exception {
		this.properties = new Properties();
		this.properties.put(SapsPropertiesConstants.SAPS_EXPORT_PATH, "/local/exports");
		this.properties.put(SapsPropertiesConstants.SAPS_CONTAINER_LINKED_PATH, "/home/ubuntu/");
		ImageDataStore imageStore = Mockito.mock(JDBCImageDataStore.class);

		Date date = new Date(10000854);
		String federationMember = "fake-fed-member";

		this.imageTask = new ImageTask("task-id-1", "LT5", "region-53", date, "link1",
				ImageTaskState.CREATED, federationMember, 0, "NE", "NE", "Default", "NE",
				"NE", "NE", new Timestamp(date.getTime()), new Timestamp(date.getTime()),
				"available", "");

		this.preProcessor = Mockito.spy(new PreProcessorImpl(this.properties, imageStore));
	}

	@Test
	public void testPreProcessImage() throws Exception {
		ExecutionScriptTag execScriptTag = new ExecutionScriptTag(
				this.imageTask.getInputPreprocessingTag(), "fogbow/preprocessor", "preprocessor",
				ExecutionScriptTagUtil.PRE_PROCESSING);

		Mockito.doNothing().when(this.preProcessor).getDockerImage(execScriptTag);

		String containerId = "fake-container-id";
		Mockito.doReturn(containerId).when(this.preProcessor).raiseContainer(
				Mockito.<ExecutionScriptTag>any(), Mockito.<ImageTask>any(), Mockito.anyString(),
				Mockito.anyString());

		try {
			Mockito.doThrow(new Exception()).when(this.preProcessor).executeContainer(
					"fake-container-id", "/home/ubuntu/run.sh", Mockito.<ImageTask>any());
			this.preProcessor.preProcessImage(this.imageTask);
			fail();
		} catch (Exception e) {
		}
	}

	@Test
	public void testGetContainerImageTags() throws SapsException {

		ExecutionScriptTag execScriptTag = new ExecutionScriptTag(
				this.imageTask.getInputPreprocessingTag(), "fogbow/preprocessor", "preprocessor",
				ExecutionScriptTagUtil.PRE_PROCESSING);

		assertEquals(this.preProcessor.getContainerImageTags(this.imageTask), execScriptTag);
	}

	@Test
	public void testGetHostPath() {

		String hostPath = "/local/exports/" + this.imageTask.getTaskId() + "/data/preprocessing";

		assertEquals(hostPath, this.preProcessor.getHostPath(this.imageTask));
	}

}
