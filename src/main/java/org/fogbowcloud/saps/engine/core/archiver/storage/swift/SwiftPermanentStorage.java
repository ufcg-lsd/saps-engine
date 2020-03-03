package org.fogbowcloud.saps.engine.core.archiver.storage.swift;

import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.INPUTDOWNLOADING_DIR;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.PREPROCESSING_DIR;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.PROCESSING_DIR;
import static org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorageConstants.SAPS_TASK_STAGE_DIR_PATTERN;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Properties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.storage.AccessLink;
import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.PermanentStorageException;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesUtil;

public class SwiftPermanentStorage implements PermanentStorage {


    private static final Logger LOGGER = Logger.getLogger(SwiftPermanentStorage.class);
    private static final String SWIFT_TASK_STAGE_DIR_PATTERN = "%s" + File.separator + "%s" + File.separator + "%s";
    private static final int MAX_ARCHIVE_TRIES = 1;
    private static final int MAX_SWIFT_UPLOAD_TRIES = 2;
    private static final String TEMP_DIR_URL = "%s?temp_url_sig=%s&temp_url_expires=%s";
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    private final SwiftAPIClient swiftAPIClient;
    //FIXME Remove properties field and add new variables
    private final Properties properties;
    private final String nfsTempStoragePath;
    private final String containerName;
    private final boolean debugMode;
    private final String swiftKey;

    public SwiftPermanentStorage(Properties properties, SwiftAPIClient swiftAPIClient) throws PermanentStorageException {
        if (!checkProperties(properties))
            throw new PermanentStorageException("Error on validate the file. Missing properties for start Swift Permanent Storage.");

        this.swiftAPIClient = swiftAPIClient;
        this.properties = properties;
        this.nfsTempStoragePath = properties.getProperty(SapsPropertiesConstants.SAPS_TEMP_STORAGE_PATH);
        this.containerName = properties.getProperty(SapsPropertiesConstants.SWIFT_CONTAINER_NAME);
        this.swiftAPIClient.createContainer(properties.getProperty(SapsPropertiesConstants.SWIFT_CONTAINER_NAME));
        this.swiftKey = properties.getProperty(SapsPropertiesConstants.SWIFT_OBJECT_STORE_KEY);
        this.debugMode = properties.containsKey(SapsPropertiesConstants.SAPS_DEBUG_MODE) && properties
                .getProperty(SapsPropertiesConstants.SAPS_DEBUG_MODE).toLowerCase().equals("true");

        if (this.debugMode && !checkPropertiesDebugMode(properties))
            throw new PermanentStorageException("Error on validate the file. Missing properties for start Saps Controller.");
    }

    public SwiftPermanentStorage(Properties properties) throws PermanentStorageException {
        this(properties, new SwiftAPIClient(properties));
    }

    private boolean checkProperties(Properties properties) {
        String[] propertiesSet = {
            SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR,
            SapsPropertiesConstants.SWIFT_CONTAINER_NAME,
            SapsPropertiesConstants.SWIFT_OBJECT_STORE_KEY
        };

        return SapsPropertiesUtil.checkProperties(properties, propertiesSet);
    }

    /**
     * This function checks if properties for debug mode have been set.
     *
     * @param properties saps properties to be check
     * @return boolean representation, true (case all properties been set) or false
     * (otherwise)
     */
    private boolean checkPropertiesDebugMode(Properties properties) {
        if (!properties.containsKey(SapsPropertiesConstants.PERMANENT_STORAGE_DEBUG_TASKS_DIR)) {
            LOGGER.error("Required property " + SapsPropertiesConstants.PERMANENT_STORAGE_DEBUG_TASKS_DIR
                    + " was not set (it's necessary when debug mode)");
            return false;
        }

        LOGGER.debug("All properties for debug mode are set");
        return true;
    }

