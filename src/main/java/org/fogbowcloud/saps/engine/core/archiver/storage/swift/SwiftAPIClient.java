package org.fogbowcloud.saps.engine.core.archiver.storage.swift;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.InvalidPropertyException;
import org.fogbowcloud.saps.engine.core.util.ProcessUtil;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesUtil;

public class SwiftAPIClient {

    private static final Logger LOGGER = Logger.getLogger(SwiftAPIClient.class);

    private final String swiftUrl;
    private final String authUrl;
    private final String projectId;
    private final String userId;
    private final String userPassword;

    SwiftAPIClient(Properties properties)
        throws InvalidPropertyException {
        if (!checkProperties(properties))
            throw new InvalidPropertyException("Error on validate the file. Missing properties for start Swift API Client.");

        this.projectId = properties.getProperty(SapsPropertiesConstants.Openstack.PROJECT_ID);
        this.userId = properties.getProperty(SapsPropertiesConstants.Openstack.USER_ID);
        this.userPassword = properties.getProperty(SapsPropertiesConstants.Openstack.USER_PASSWORD);
        this.authUrl = properties.getProperty(SapsPropertiesConstants.Openstack.IdentityService.API_URL);
        swiftUrl = properties.getProperty(SapsPropertiesConstants.Openstack.ObjectStoreService.API_URL);
        }

    private boolean checkProperties(Properties properties) {
        String[] propertiesSet = {
                SapsPropertiesConstants.Openstack.PROJECT_ID,
                SapsPropertiesConstants.Openstack.USER_ID,
                SapsPropertiesConstants.Openstack.USER_PASSWORD,
                SapsPropertiesConstants.Openstack.IdentityService.API_URL,
                SapsPropertiesConstants.Openstack.ObjectStoreService.API_URL
        };

        return SapsPropertiesUtil.checkProperties(properties, propertiesSet);
    }

    //TODO Throws exception when container creation was not success
    public void createContainer(String containerName) throws Exception {
        // TODO: test JUnit
        LOGGER.debug("Creating container " + containerName);
        String cmd[] = buildCliCommand("post", containerName);
        ProcessBuilder builder = new ProcessBuilder(cmd);

        LOGGER.debug("Executing command " + builder.command());

        Process p = builder.start();
        p.waitFor();

        if (p.exitValue() != 0) {
            throw new Exception("Error while creating swift container [" + containerName + "]: " + IOUtils.toString(p.getErrorStream()));
        }
    }

    public void uploadFile(String containerName, File file, String pseudFolder) throws Exception {
        String completeFileName = pseudFolder + File.separator + file.getName();

        LOGGER.debug("Uploading " + completeFileName + " to " + containerName);
        String cmd[] = buildCliCommand("upload", containerName, file.getAbsolutePath(), "--object-name", completeFileName);
        ProcessBuilder builder = new ProcessBuilder(cmd);
        Process p = builder.start();
        p.waitFor();

        if (p.exitValue() != 0) {
            throw new Exception("Error while upload file [" + completeFileName + "] to swift container [" + containerName + "]: " + IOUtils.toString(p.getErrorStream()));
        }
    }

    public void deleteFile(String containerName, String filePath) throws Exception {
        LOGGER.debug("Deleting " + filePath + " from " + containerName);
        String cmd[] = buildCliCommand("delete", containerName, filePath);
        ProcessBuilder builder = new ProcessBuilder(cmd);

        Process p = builder.start();
        p.waitFor();

        if (p.exitValue() != 0) {
            throw new Exception("Error while delete file [" + filePath + "] to swift container [" + containerName + "]: " + IOUtils.toString(p.getErrorStream()));
        }

        LOGGER.debug(filePath + " file deleted successfully from " + containerName);
    }

    public List<String> listFiles(String containerName, String dirPath) throws Exception {
        LOGGER.info("Listing files in path [" + dirPath + "] from container [" + containerName + "]");
        String cmd[] = buildCliCommand("list", "-p", dirPath, containerName);
        ProcessBuilder builder = new ProcessBuilder(cmd);

        Process p = builder.start();
        p.waitFor();

        if (p.exitValue() != 0) {
            throw new IOException("Error while listing files in path [" + dirPath + "] from container [" + containerName + "]" + IOUtils.toString(p.getErrorStream()));
        }

        String output = ProcessUtil.getOutput(p);
        return Arrays.asList(output.split(System.lineSeparator()));
    }

    public boolean existsTask(String containerName, String basePath, String taskId) throws IOException {
        try {
            String taskDir = basePath + "/" + taskId;
            List<String> files = this.listFiles(containerName, taskDir);
            if(files.isEmpty()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            throw new IOException("Error while check if task [" + taskId + "] exists on container [" + containerName + "]", e);
        }
    }

    private String[] buildCliCommand(String... args) {
        List<String> defaultArgs = new ArrayList<>(Arrays.asList("swift",
                "--os-auth-url", this.authUrl,
                "--auth-version", "3",
                "--os-project-id", this.projectId,
                "--os-user-id", this.userId,
                "--os-password", this.userPassword,
                "--os-storage-url", this.swiftUrl));
        Collections.addAll(defaultArgs, args);
        String[] cmd = new String[defaultArgs.size()];
        defaultArgs.toArray(cmd);
        return cmd;
    }
}
