package org.fogbowcloud.saps.engine.core.catalog;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.fogbowcloud.saps.engine.core.catalog.exceptions.CatalogException;
import org.fogbowcloud.saps.engine.core.catalog.exceptions.TaskNotFoundException;
import org.fogbowcloud.saps.engine.core.catalog.exceptions.UserNotFoundException;
import org.fogbowcloud.saps.engine.core.dispatcher.notifier.Ward;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;

public interface Catalog {

    SapsImage addTask(String taskId, String dataset, String region, Date date, int priority, String user,
                      String inputdownloadingPhaseTag, String digestInputdownloading, String preprocessingPhaseTag,
                      String digestPreprocessing, String processingPhaseTag, String digestProcessing) throws CatalogException;

    void addStateChangeTime(String taskId, ImageTaskState state, Timestamp timestamp) throws CatalogException;

    void addUser(String userEmail, String userName, String userPass, boolean userState, boolean userNotify,
                 boolean adminRole) throws CatalogException;

    void addUserNotification(String submissionId, String taskId, String userEmail) throws CatalogException;

    List<Ward> getUsersToNotify() throws CatalogException;

    void updateImageTask(SapsImage imageTask) throws CatalogException;

    boolean isUserNotifiable(String userEmail) throws CatalogException;

    List<SapsImage> getAllTasks() throws CatalogException;

    List<SapsImage> getTasksInProcessingStates() throws CatalogException;

    List<SapsImage> getTasksByState(ImageTaskState state, int limit) throws CatalogException;

    SapsImage getTaskById(String taskId) throws CatalogException, TaskNotFoundException;

    SapsUser getUserByEmail(String userEmail) throws CatalogException, UserNotFoundException;

    void removeNotification(String submissionId, String taskId, String userEmail) throws CatalogException;

    void removeStateChangeTime(String taskId, ImageTaskState state, Timestamp timestamp) throws CatalogException;

    List<SapsImage> getSuccessfullyProcessedTasks(String region, Date initDate, Date endDate, String inputGathering,
                                                  String inputPreprocessing, String algorithmExecution) throws CatalogException;
}