    /**
     * This function tries to archive a task trying each folder in order
     * (inputdownloading -> preprocessing -> processing).
     *
     * @param task task to be archived
     * @return boolean representation, success (true) or failure (false) in to
     * archive the three folders.
     */
    @Override
    public boolean archive(SapsImage task) {
        String taskId = task.getTaskId();

        LOGGER.info("Attempting to archive task [" + taskId + "] with a maximum of " + MAX_ARCHIVE_TRIES
                + " archiving attempts for each folder (inputdownloading, preprocessing, processing)");

        String swiftExports = (task.getState() == ImageTaskState.FAILED && this.debugMode)
                ? properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_DEBUG_TASKS_DIR)
                : properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR);

        String inputdownloadingLocalDir = String.format(SAPS_TASK_STAGE_DIR_PATTERN, nfsTempStoragePath, taskId, INPUTDOWNLOADING_DIR);
        String inputdownloadingSwiftDir = String.format(SWIFT_TASK_STAGE_DIR_PATTERN, swiftExports, taskId, INPUTDOWNLOADING_DIR);

        String preprocessingLocalDir = String.format(SAPS_TASK_STAGE_DIR_PATTERN, nfsTempStoragePath, taskId, PREPROCESSING_DIR);
        String preprocessingSwiftDir = String.format(SWIFT_TASK_STAGE_DIR_PATTERN, swiftExports, taskId, PREPROCESSING_DIR);

        String processingLocalDir = String.format(SAPS_TASK_STAGE_DIR_PATTERN, nfsTempStoragePath, taskId, PROCESSING_DIR);
        String processingSwiftDir = String.format(SWIFT_TASK_STAGE_DIR_PATTERN, swiftExports, taskId, PROCESSING_DIR);

        boolean inputdownloadingSentSuccess = archive(taskId, inputdownloadingLocalDir, inputdownloadingSwiftDir);
        boolean preprocessingSentSuccess = inputdownloadingSentSuccess && archive(taskId, preprocessingLocalDir, preprocessingSwiftDir);
        boolean processingSentSuccess = preprocessingSentSuccess && archive(taskId, processingLocalDir, processingSwiftDir);

        LOGGER.info("Archive process result of task [" + task.getTaskId() + ":\nInputdownloading phase: "
                + (inputdownloadingSentSuccess ? "Success" : "Failure") + "\n" + "Preprocessing phase: "
                + (preprocessingSentSuccess ? "Success" : "Failure") + "\n" + "Processing phase: "
                + (processingSentSuccess ? "Success" : "Failure"));

        return inputdownloadingSentSuccess && preprocessingSentSuccess && processingSentSuccess;
    }

    /**
     * This function tries to archive a task folder in Swift.
     *
     * @param taskId     task id in archive process
     * @param localDir task folder to be archived
     * @param swiftDir directory swift to archive new data
     * @return boolean representation, success (true) or failure (false) to archive
     */
    private boolean archive(String taskId, String localDir, String swiftDir) {
        LOGGER.info("Trying to archive task [" + taskId + "] " + localDir + " folder to " + swiftDir +
                " folder with a maximum of " + MAX_ARCHIVE_TRIES + " archiving attempts");

        File localFileDir = new File(localDir);

        if (!localFileDir.exists() || !localFileDir.isDirectory()) {
            LOGGER.error("Failed to archive task [" + taskId + "]. " + localDir
                    + " folder isn't directory or not exists");
            return false;
        }

        for (int itry = 0; itry < MAX_ARCHIVE_TRIES; itry++) {
            LOGGER.info("Trying to archive task [" + taskId + "] " + localDir + " folder for swift");
            if (uploadFiles(localFileDir, swiftDir))
                return true;
        }

        return false;
    }

    /**
     * This function tries upload task folder files to Swift.
     *
     * @param localDir task folder to be archived
     * @param swiftDir directory swift to archive new data
     * @return boolean representation, success (true) or failure (false) to archive
     */
    private boolean uploadFiles(File localDir, String swiftDir) {
        for (File actualFile : localDir.listFiles()) {
            if (!uploadFile(actualFile, swiftDir)) {
                LOGGER.info("Failure in archiving file [" + actualFile.getAbsolutePath() + "]");
                //TODO What should really be done when one or more files fail to upload?
                return false;
            }
        }

        LOGGER.info("Upload to swift successfully done");
        return true;
    }

    /**
     * This function tries upload a task folder file to Swift.
     *
     * @param actualFile file to be uploaded
     * @param swiftDir   directory swift to archive
     * @return boolean representation, success (true) or failure (false) to archive
     */
    private boolean uploadFile(File actualFile, String swiftDir) {
        LOGGER.info("Trying to archive file [" + actualFile.getAbsolutePath() + "] for swift container ["
                + containerName + "] with a maximum of " + MAX_SWIFT_UPLOAD_TRIES + " uploading attempts");

        for (int itry = 0; itry < MAX_SWIFT_UPLOAD_TRIES; itry++) {
            try {
                swiftAPIClient.uploadFile(containerName, actualFile, swiftDir);
                return true;
            } catch (Exception e) {
                LOGGER.error("Error while uploading file " + actualFile.getAbsolutePath() + " to swift", e);
            }
        }

        return false;
    }

    /**
     * This function delete all files from task in Permanent Storage.
     *
     * @param task task with files information to be deleted
     * @return boolean representation, success (true) or failure (false) to delete
     * files
     */
    @Override
    public boolean delete(SapsImage task) {
        String taskId = task.getTaskId();

        LOGGER.debug("Deleting files from task [" + taskId + "] in Swift [" + containerName + "]");
        String swiftExports = (task.getState() == ImageTaskState.FAILED && this.debugMode)
                ? properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_DEBUG_TASKS_DIR)
                : properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR);

        String prefix = swiftExports + File.separator + taskId;

        List<String> fileNames = swiftAPIClient.listFilesWithPrefix(containerName, prefix);

        LOGGER.info("Files List: " + fileNames);

        for (String file : fileNames) {
            LOGGER.debug("Trying to delete file " + file + " from " + containerName);
            swiftAPIClient.deleteFile(containerName, file);
        }

        return true;
    }

    @Override
    public List<AccessLink> generateAccessLinks(String taskId) {
        List<AccessLink> filesLinks = new ArrayList<>();
        List<String> files = new ArrayList<>();

        String tasksDir = properties.getProperty(SapsPropertiesConstants.PERMANENT_STORAGE_TASKS_DIR);
        String[] dirs = {INPUTDOWNLOADING_DIR, PREPROCESSING_DIR, PROCESSING_DIR};

        for (String dir : dirs) {
            String dirPath = String.format(SWIFT_TASK_STAGE_DIR_PATTERN, tasksDir, taskId, dir);
            try {
                files.addAll(this.swiftAPIClient.listFiles(this.containerName, dirPath));
            } catch (IOException e) {
                LOGGER.error("Error while list files of path [" + dir + "] from Object Storage", e);
            }
        }

        for(String filePath : files) {
            String link;
            try {
                link = generateTempURL(filePath);
                Path p = Paths.get(filePath);
                String name = p.getParent().getFileName().toString() + "/" + p.getFileName().toString();
                filesLinks.add(new AccessLink(name, link));
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                LOGGER.error("Error while generate temp url to File Path [" + filePath + "]", e);
            }

        }
        return filesLinks;
    }

    private String generateTempURL(String filePath)
        throws NoSuchAlgorithmException, InvalidKeyException {

        Formatter objectStoreFormatter = new Formatter();
        objectStoreFormatter.format("%s\n%s\n%s", "GET", Long.MAX_VALUE, filePath);
        String signature = calculateRFC2104HMAC(objectStoreFormatter.toString(), this.swiftKey);
        objectStoreFormatter.close();

        objectStoreFormatter = new Formatter();
        objectStoreFormatter.format(TEMP_DIR_URL, filePath, signature, Long.MAX_VALUE);
        String res = objectStoreFormatter.toString();
        objectStoreFormatter.close();

        return res;
    }

    private String calculateRFC2104HMAC(String data, String key)
        throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return toHexString(mac.doFinal(data.getBytes()));
    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        String hexString = formatter.toString();
        formatter.close();

        return hexString;
    }

}
