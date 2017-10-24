package org.fogbowcloud.saps.engine.core.util;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class DockerUtilTests {

	private String repository;
	private String tag;
	private String containerPath;
	private String hostPath;
	private File hostDir;
	private final String fileTouch = "testFile";

	@Before
	public void setUp() {
		repository = "ubuntu";
		tag = "latest";
		containerPath = File.separator + "tmp" + File.separator + "sapsTest";
		hostPath = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "sapsTest";
		hostDir = new File(hostPath);
		hostDir.mkdirs();
	}

	@After
	public void finallyTest() throws IOException {
		FileUtils.deleteDirectory(hostDir);
	}

	@Test
	public void pullDockerImageTest() {
		// removes docker image if this repo is in our image list.
		DockerUtil.removeImage(repository);
		boolean imageExists = (DockerUtil.execDockerInspect("image", repository) == 0);
		Assert.assertFalse(imageExists);

		// pull and check if it has been done
		DockerUtil.pullImage(repository, tag);
		imageExists = (DockerUtil.execDockerInspect("image", repository) == 0);
		Assert.assertTrue(imageExists);
	}

	@Test
	public void commandExecAndRunMappedDockerTest() {
		// Should touch a new file inside a container
		String command = "mkdir -p " + containerPath;
		String containerId = DockerUtil.runMappedContainer(repository, tag, hostPath,
				containerPath);
		int exitValue = DockerUtil.execDockerCommand(containerId, command);
		Assert.assertEquals(0, exitValue);

		String touchCommand = "touch " + containerPath + File.separator + fileTouch;
		exitValue = DockerUtil.execDockerCommand(containerId, touchCommand);
		Assert.assertEquals(0, exitValue);

		// Assert the file exists in host.
		File hostFile = new File(hostPath + File.separator + fileTouch);
		Assert.assertEquals(true, hostFile.exists());

		DockerUtil.removeContainer(containerId);
	}

	@Test
	public void killDockerContainerTest() {
		// inspect container
		String containerId = DockerUtil.runMappedContainer(repository, tag, hostPath,
				containerPath);
		boolean isRunning = (DockerUtil.execDockerInspect("container", containerId) == 0);
		Assert.assertTrue(isRunning);

		// A running container should be killed
		DockerUtil.removeContainer(containerId);
		isRunning = (DockerUtil.execDockerInspect("container", containerId) == 0);
		Assert.assertFalse(isRunning);
	}
}