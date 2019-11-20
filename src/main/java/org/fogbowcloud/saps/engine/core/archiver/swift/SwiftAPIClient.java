package org.fogbowcloud.saps.engine.core.archiver.swift;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.util.ProcessUtil;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;

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

	public static final Logger LOGGER = Logger.getLogger(SwiftAPIClient.class);


	public SwiftAPIClient(Properties properties) {
		this.properties = properties;
		
		projectId = properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PROJECT_ID);
		userId = properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_USER_ID);
		userPassword = properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PASSWORD);
		tokenAuthUrl = properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_AUTH_URL);
		swiftUrl = properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_SWIFT_URL);

		handleTokenUpdate(Executors.newScheduledThreadPool(1));
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			LOGGER.error(e);
		}
	}

	public void createContainer(String containerName) {
		// TODO: test JUnit
		LOGGER.debug("Creating container " + containerName);
		ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token", token,
				"--os-storage-url", swiftUrl, "post", "--header",
				SapsPropertiesConstants.SWIFT_CONTAINER_POST_HEADER, containerName);
				
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
		// TODO: test JUnit
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

	protected boolean isContainerEmpty(String containerName) {
		// TODO: test JUnit
		int numberOfFiles = numberOfFilesInContainer(containerName);
		if (numberOfFiles != 0) {			
			return false;
		}

		return true;
	}

	public void uploadFile(String containerName, File file, String pseudFolder)
			throws Exception {
		// TODO: test JUnit
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
				file.getAbsolutePath(), "--object-name", completeFileName);
		try {
			Process p = builder.start();
			p.waitFor();
			
			if(p.exitValue() != 0) {
				throw new Exception("process_output=" + p.exitValue());
			}
		} catch (IOException e) {
			LOGGER.error("Error while uploading file " + completeFileName
					+ " to container " + containerName, e);
		} catch (InterruptedException e) {
			LOGGER.error("Error while uploading file " + completeFileName
					+ " to container " + containerName, e);
		}		
	}

	public void downloadFile(String containerName, String fileName,
			String pseudFolder, String localOutputPath) {
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

		LOGGER.debug("Downloading " + completeFileName + " to " + containerName);
		ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token",
				token, "--os-storage-url", swiftUrl, "download", containerName,
				completeFileName, "-o", localOutputPath + File.separator
						+ fileName);
		
		try {
			Process p = builder.start();
			p.waitFor();
			
			LOGGER.debug("File " + completeFileName + " from " + containerName
					+ " download successfully into " + localOutputPath);
		} catch (IOException e) {
			LOGGER.error("Error while uploading file " + completeFileName
					+ " to container " + containerName, e);
		} catch (InterruptedException e) {
			LOGGER.error("Error while uploading file " + completeFileName
					+ " to container " + containerName, e);
		}
	}

	public void deleteFile(String containerName, String pseudFolder,
			String fileName) {
		// TODO: test JUnit
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

	protected int numberOfFilesInContainer(String containerName) {
		// TODO: test JUnit
		LOGGER.debug("containerName " + containerName);
		
		ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token",
				token, "--os-storage-url", swiftUrl, "list", "-l", containerName);

		try {
			Process p = builder.start();
			p.waitFor();
						
			String commandOutput = ProcessUtil.getOutput(p);
			
			for(int i = 0; i < commandOutput.length(); i++) {
				if(Character.isDigit(commandOutput.charAt(i))) {
					return Character.getNumericValue(commandOutput.charAt(i));
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error while getting number of files in " + containerName, e);
		} catch (InterruptedException e) {
			LOGGER.error("Error while getting number of files in " + containerName, e);
		}
				
		return 0;
	}
	
	public List<String> listFilesInContainer(String containerName) {
		LOGGER.info("Listing files in container " + containerName);
		ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token",
				token, "--os-storage-url",
				properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_SWIFT_URL), "list",
				containerName);
		LOGGER.debug("Executing command " + builder.command());
        
        Process p;
        String output;
        
        try {
			p = builder.start();
			p.waitFor();

			output = ProcessUtil.getOutput(p);

			// TODO: test this
			// each file has complete path
			// ex.: fetcher/images/image_name/file.nc
			return getOutputLinesIntoList(output);
		} catch (IOException e) {
			LOGGER.error("Error while listing files from " + containerName);
			return new ArrayList<String>();
		} catch (InterruptedException e) {
			LOGGER.error("Error while listing files from " + containerName);
			return new ArrayList<String>();
		}        
	}
	
	public List<String> listFilesWithPrefix(String containerName, String prefix) {
		LOGGER.info("Listing files in container " + containerName + " with prefix " + prefix);
		ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token", token, "--os-storage-url", 
				properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_SWIFT_URL),
				"list", "-p", prefix, containerName);
		LOGGER.debug("Executing command " + builder.command());
        
        Process p;
        String output;
        
        try {
			p = builder.start();
			p.waitFor();

			output = ProcessUtil.getOutput(p);

			return getOutputLinesIntoList(output);
		} catch (IOException e) {
			LOGGER.error("Error while listing files from " + containerName);
			return new ArrayList<String>();
		} catch (InterruptedException e) {
			LOGGER.error("Error while listing files from " + containerName);
			return new ArrayList<String>();
		}        
	}
	
	private List<String> getOutputLinesIntoList(String fileNames) throws IOException {
		List<String> fileNamesList = new ArrayList<String>();
		
		String[] lines = fileNames.split(System.getProperty("line.separator"));
		
		for(int i = 0; i < lines.length; i++) {
			fileNamesList.add(lines[i]);
		}
		
		return fileNamesList;
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
					properties.get(SapsPropertiesConstants.FOGBOW_CLI_PATH) + File.separator
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
	
	protected void handleTokenUpdate(ScheduledExecutorService handleTokenUpdateExecutor) {
		LOGGER.debug("Turning on handle token update.");
		
		handleTokenUpdateExecutor.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				setToken(generateToken());
			}
		}, 0, Integer.parseInt(properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_UPDATE_PERIOD)), TimeUnit.MILLISECONDS);
	}
	
	protected void setToken(String token) {
		LOGGER.debug("Setting token to " + token);
		this.token = token;
	}
}
