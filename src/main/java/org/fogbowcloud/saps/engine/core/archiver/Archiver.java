package org.fogbowcloud.saps.engine.core.archiver;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.exceptions.ArchiverException;
import org.fogbowcloud.saps.engine.core.archiver.storage.NfsPermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.swift.SwiftPermanentStorage;
import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.catalog.jdbc.JDBCCatalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesUtil;
import org.fogbowcloud.saps.engine.utils.retry.CatalogUtils;

public class Archiver {

    private final Properties properties;
    private final Catalog catalog;
    private final PermanentStorage permanentStorage;
    private final ScheduledExecutorService sapsExecutor;
    private final boolean executionDebugMode;

    public static final Logger LOGGER = Logger.getLogger(Archiver.class);

    public Archiver(Properties properties) throws SapsException {
        this(properties, new JDBCCatalog(properties));
    }

    protected Archiver(Properties properties, Catalog catalog) throws SapsException {
        if (!checkProperties(properties))
            throw new SapsException("Error on validate the file. Missing properties for start Saps Controller.");

        this.properties = properties;
        this.catalog = catalog;
        this.sapsExecutor = Executors.newScheduledThreadPool(1);
        this.permanentStorage = createStorageInstance(properties.getProperty(SapsPropertiesConstants.SAPS_PERMANENT_STORAGE_TYPE));
        this.executionDebugMode = properties.containsKey(SapsPropertiesConstants.SAPS_EXECUTION_DEBUG_MODE) && properties
                .getProperty(SapsPropertiesConstants.SAPS_EXECUTION_DEBUG_MODE).toLowerCase().equals("true");
    }

    /**
     * It creates an instance from declared {@code PermanentStorage} type.
     *
     * @param type permanent storage type to be created
     * @return {@code PermanentStorage} instance
     * @throws SapsException
     */
    private PermanentStorage createStorageInstance(String type) throws SapsException {
        String lType = type.toLowerCase();
        if (lType.equals("swift"))
            return new SwiftPermanentStorage(properties);
        if (lType.equals("nfs"))
            return new NfsPermanentStorage();

        throw new SapsException("Failed to recognize type of permanent storage");
    }

    private boolean checkProperties(Properties properties) {
		String[] propertiesSet = {
				SapsPropertiesConstants.IMAGE_DATASTORE_IP,
				SapsPropertiesConstants.IMAGE_DATASTORE_PORT,
				SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_GARBAGE_COLLECTOR,
				SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_ARCHIVER,
				SapsPropertiesConstants.SAPS_EXPORT_PATH,
				SapsPropertiesConstants.SAPS_PERMANENT_STORAGE_TYPE
		};

		return SapsPropertiesUtil.checkProperties(properties, propertiesSet);
    }

    public void start() throws ArchiverException {
        cleanUnfinishedArchivedData();

        sapsExecutor.scheduleWithFixedDelay(() -> garbageCollector(), 0, Long.valueOf(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_GARBAGE_COLLECTOR)),
                TimeUnit.SECONDS);

