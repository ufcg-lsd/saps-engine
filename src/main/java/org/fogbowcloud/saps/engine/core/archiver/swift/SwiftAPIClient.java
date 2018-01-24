package org.fogbowcloud.saps.engine.core.archiver.swift;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.util.ProcessUtil;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;

public class SwiftAPIClient {

    private static final String SWIFT = "swift";
    private static final String OS_AUTH_TOKEN = "--os-auth-token";
    private static final String OS_STORAGE_URL = "--os-storage-url";
    private static final String OBJECT_NAME = "--object-name";
    private static final String SWIFT_COMMAND_LIST = "list";
    private static final String SWIFT_COMMAND_DOWNLOAD = "download";
    private static final String SWIFT_COMMAND_DELETE = "delete";
    private static final String SWIFT_COMMAND_UPLOAD = "upload";
    private static final String SWIFT_COMMAND_POST = "post";
    private static final String SWIFT_HEADER = "--header";
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
		token = null;
	}

    public void start() {
        handleTokenUpdate(Executors.newScheduledThreadPool(1));
        while (token == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                LOGGER.debug("Interrupted", e);
            }
        }
    }

	public void createContainer(String containerName) {
		LOGGER.debug("Creating container " + containerName);
				
		try {
			Process p = buildCreateContainerProcess(containerName);
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Error while creating container " + containerName, e);
		}
    }

	public void deleteContainer(String containerName) {
		LOGGER.debug("Deleting container " + containerName);

		try {
			Process p = buildDeleteContainerProcess(containerName);
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Error while deleting container " + containerName, e);
		}
    }

	protected boolean isContainerEmpty(String containerName) {
        return numberOfFilesInContainer(containerName) == 0;
    }

	public void uploadFile(String containerName, File file, String pseudFolder) throws Exception {
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
		try {
			Process p = buildUploadFileProcess(
                    containerName, file, completeFileName
            );
			p.waitFor();

			if(p.exitValue() != 0) {
                Scanner scanner = new Scanner(p.getErrorStream()).useDelimiter("\n");
                boolean error = false;
                while (scanner.hasNext()) {
                    String n = scanner.next();
                    if (!n.contains("409 Conflict")) {
                        LOGGER.error("Process output: " + n);
                        error = true;
                    }
                }
                if (error) {
                    throw new Exception("process_output=" + p.exitValue());
                }
			}
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Error while uploading file " + completeFileName
					+ " to container " + containerName);
			throw e;
		}
    }

	public void downloadFile(String containerName, String fileName,
			String pseudFolder, String localOutputPath) {
	    // TODO unit test
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
		ProcessBuilder builder = buildDownloadFileProcess(
		        containerName, fileName, completeFileName, localOutputPath
        );
		
		try {
			Process p = builder.start();
			p.waitFor();
			
			LOGGER.debug("File " + completeFileName + " from " + containerName
					+ " download successfully into " + localOutputPath);
		} catch (IOException | InterruptedException e) {
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
		ProcessBuilder builder = buildDeleteFileProcess(containerName, completeFileName);

		try {
			Process p = builder.start();
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Error while deleting file " + completeFileName
					+ " from container " + containerName, e);
		}

        LOGGER.debug("Object " + completeFileName
				+ " deleted successfully from " + containerName);
	}

    protected int numberOfFilesInContainer(String containerName) {
		// TODO: test JUnit
		LOGGER.debug("containerName " + containerName);
		
		ProcessBuilder builder = buildNumberOfFilesInContainerProcess(containerName);

		try {
			Process p = builder.start();
			p.waitFor();
						
			String commandOutput = ProcessUtil.getOutput(p);
			
			for(int i = 0; i < commandOutput.length(); i++) {
				if(Character.isDigit(commandOutput.charAt(i))) {
					return Character.getNumericValue(commandOutput.charAt(i));
				}
			}
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Error while getting number of files in " + containerName, e);
		}

        return 0;
	}

    public List<String> listFilesInContainer(String containerName) {
		LOGGER.info("Listing files in container " + containerName);
		ProcessBuilder builder = buildListFilesInContainerProcess(containerName);
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
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Error while listing files from " + containerName);
			return new ArrayList<>();
		}
    }

    public List<String> listFilesWithPrefix(String containerName, String prefix) {
		LOGGER.info("Listing files in container " + containerName + " with prefix " + prefix);
		ProcessBuilder builder = buildListFilesWithPrefixProcess(containerName, prefix);
		LOGGER.debug("Executing command " + builder.command());
        
        Process p;
        String output;
        
        try {
			p = builder.start();
			p.waitFor();

			output = ProcessUtil.getOutput(p);

			return getOutputLinesIntoList(output);
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Error while listing files from " + containerName);
			return new ArrayList<>();
		}
    }

    private List<String> getOutputLinesIntoList(String fileNames) {

        String[] lines = fileNames.split(System.getProperty("line.separator"));

        return new ArrayList<>(Arrays.asList(lines));
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
			ProcessBuilder builder = buildGenerateTokenProcess();

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
		
		handleTokenUpdateExecutor.scheduleWithFixedDelay(
		        new Runnable() {
		            @Override
                    public void run() {
		                setToken(generateToken());
		            }
		            },
                0,
                Integer.parseInt(
                        properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_UPDATE_PERIOD)
                ),
                TimeUnit.MILLISECONDS
        );
	}
	
	protected void setToken(String token) {
		LOGGER.debug("Setting token to " + token);
		this.token = token;
	}

    Process buildCreateContainerProcess(String containerName) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(SWIFT, OS_AUTH_TOKEN, token,
                OS_STORAGE_URL, swiftUrl, SWIFT_COMMAND_POST, SWIFT_HEADER,
                SapsPropertiesConstants.SWIFT_CONTAINER_POST_HEADER, containerName);
        return builder.start();
    }

    Process buildDeleteContainerProcess(String containerName) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(SWIFT, OS_AUTH_TOKEN,
                token, OS_STORAGE_URL, swiftUrl, SWIFT_COMMAND_DELETE, containerName);
        return builder.start();
    }

    Process buildUploadFileProcess(String containerName, File file, String completeFileName) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(SWIFT, OS_AUTH_TOKEN,
                token, OS_STORAGE_URL, swiftUrl, SWIFT_COMMAND_UPLOAD, containerName,
                file.getAbsolutePath(), OBJECT_NAME, completeFileName);
        return builder.start();
    }

    ProcessBuilder buildDownloadFileProcess(String containerName, String fileName, String completeFileName, String localOutputPath) {
        return new ProcessBuilder(SWIFT, OS_AUTH_TOKEN,
                token, OS_STORAGE_URL, swiftUrl, SWIFT_COMMAND_DOWNLOAD, containerName,
                completeFileName, "-o", localOutputPath + File.separator
                + fileName);
    }

    ProcessBuilder buildDeleteFileProcess(String containerName, String completeFileName) {
        return new ProcessBuilder(SWIFT, OS_AUTH_TOKEN,
                token, OS_STORAGE_URL, swiftUrl, SWIFT_COMMAND_DELETE, containerName,
                completeFileName);
    }

    ProcessBuilder buildNumberOfFilesInContainerProcess(String containerName) {
        return new ProcessBuilder(SWIFT, OS_AUTH_TOKEN,
                token, OS_STORAGE_URL, swiftUrl, SWIFT_COMMAND_LIST, "-l", containerName);
    }

    ProcessBuilder buildListFilesInContainerProcess(String containerName) {
        return new ProcessBuilder(SWIFT, OS_AUTH_TOKEN,
                token, OS_STORAGE_URL,
                properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_SWIFT_URL), SWIFT_COMMAND_LIST,
                containerName);
    }

    ProcessBuilder buildListFilesWithPrefixProcess(String containerName, String prefix) {
        return new ProcessBuilder(SWIFT, OS_AUTH_TOKEN, token, OS_STORAGE_URL,
                properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_SWIFT_URL),
                SWIFT_COMMAND_LIST, "-p", prefix, containerName);
    }

    ProcessBuilder buildGenerateTokenProcess() {
        return new ProcessBuilder("bash",
                properties.get(SapsPropertiesConstants.FOGBOW_CLI_PATH) + File.separator
                        + "bin/fogbow-cli", "token", "--create",
                "-DprojectId=" + projectId, "-DuserId=" + userId,
                "-Dpassword=" + userPassword, "-DauthUrl=" + tokenAuthUrl,
                "--type", "openstack");
    }
}
