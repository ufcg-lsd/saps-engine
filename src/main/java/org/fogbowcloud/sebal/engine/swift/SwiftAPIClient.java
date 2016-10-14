package org.fogbowcloud.sebal.engine.swift;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.sebal.ProcessUtil;

public class SwiftAPIClient {

	// Properties
	private Properties properties;

	// Core Attributes
	private String projectId;
	private String userId;
	private String userPassword;
	private String tokenAuthUrl;
	private String swiftUrl;
	private String token;

	// Constants
	private static final String URL_PATH_SEPARATOR = "/";
	private static final String FOGBOW_CLI_PATH = "fogbow_cli_path";
	private static final String FOGBOW_KEYSTONEV3_PROJECT_ID = "fogbow.keystonev3.project.id";
	private static final String FOGBOW_KEYSTONEV3_USER_ID = "fogbow.keystonev3.user.id";
	private static final String FOGBOW_KEYSTONEV3_PASSWORD = "fogbow.keystonev3.password";
	private static final String FOGBOW_KEYSTONEV3_AUTH_URL = "fogbow.keystonev3.auth.url";
	private static final String FOGBOW_KEYSTONEV3_SWIFT_URL = "fogbow.keystonev3.swift.url";

	public static final Logger LOGGER = Logger.getLogger(SwiftAPIClient.class);

	public SwiftAPIClient(Properties properties) {
		this.properties = properties;

		projectId = properties.getProperty(FOGBOW_KEYSTONEV3_PROJECT_ID);
		userId = properties.getProperty(FOGBOW_KEYSTONEV3_USER_ID);
		userPassword = properties.getProperty(FOGBOW_KEYSTONEV3_PASSWORD);
		tokenAuthUrl = properties.getProperty(FOGBOW_KEYSTONEV3_AUTH_URL);
		swiftUrl = properties.getProperty(FOGBOW_KEYSTONEV3_SWIFT_URL);

		token = generateToken();
	}

	public void createContainer(String containerName) {
		// TODO: test
		LOGGER.debug("Creating container " + containerName);
		ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token",
				token, "--os-storage-url", swiftUrl, "post", containerName);
				