        sapsExecutor.scheduleWithFixedDelay(() -> archiver(), 0, Long.valueOf(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_ARCHIVER)),
                TimeUnit.SECONDS);
    }

    /**
     * Its an garbage collector deleting data directory from {@code ImageTaskState.FAILED} tasks.
     */
    private void garbageCollector() {
        List<SapsImage> failedTasks = tasksInFailedState();

        LOGGER.info("Deleting data directory from " + failedTasks.size() + " failed tasks");

        for (SapsImage task : failedTasks)
            deleteAllTaskFilesFromDisk(task);
    }

    /**
     * It gets tasks in failed state in {@code Catalog}.
     *
     * @return {@code SapsImage} list in {@code ImageTaskState.FAILED} state
     */
    private List<SapsImage> tasksInFailedState() {
        return CatalogUtils.getTasks(catalog, ImageTaskState.FAILED,
                "gets tasks with " + ImageTaskState.FAILED.getValue() + " state");
    }

    /**
     * It cleans unfinished data from incomplete {@code ImageTaskState.ARCHIVING}.
     *
     * @throws ArchiverException
     */
    private void cleanUnfinishedArchivedData() throws ArchiverException {
        List<SapsImage> archivingTasks = tasksInArchivingState();

        LOGGER.info("Rollback in " + archivingTasks.size() + " tasks in archiving state");

        for (SapsImage task : archivingTasks) {
            LOGGER.info("Applying task [" + task.getTaskId() + "] rollback");
            rollBackArchive(task);
            permanentStorage.delete(task);
        }
    }

    /**
     * It gets tasks in archiving state in {@code Catalog}.
     *
     * @return {@code SapsImage} list in {@code ImageTaskState.ARCHIVING} state
     */
    private List<SapsImage> tasksInArchivingState() {
        return CatalogUtils.getTasks(catalog, ImageTaskState.ARCHIVING,
                "gets tasks with " + ImageTaskState.ARCHIVING.getValue() + " state");
    }

    /**
     * It applies rollback in specific {@code SapsImage}, returning for {@code ImageTaskState.FINISHED} state.
     *
     * @param task {@code SapsImage} to be rollbacked
     */
    private void rollBackArchive(SapsImage task) {
        String taskId = task.getTaskId();
        LOGGER.info("Reversing the archiving done in task [" + taskId + "] and returning to state ["
                + ImageTaskState.FINISHED.getValue() + "]");

        removeTimestampTaskInCatalog(task, "removes task [" + taskId + "] timestamp");

        updateStateInCatalog(task, ImageTaskState.FINISHED, SapsImage.AVAILABLE,
                "Max archive tries reached", SapsImage.NONE_ARREBOL_JOB_ID,
                "updates task [" + taskId + "] to failed state");

        addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
    }

    /**
     * It archives {@code ImageTaskState.FINISHED} {@code SapsImage} in {@code PermanentStorage}.
     */
    private void archiver() {
        List<SapsImage> tasksToArchive = tasksToArchive();

        LOGGER.info("Trying to archive " + tasksToArchive.size() + " finished tasks: " + tasksToArchive);

        for (SapsImage task : tasksToArchive) {
            LOGGER.info("Try to archive task [" + task.getTaskId() + "]");
            tryTaskArchive(task);
            deleteAllTaskFilesFromDisk(task);
        }
    }

    /**
     * It gets {@code SapsImage} list in {@code ImageTaskState.FINISHED} state in {@code Catalog}.
     *
     * @return {@code SapsImage} list in {@code ImageTaskState.FINISHED} state
     */
    private List<SapsImage> tasksToArchive() {
        return CatalogUtils.getTasks(catalog, ImageTaskState.FINISHED,
                "gets tasks with " + ImageTaskState.FINISHED.getValue() + " state");
    }

    /**
     * It try to archive a {@code SapsImage} in {@code PermanentStorage}.
     *
     * @param task {@code SapsImage} to be archived
     */
    private void tryTaskArchive(SapsImage task) {
        if (prepareArchive(task) && permanentStorage.archive(task)) {
            LOGGER.info("SUCCESS in archiving task [" + task.getTaskId() + "]");
            finishArchive(task);
        } else {
            LOGGER.info("FAILURE in archiving task [" + task.getTaskId() + "]");
            failedArchive(task);
        }
    }

    /**
     * It prepares a {@code SapsImage} for archive.
     *
     * @param task {@code SapsImage} to be prepared for archive
     * @return success (true) or failure (false) in preparing the {@code SapsImage}.
     */
    private boolean prepareArchive(SapsImage task) {
        LOGGER.info("Preparing task [" + task.getTaskId() + "] to archive");

        String taskId = task.getTaskId();

        updateStateInCatalog(task, ImageTaskState.ARCHIVING, SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA,
                SapsImage.NONE_ARREBOL_JOB_ID, "updates task [" + taskId + "] with state [" + ImageTaskState.ARCHIVING.getValue() + "]");

        addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");

        LOGGER.info("Task [" + taskId + "] ready to archive");
        return true;
    }

    /**
     * It finishes a success {@code SapsImage}.
     *
     * @param task {@code SapsImage} to be finished
     */
    private void finishArchive(SapsImage task) {
        LOGGER.debug("Finishing archive for task [" + task + "]");

        String taskId = task.getTaskId();
        updateStateInCatalog(task, ImageTaskState.ARCHIVED, SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA,
                SapsImage.NONE_ARREBOL_JOB_ID, "updates task [" + taskId + "] with state [" + ImageTaskState.ARCHIVED.getValue() + "]");
        addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
    }

    /**
     * It finishes a {@code SapsImage} in {@code ImageTaskState.FAILED} state.
     *
     * @param task {@code SapsImage} to be finished
     */
    private void failedArchive(SapsImage task) {
        String taskId = task.getTaskId();

        updateStateInCatalog(task, ImageTaskState.FAILED, SapsImage.AVAILABLE,
                "Max archive tries reached", SapsImage.NONE_ARREBOL_JOB_ID,
                "updates task [" + taskId + "] to failed state");
        addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
    }

    /**
     * It deletes directory from {@code SapsImage}.
     *
     * @param task {@code SapsImage} that contains information to delete your folder
     */
    private void deleteAllTaskFilesFromDisk(SapsImage task) {
        LOGGER.info("Deleting all task [" + task.getTaskId() + "] files from disk");

        String sapsExports = properties.getProperty(SapsPropertiesConstants.SAPS_EXPORT_PATH);
        String taskDirPath = sapsExports + File.separator + task.getTaskId();

        File taskDir = new File(taskDirPath);
        if (taskDir.exists() && taskDir.isDirectory()) {
            if (this.executionDebugMode && task.getState().equals(ImageTaskState.FAILED))
                permanentStorage.archive(task);
            try {
                FileUtils.deleteDirectory(taskDir);
            } catch (IOException e) {
                LOGGER.error("Error while delete all task files from disk: ", e);
            }
        } else
            LOGGER.info("Path " + taskDirPath + " does not exist or is not a directory!");
    }

    /**
     * It updates {@code SapsImage} state in {@code Catalog}.
     *
     * @param task         task to be updated
     * @param state        new task state
     * @param status       new task status
     * @param error        new error message
     * @param arrebolJobId new Arrebol job id
     * @param message      information message
     * @return boolean representation reporting success (true) or failure (false) in update {@code SapsImage} state
     * in {@code Cataloh}
     */
    private boolean updateStateInCatalog(SapsImage task, ImageTaskState state, String status, String error,
                                         String arrebolJobId, String message) {
        task.setState(state);
        task.setStatus(status);
        task.setError(error);
        task.setArrebolJobId(arrebolJobId);
        return CatalogUtils.updateState(catalog, task,
                "updates task[" + task.getTaskId() + "] state for " + state.getValue());
    }

    /**
     * It adds new tuple in timestamp table and updates {@code SapsImage} timestamp.
     *
     * @param task    task to be update
     * @param message information message
     */
    private void addTimestampTaskInCatalog(SapsImage task, String message) {
        CatalogUtils.addTimestampTask(catalog, task, message);
    }

    /**
     * It removes {@code SapsImage} timestamp.
     *
     * @param task    task to be remove
     * @param message information message
     */
    private void removeTimestampTaskInCatalog(SapsImage task, String message) {
        CatalogUtils.removeTimestampTask(catalog, task, message);
    }
}
