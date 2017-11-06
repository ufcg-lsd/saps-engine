package org.fogbowcloud.saps.engine.core.util;

import org.apache.log4j.Logger;

public class DockerUtil {

	public static final Logger LOGGER = Logger.getLogger(DockerUtil.class);

	/**
	 * Get, update (if in earlier version), or ignore (if in the latest version) a
	 * Docker image.
	 *
	 * @param repositoryName
	 *            The repository name.
	 * @param repositoryTag
	 *            The tag name.
	 * @return True, if it was successfully pulled, if raised and error, False.
	 */
	public static boolean pullImage(String repositoryName, String repositoryTag) {
		ProcessBuilder builder = new ProcessBuilder("docker", "pull",
				repositoryName + ":" + repositoryTag);
		LOGGER.debug("Pulling Docker image: " + builder.command());

		try {
			Process p = builder.start();
			p.waitFor();
			LOGGER.debug("Pull status output: " + p.exitValue());
			if (p.exitValue() == 0) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Error to pull Docker image " + repositoryName + ":" + repositoryTag + ".",
					e);
		}
		return false;
	}

	/**
	 * Runs a container of a given image repository and tag, but mapping a host
	 * directory to a container's one.
	 *
	 * @param repository
	 *            The Docker image repository name.
	 * @param tag
	 *            The Docker tag name.
	 * @param hostPath
	 *            A directory path in host.
	 * @param containerPath
	 *            A directory path into the container.
	 * @return The running container ID.
	 */
	public static String runMappedContainer(String repository, String tag, String hostPath,
			String containerPath) {
		ProcessBuilder builder = new ProcessBuilder("docker", "run", "-td", "-v",
				hostPath + ":" + containerPath, repository + ":" + tag);
		LOGGER.debug("Running container: " + builder.command());

		try {
			Process p = builder.start();
			p.waitFor();
			LOGGER.debug("Run Docker container output: " + p.exitValue());
			return ProcessUtil.getOutput(p);
		} catch (Exception e) {
			LOGGER.error("Error while running Docker container " + repository + ":" + tag + ".", e);
		}
		return "";
	}

	/**
	 * Removes a specific container.
	 *
	 * @param containerId
	 *            The container ID to be removed.
	 * @return True, if it was successfully removed, otherwise, False.
	 */
	public static boolean removeContainer(String containerId) {
		ProcessBuilder builder = new ProcessBuilder("docker", "rm", "-f", containerId);
		LOGGER.debug("Remove Docker container: " + builder.command());

		try {
			Process p = builder.start();
			p.waitFor();
			LOGGER.debug("Remove Docker container status output: " + p.exitValue());
			if (p.exitValue() == 0) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Error to remove Docker container " + containerId + ".", e);
		}
		return false;
	}

	/**
	 * Removes a specific Docker image.
	 *
	 * @param imageName
	 *            The image name (or id) to be killed.
	 * @return True, if it was successfully removed, otherwise, False.
	 */
	public static boolean removeImage(String imageName) {
		ProcessBuilder builder = new ProcessBuilder("docker", "rmi", imageName);
		LOGGER.debug("Remove Docker container: " + builder.command());

		try {
			Process p = builder.start();
			p.waitFor();
			LOGGER.debug("Remove Docker image status output: " + p.exitValue());
			if (p.exitValue() == 0) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Error to remove Docker image " + imageName + ".", e);
		}
		return false;
	}

	/**
	 * Executes a given command inside a Docker container.
	 * 
	 * @param containerId
	 *            The container Id.
	 * @param command
	 *            The command to be executed.
	 * @return The exit value.
	 */
	public static int execDockerCommand(String containerId, String command) {
		String fullCommand = "docker exec " + containerId + " " + command;
		String[] commandArray = fullCommand.split("\\s+");
		ProcessBuilder builder = new ProcessBuilder(commandArray);
		LOGGER.debug("Executing containerized file: " + builder.command());

		try {
			Process p = builder.start();
			p.waitFor();
			LOGGER.debug("Containerized file execution status output:" + p.exitValue());
			return p.exitValue();
		} catch (Exception e) {
			LOGGER.error("Error while executing containerized command: run.sh"
					+ " on container ID: " + containerId, e);
		}
		return -1;
	}

	/**
	 * Executes a given command inside a Docker container.
	 * 
	 * @param type
	 *            The type, if image or container.
	 * @param filter
	 *            Word to be filtered.
	 * @return The exit value.
	 */
	public static int execDockerInspect(String type, String filter) {
		String fullCommand = "docker inspect --type=" + type + " " + filter;
		String[] commandArray = fullCommand.split("\\s+");
		ProcessBuilder builder = new ProcessBuilder(commandArray);
		LOGGER.debug("Executing containerized file: " + builder.command());

		try {
			Process p = builder.start();
			p.waitFor();
			LOGGER.debug("Docker inspect execution status output:" + p.exitValue());
			return p.exitValue();
		} catch (Exception e) {
			LOGGER.error("Error while executing docker inspect.", e);
		}
		return -1;
	}
}