		try {
			Process p = builder.start();
			p.waitFor();
		} catch (IOException e) {
			LOGGER.error("Error while creating container " + containerName, e);
		} catch (InterruptedException e) {
			LOGGER.error("Error while creating container " + containerName, e);
		}
	}

	public void deleteContainer(String containerName) {
		// TODO: test
		LOGGER.debug("Deleting container " + containerName);
		ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token",
				token, "--os-storage-url", swiftUrl, "delete", containerName);

		try {
			Process p = builder.start();
			p.waitFor();
		} catch (IOException e) {
			LOGGER.error("Error while deleting container " + containerName, e);
		} catch (InterruptedException e) {
			LOGGER.error("Error while deleting container " + containerName, e);
		}
	}

	public boolean isContainerEmpty(String containerName) {
		// TODO: test
		if (numberOfFilesInContainer(containerName) <= 3) {
			return true;
		}

		return false;
	}

	public void uploadFile(String containerName, File file, String pseudFolder)
			throws Exception {
		// TODO: test
		LOGGER.debug("containerName " + containerName);
		LOGGER.debug("pseudoFolder " + pseudFolder + " before normalize");

		String completeFileName;
		if (pseudFolder != null && !pseudFolder.isEmpty()) {
			pseudFolder = this.normalizePseudFolder(pseudFolder);
			LOGGER.debug("Pseudo folder " + pseudFolder + " after normalize");

			completeFileName = pseudFolder + file.getName();
		} else {
			completeFileName = file.getName();
		}

		LOGGER.debug("Uploading " + completeFileName + " to " + containerName);
		ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token",
				token, "--os-storage-url", swiftUrl, "upload", containerName,
				completeFileName);
		try {
			Process p = builder.start();
			p.waitFor();
		} catch (IOException e) {
			LOGGER.error("Error while uploading file " + completeFileName
					+ " to container " + containerName, e);
		} catch (InterruptedException e) {
			LOGGER.error("Error while uploading file " + completeFileName
					+ " to container " + containerName, e);
		}
	}

	public byte[] downloadFile(String containerName, String fileName,
			String pseudFolder) {
		// TODO
		LOGGER.debug("containerName " + containerName);
		LOGGER.debug("pseudoFolder " + pseudFolder + " before normalize");

		String completeFileName;
		if (pseudFolder != null && !pseudFolder.isEmpty()) {
			pseudFolder = this.normalizePseudFolder(pseudFolder);
			LOGGER.debug("Pseudo folder " + pseudFolder + " after normalize");

			completeFileName = pseudFolder + fileName;
		} else {
			completeFileName = fileName;
		}

		LOGGER.debug("Uploading " + completeFileName + " to " + containerName);
		ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token",
				token, "--os-storage-url", swiftUrl, "download", containerName,
				completeFileName);
		try {
			Process p = builder.start();
			p.waitFor();
			
			// TODO: see how output will be handled
		} catch (IOException e) {
			LOGGER.error("Error while uploading file " + completeFileName
					+ " to container " + containerName, e);
		} catch (InterruptedException e) {
			LOGGER.error("Error while uploading file " + completeFileName
					+ " to container " + containerName, e);
		}

		return null;
	}

	public void deleteFile(String containerName, String pseudFolder,
			String fileName) {
		// TODO: test
		LOGGER.debug("fileName " + fileName);
		LOGGER.debug("containerName " + containerName);

		String completeFileName;
		if (pseudFolder != null && !pseudFolder.isEmpty()) {
			pseudFolder = this.normalizePseudFolder(pseudFolder);
			LOGGER.debug("Pseudo folder " + pseudFolder + " after normalize");

			completeFileName = pseudFolder + fileName;
		} else {
			completeFileName = fileName;
		}

		LOGGER.debug("Deleting " + completeFileName + " from " + containerName);
		ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token",
				token, "--os-storage-url", swiftUrl, "delete", containerName,
				completeFileName);

		try {
			Process p = builder.start();
			p.waitFor();
		} catch (IOException e) {
			LOGGER.error("Error while deleting file " + completeFileName
					+ " from container " + containerName, e);
		} catch (InterruptedException e) {
			LOGGER.error("Error while deleting file " + completeFileName
					+ " from container " + containerName, e);
		}

		LOGGER.debug("Object " + completeFileName
				+ " deleted successfully from " + containerName);
	}

	public int numberOfFilesInContainer(String containerName) {
		// TODO: test
		LOGGER.debug("containerName " + containerName);
		
		ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token",
				token, "--os-storage-url", swiftUrl, "stat", "--lh", containerName);

		try {
			Process p = builder.start();
			p.waitFor();
						
			String[] splitCommandOutput = ProcessUtil.getOutput(p).split("\n");		
			
			for(String line : splitCommandOutput) {
				if(line.contains("Objects:")) {
					String[] lineSplit = line.split("Objects:");
					
					return Integer.valueOf(lineSplit[1]);						
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error while getting number of files in " + containerName, e);
		} catch (InterruptedException e) {
			LOGGER.error("Error while getting number of files in " + containerName, e);
		}
				
		return 0;
	}

	private String normalizePseudFolder(String value) {
		StringBuilder normalizedPath = new StringBuilder();
		// Path cannot have separator "/" in begin.
		if (value.startsWith(URL_PATH_SEPARATOR)) {
			value = value.substring(1, value.length());
		}
		normalizedPath.append(value);
		if (!value.endsWith(URL_PATH_SEPARATOR)) {
			normalizedPath.append(URL_PATH_SEPARATOR);
		}
		return normalizedPath.toString();
	}

	protected String generateToken() {

		try {
			ProcessBuilder builder = new ProcessBuilder("bash",
					properties.get(FOGBOW_CLI_PATH) + File.separator
							+ "bin/fogbow-cli", "token", "--create",
					"-DprojectId=" + projectId, "-DuserId=" + userId,
					"-Dpassword=" + userPassword, "-DauthUrl=" + tokenAuthUrl,
					"--type", "openstack");

			LOGGER.debug("Executing command " + builder.command());

			Process p = builder.start();
			p.waitFor();

			return ProcessUtil.getOutput(p);
		} catch (Throwable e) {
			LOGGER.error("Error while generating keystone token", e);
		}

		return null;
	}
}
