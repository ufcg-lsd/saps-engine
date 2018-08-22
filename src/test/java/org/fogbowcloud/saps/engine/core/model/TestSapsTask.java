package org.fogbowcloud.saps.engine.core.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.util.Properties;
import java.util.UUID;

import org.fogbowcloud.blowout.core.model.Specification;
import org.fogbowcloud.blowout.core.model.TaskImpl;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.junit.Assert;
import org.junit.Test;

public class TestSapsTask {

	@Test
	@SuppressWarnings("static-access")
	public void testCreateSapsTask() {
		// set up
		String taskId = "fake-id";
		String federationMember = "fake-fed-member";
		String nfsServerIP = "fake-nfs-ip";
		String nfsServerPort = "fake-nfs-port";
		String workerContainerRepository = "fake-worker-repository";
		String workerContainerTag = "fake-worker-tag";
		String exportPath = "fake-export-path";
		String operatingSystem = "fake-operating-system";
		String kernelVersion = "fake-kernel-version";
		String scriptFilePath = "fake-script-path";

		File fakeScriptFile = new File(scriptFilePath);
		Specification spec = mock(Specification.class);
		
		TaskImpl taskImpl = new TaskImpl(taskId, spec, UUID.randomUUID().toString());
		SapsTask sapsTask = spy(new SapsTask());

		Properties properties = new Properties();
		properties.put(TaskImpl.METADATA_MAX_RESOURCE_CONN_RETRIES, "fake-max-retries");
		properties.put(sapsTask.WORKER_SANDBOX, "fake-worker-sandbox");
		properties.put(sapsTask.WORKER_TASK_TIMEOUT, "fake-worker-timeout");
		properties.put(sapsTask.WORKER_REMOTE_USER, "fake-user");
		properties.put(sapsTask.WORKER_EXPORT_PATH, exportPath);
		properties.put(sapsTask.METADATA_MAX_TASK_EXECUTION_TIME, "fake-max-time");
		properties.put(sapsTask.WORKER_MOUNT_POINT, "fake-mount-point");
		properties.put(taskImpl.METADATA_SANDBOX, "fake-worker-sandbox");
		properties.put(SapsPropertiesConstants.WORKER_OPERATING_SYSTEM, operatingSystem);
		properties.put(SapsPropertiesConstants.WORKER_KERNEL_VERSION, kernelVersion);
		properties.put(sapsTask.SAPS_WORKER_RUN_SCRIPT_PATH, scriptFilePath);

		TaskImpl createdTask = sapsTask.createSapsTask(taskImpl, properties, spec, federationMember,
				nfsServerIP, nfsServerPort, workerContainerRepository, workerContainerTag);

		// expect
		Assert.assertEquals(exportPath, createdTask.getMetadata(sapsTask.METADATA_EXPORT_PATH));

		Assert.assertEquals(operatingSystem,
				createdTask.getMetadata(sapsTask.METADATA_WORKER_OPERATING_SYSTEM));

		Assert.assertEquals(kernelVersion,
				createdTask.getMetadata(sapsTask.METADATA_WORKER_KERNEL_VERSION));
		
		fakeScriptFile.delete();
	}
}