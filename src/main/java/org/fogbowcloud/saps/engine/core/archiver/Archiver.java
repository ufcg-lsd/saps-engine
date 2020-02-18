package org.fogbowcloud.saps.engine.core.archiver;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.exceptions.ArchiverException;
import org.fogbowcloud.saps.engine.core.archiver.storage.PermanentStorage;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.PermanentStorageException;
import org.fogbowcloud.saps.engine.core.catalog.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesUtil;
import org.fogbowcloud.saps.engine.utils.retry.CatalogUtils;

public class Archiver {

    private final Catalog catalog;
    private final PermanentStorage permanentStorage;
    private final ScheduledExecutorService sapsExecutor;
    private final boolean executionDebugMode;
    private final String tempStoragePath;
    private final long gcDelayPeriod;
    private final long archiverDelayPeriod;

    private static final Logger LOGGER = Logger.getLogger(Archiver.class);

    public Archiver(Properties properties, Catalog catalog, PermanentStorage permanentStorage, ScheduledExecutorService executor)
        throws ArchiverException {
        if (!checkProperties(properties))
            //FIXME Change exception to WrongConfigurationException and move it to inside check properties
            throw new ArchiverException("Error on validate the file. Missing properties for start Saps Controller.");
        this.catalog = catalog;
        this.permanentStorage = permanentStorage;
        this.sapsExecutor = executor;
        this.tempStoragePath = properties.getProperty(SapsPropertiesConstants.SAPS_TEMP_STORAGE_PATH);
        this.gcDelayPeriod = Long.valueOf(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_GARBAGE_COLLECTOR));
        this.archiverDelayPeriod = Long.valueOf(properties.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_ARCHIVER));
        this.executionDebugMode = properties.containsKey(SapsPropertiesConstants.SAPS_DEBUG_MODE) && properties
            .getProperty(SapsPropertiesConstants.SAPS_DEBUG_MODE).toLowerCase().equals("true");
    }

    private boolean checkProperties(Properties properties) {
		String[] propertiesSet = {
				SapsPropertiesConstants.IMAGE_DATASTORE_IP,
				SapsPropertiesConstants.IMAGE_DATASTORE_PORT,
				SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_GARBAGE_COLLECTOR,
				SapsPropertiesConstants.SAPS_EXECUTION_PERIOD_ARCHIVER,
				SapsPropertiesConstants.SAPS_TEMP_STORAGE_PATH,
				SapsPropertiesConstants.SAPS_PERMANENT_STORAGE_TYPE
		};

		return SapsPropertiesUtil.checkProperties(properties, propertiesSet);
    }

    public void start() throws ArchiverException {
        resetUnfinishedTasks();

        sapsExecutor.scheduleWithFixedDelay(() -> runGarbageCollector(), 0, gcDelayPeriod,
                TimeUnit.SECONDS);

        sapsExecutor.scheduleWithFixedDelay(() -> runArchiver(), 0, archiverDelayPeriod,
                TimeUnit.SECONDS);
    }

    /**
     * Its an garbage collector deleting data directory from {@code ImageTaskState.FAILED} tasks.
     */
    private void runGarbageCollector() {
        List<SapsImage> failedTasks = getFailedTasks();

        LOGGER.info("Deleting data directory from " + failedTasks.size() + " failed tasks");

        for (SapsImage task : failedTasks)
            deleteTaskTempData(task);
    }

    /**
     * It gets tasks in failed state in {@code Catalog}.
     *
     * @return {@code SapsImage} list in {@code ImageTaskState.FAILED} state
     */
    private List<SapsImage> getFailedTasks() {
        return CatalogUtils.getTasks(catalog, ImageTaskState.FAILED,
                "gets tasks with " + ImageTaskState.FAILED.getValue() + " state");
    }

    /**
     * It cleans unfinished data from incomplete {@code ImageTaskState.ARCHIVING}.
     *
     * @throws ArchiverException
     */
    private void resetUnfinishedTasks() throws ArchiverException {
        List<SapsImage> archivingTasks = getArchivingTasks();

        LOGGER.info("Rollback in " + archivingTasks.size() + " tasks in archiving state");

        for (SapsImage task : archivingTasks) {
            LOGGER.info("Applying task [" + task.getTaskId() + "] rollback");
            rollBackArchive(task);
            try {
                permanentStorage.delete(task);
            } catch (PermanentStorageException e) {
                LOGGER.error("Error while delete task [" + task.getTaskId() + "] from Permanent Storage");
            }
        }
    }

    /**
     * It gets tasks in archiving state in {@code Catalog}.
     *
     * @return {@code SapsImage} list in {@code ImageTaskState.ARCHIVING} state
     */
    private List<SapsImage> getArchivingTasks() {
        return CatalogUtils.getTasks(catalog, ImageTaskState.ARCHIVING,
                "gets tasks with " + ImageTaskState.ARCHIVING.getValue() + " state");
    }

    /**
     * It applies rollback in specific {@code SapsImage}, returning for {@code ImageTaskState.FINISHED} state.
     *
     * @param task {@code SapsImage} to be rollbacked
     */
    //TODO Update method doc
    private void rollBackArchive(SapsImage task) {
        String taskId = task.getTaskId();
        LOGGER.info("Reversing the archiving done in task [" + taskId + "] and returning to state ["
                + ImageTaskState.FINISHED.getValue() + "]");

        removeLastStateChangeTimeFromTask(task, "removes task [" + taskId + "] timestamp");

        updateTaskState(task, ImageTaskState.FINISHED, SapsImage.AVAILABLE,
                "Max archive tries reached", SapsImage.NONE_ARREBOL_JOB_ID);
        addTaskStateChangeTime(task, "updates task [" + taskId + "] timestamp");
    }

    /**
     * It archives {@code ImageTaskState.FINISHED} {@code SapsImage} in {@code PermanentStorage}.
     */
    private void runArchiver() {
        List<SapsImage> tasksToArchive = getFinishedTasks();

        LOGGER.info("Trying to archive " + tasksToArchive.size() + " finished tasks");

        for (SapsImage task : tasksToArchive) {
            LOGGER.info("Try to archive task [" + task.getTaskId() + "]");
            try {
                archiveTask(task);
            } catch (PermanentStorageException e) {
                LOGGER.info("FAILURE in archiving task [" + task.getTaskId() + "]");
                setTaskToFailedState(task);
            }
            deleteTaskTempData(task);
        }
    }

    /**
     * It gets {@code SapsImage} list in {@code ImageTaskState.FINISHED} state in {@code Catalog}.
     *
     * @return {@code SapsImage} list in {@code ImageTaskState.FINISHED} state
     */
    private List<SapsImage> getFinishedTasks() {
        return CatalogUtils.getTasks(catalog, ImageTaskState.FINISHED,
                "gets tasks with " + ImageTaskState.FINISHED.getValue() + " state");
    }

    /**
     * It try to archive a {@code SapsImage} in {@code PermanentStorage}.
     *
     * @param task {@code SapsImage} to be archived
     */
    private void archiveTask(SapsImage task) throws PermanentStorageException {
        LOGGER.info("Preparing task [" + task.getTaskId() + "] to archive");
        updateTaskState(task, ImageTaskState.ARCHIVING, SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA,
            SapsImage.NONE_ARREBOL_JOB_ID);
        addTaskStateChangeTime(task, "updates task [" + task.getTaskId() + "] timestamp");

        permanentStorage.archive(task);

        LOGGER.info("SUCCESS in archiving task [" + task.getTaskId() + "]");
        updateTaskState(task, ImageTaskState.ARCHIVED, SapsImage.AVAILABLE, SapsImage.NON_EXISTENT_DATA,
            SapsImage.NONE_ARREBOL_JOB_ID);
        addTaskStateChangeTime(task, "updates task [" + task.getTaskId() + "] timestamp");
    }

    /**
     * It finishes a {@code SapsImage} in {@code ImageTaskState.FAILED} state.
     *
     * @param task {@code SapsImage} to be finished
     */
    private void setTaskToFailedState(SapsImage task) {
        String taskId = task.getTaskId();

        updateTaskState(task, ImageTaskState.FAILED, SapsImage.AVAILABLE,
                "Max archive tries reached", SapsImage.NONE_ARREBOL_JOB_ID);
        addTaskStateChangeTime(task, "updates task [" + taskId + "] timestamp");
    }

    /**
     * It deletes directory from {@code SapsImage}.
     *
     * @param task {@code SapsImage} that contains information to delete your folder
     */
    //FIXME Rename to deleteTempData
    private void deleteTaskTempData(SapsImage task) {
        LOGGER.info("Deleting all task [" + task.getTaskId() + "] files from disk");
        String taskDirPath = tempStoragePath + File.separator + task.getTaskId();

        File taskDir = new File(taskDirPath);
        if (taskDir.exists() && taskDir.isDirectory()) {
            try {
                //TODO Remove archive task from here
                if (this.executionDebugMode && task.getState().equals(ImageTaskState.FAILED))
                    permanentStorage.archive(task);
                FileUtils.deleteDirectory(taskDir);
            } catch (IOException e) {
                LOGGER.error("Error while delete task [" + task.getTaskId() +"] files from disk: ", e);
            } catch (PermanentStorageException e) {
                LOGGER.error("Error while archive task [" + task.getTaskId() + "] to debug permanent storage dir");
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
     * @return boolean representation reporting success (true) or failure (false) in update {@code SapsImage} state
     * in {@code Cataloh}
     */
    private boolean updateTaskState(SapsImage task, ImageTaskState state, String status, String error,
                                         String arrebolJobId) {
        task.setState(state);
        task.setStatus(status);
        task.setError(error);
        task.setArrebolJobId(arrebolJobId);
        return CatalogUtils.updateState(catalog, task,
                "updates task [" + task.getTaskId() + "] state for " + state.getValue());
    }

    /**
     * It adds new tuple in timestamp table and updates {@code SapsImage} timestamp.
     *
     * @param task    task to be update
     * @param message information message
     */
    private void addTaskStateChangeTime(SapsImage task, String message) {
        CatalogUtils.addTimestampTask(catalog, task, message);
    }

    /**
     * It removes {@code SapsImage} timestamp.
     *
     * @param task    task to be remove
     * @param message information message
     */
    //TODO Review it's param and name
    private void removeLastStateChangeTimeFromTask(SapsImage task, String message) {
        CatalogUtils.removeTimestampTask(catalog, task, message);
    }
}
