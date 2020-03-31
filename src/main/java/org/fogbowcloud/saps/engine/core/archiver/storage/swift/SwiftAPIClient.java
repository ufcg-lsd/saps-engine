package org.fogbowcloud.saps.engine.core.archiver.storage.swift;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.InvalidPropertyException;
import org.fogbowcloud.saps.engine.core.dispatcher.email.keystone.IdentityToken;
import org.fogbowcloud.saps.engine.core.dispatcher.email.keystone.KeystoneV3IdentityRequestHelper;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesUtil;
import org.json.JSONException;

public class SwiftAPIClient {

    private static final Logger LOGGER = Logger.getLogger(SwiftAPIClient.class);
    private static final String CONTAINER_URL_PATTERN = "%s/%s?path=%s";

    private final String swiftUrl;

    private IdentityToken token;

    SwiftAPIClient(Properties properties)
        throws InvalidPropertyException, IOException, JSONException {
        if (!checkProperties(properties))
            throw new InvalidPropertyException("Error on validate the file. Missing properties for start Swift API Client.");

        String projectId = properties.getProperty(SapsPropertiesConstants.Openstack.PROJECT_ID);
        String userId = properties.getProperty(SapsPropertiesConstants.Openstack.USER_ID);
        String userPassword = properties.getProperty(SapsPropertiesConstants.Openstack.USER_PASSWORD);
        String tokenAuthUrl = properties.getProperty(SapsPropertiesConstants.Openstack.IdentityService.API_URL);
        swiftUrl = properties.getProperty(SapsPropertiesConstants.Openstack.ObjectStoreService.API_URL);

        this.token = KeystoneV3IdentityRequestHelper.createIdentityToken(tokenAuthUrl, projectId,
                userId, userPassword);
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
        ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token", token.getAccessId(), "--os-storage-url", swiftUrl,
                "post", containerName);

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
        ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token", token.getAccessId(), "--os-storage-url", swiftUrl,
                "upload", containerName, file.getAbsolutePath(), "--object-name", completeFileName);
        Process p = builder.start();
        p.waitFor();

        if (p.exitValue() != 0) {
            throw new Exception("Error while upload file [" + completeFileName + "] to swift container [" + containerName + "]: " + IOUtils.toString(p.getErrorStream()));
        }
    }

    public void deleteFile(String containerName, String filePath) throws Exception {
        LOGGER.debug("Deleting " + filePath + " from " + containerName);
        ProcessBuilder builder = new ProcessBuilder("swift", "--os-auth-token", token.getAccessId(), "--os-storage-url", swiftUrl,
                "delete", containerName, filePath);

        Process p = builder.start();
        p.waitFor();

        if (p.exitValue() != 0) {
            throw new Exception("Error while delete file [" + filePath + "] to swift container [" + containerName + "]: " + IOUtils.toString(p.getErrorStream()));
        }

        LOGGER.debug(filePath + " file deleted successfully from " + containerName);
    }

    public List<String> listFiles(String containerName, String dirPath) throws IOException {
        List<String> files = new ArrayList<>();
        String url = String.format(CONTAINER_URL_PATTERN, swiftUrl, containerName, dirPath);
        HttpClient client = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        httpget.addHeader("X-Auth-Token", token.getAccessId());
        HttpResponse response = client.execute(httpget);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK && response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            throw new IOException("The request to list files on object storage was failed: " + EntityUtils.toString(response.getEntity()));
        }
        if(Objects.nonNull(response.getEntity())) {
            files = Arrays.asList(EntityUtils.toString(response.getEntity()).split("\n"));
        }
        return files;
    }

    public boolean existsTask(String containerName, String basePath, String taskId) throws IOException {
        List<String> files = this.listFiles(containerName, basePath);
        for(String filePath : files) {
            if(Paths.get(filePath).getFileName().toString().equals(taskId)) {
                return true;
            }
        }
        return false;
    }
}
