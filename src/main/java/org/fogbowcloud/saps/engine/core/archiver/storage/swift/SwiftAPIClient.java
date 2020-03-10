package org.fogbowcloud.saps.engine.core.archiver.storage.swift;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.util.ProcessUtil;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesUtil;

public class SwiftAPIClient {

    private static final Logger LOGGER = Logger.getLogger(SwiftAPIClient.class);
    private static final String CONTAINER_URL_PATTERN = "%s/%s?path=%s";
    // 30 Min
    private static final int DEFAULT_TOKEN_UPDATE_PERIOD = 1800000;

    private final String swiftUrl;
    private final String tokenAuthUrl;
    private final String projectId;
    private final String userId;
    private final String userPassword;
    private final String fogbowCliPath;
    private final int tokenUpdatePeriod;
    private final ScheduledExecutorService executorService;

    private String token;

    SwiftAPIClient(Properties properties) throws SwiftPermanentStorageException {
        if (!checkProperties(properties))
            throw new SwiftPermanentStorageException("Error on validate the file. Missing properties for start Swift API Client.");
        swiftUrl = properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_SWIFT_URL);
        tokenAuthUrl = properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_AUTH_URL);
        projectId = properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PROJECT_ID);
        userId = properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_USER_ID);
        userPassword = properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PASSWORD);
        fogbowCliPath = properties.getProperty(SapsPropertiesConstants.FOGBOW_CLI_PATH);
        String tokenUpdatePeriodStr = properties.getProperty(SapsPropertiesConstants.FOGBOW_KEYSTONEV3_UPDATE_PERIOD);
        tokenUpdatePeriod = Objects.isNull(tokenUpdatePeriodStr) || tokenUpdatePeriodStr.trim().isEmpty() ? DEFAULT_TOKEN_UPDATE_PERIOD : Integer.parseInt(tokenUpdatePeriodStr);
        executorService = Executors.newScheduledThreadPool(1);
    }

    private boolean checkProperties(Properties properties) {
        String[] propertiesSet = {
            SapsPropertiesConstants.FOGBOW_KEYSTONEV3_SWIFT_URL,
            SapsPropertiesConstants.FOGBOW_KEYSTONEV3_AUTH_URL,
            SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PROJECT_ID,
            SapsPropertiesConstants.FOGBOW_KEYSTONEV3_USER_ID,
            SapsPropertiesConstants.FOGBOW_KEYSTONEV3_PASSWORD,
            SapsPropertiesConstants.FOGBOW_CLI_PATH
        };

        return SapsPropertiesUtil.checkProperties(properties, propertiesSet);
    }

    void startTokenUpdateRoutine() {
        LOGGER.debug("Turning on handle token update.");
        executorService.scheduleWithFixedDelay(this::updateToken, 0, this.tokenUpdatePeriod,
            TimeUnit.MILLISECONDS);

        LOGGER.info("Waiting to get token...");
        while (token == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateToken() {
        String newToken = generateToken();
        LOGGER.debug("Setting token to " + token);
        this.token = newToken;
    }

    //TODO Throws exception when container creation was not success
    void createContainer(String containerName) {
        // TODO: test JUnit
        LOGGER.debug("Creating container " + containerName);
        ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token", token, "--os-storage-url", swiftUrl,
                "post", containerName);

        LOGGER.debug("Executing command " + builder.command());

        try {
            Process p = builder.start();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error while creating container " + containerName, e);
        }
    }

    public void uploadFile(String containerName, File file, String pseudFolder) throws Exception {
        String completeFileName = pseudFolder + File.separator + file.getName();

        LOGGER.debug("Uploading " + completeFileName + " to " + containerName);
        ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token", token, "--os-storage-url", swiftUrl,
                "upload", containerName, file.getAbsolutePath(), "--object-name", completeFileName);
        try {
            Process p = builder.start();
            p.waitFor();

            if (p.exitValue() != 0) {
                throw new Exception("process_output=" + p.exitValue());
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error while uploading file " + completeFileName + " to container " + containerName, e);
        }
    }

    public void deleteFile(String containerName, String filePath) {
        LOGGER.debug("Deleting " + filePath + " from " + containerName);
        ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token", token, "--os-storage-url", swiftUrl,
                "delete", containerName, filePath);

        try {
            Process p = builder.start();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error while deleting " + filePath + " from container " + containerName, e);
        }

        LOGGER.debug(filePath + " file deleted successfully from " + containerName);
    }

    private String generateToken() {

        try {
            ProcessBuilder builder = new ProcessBuilder("bash",
                    this.fogbowCliPath + File.separator + "bin/fogbow-cli",
                    "token", "--create", "-DprojectId=" + projectId, "-DuserId=" + userId, "-Dpassword=" + userPassword,
                    "-DauthUrl=" + tokenAuthUrl, "--type", "openstack");

            LOGGER.debug("Executing command " + builder.command());

            Process p = builder.start();
            p.waitFor();

            return ProcessUtil.getOutput(p);
        } catch (Throwable e) {
            LOGGER.error("Error while generating keystone token", e);
        }

        return null;
    }

    public List<String> listFiles(String containerName, String dirPath) throws IOException {
        String url = String.format(CONTAINER_URL_PATTERN, swiftUrl, containerName, dirPath);
        HttpClient client = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        httpget.addHeader("X-Auth-Token", token);
        HttpResponse response = client.execute(httpget);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("The request to list files on object storage was failed: " + EntityUtils.toString(response.getEntity()));
        }
        return Arrays.asList(EntityUtils.toString(response.getEntity()).split("\n"));
    }

    boolean existsTask(String containerName, String basePath, String taskId) throws IOException {
        List<String> files = this.listFiles(containerName, basePath);
        for(String filePath : files) {
            if(Paths.get(filePath).getFileName().toString().equals(taskId)) {
                return true;
            }
        }
        return false;
    }
}
