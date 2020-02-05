package org.fogbowcloud.saps.engine.core.catalog;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.fogbowcloud.saps.engine.core.catalog.exceptions.CatalogException;
import org.fogbowcloud.saps.engine.core.catalog.exceptions.TaskNotFoundException;
import org.fogbowcloud.saps.engine.core.catalog.exceptions.UserNotFoundException;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;

public interface Catalog {

    SapsImage addTask(String taskId, String dataset, String region, Date date, int priority, String user,
                      String inputdownloadingPhaseTag, String digestInputdownloading, String preprocessingPhaseTag,
                      String digestPreprocessing, String processingPhaseTag, String digestProcessing) throws CatalogException;

    void addStateChangeTime(String taskId, ImageTaskState state, Timestamp timestamp) throws SQLException;

    void addUser(String userEmail, String userName, String userPass, boolean userState, boolean userNotify,
                 boolean adminRole) throws SQLException;

    void updateImageTask(SapsImage imageTask) throws SQLException;

    List<SapsImage> getAllTasks() throws SQLException;

    List<SapsImage> getTasksByState(int limit, ImageTaskState... tasksStates) throws CatalogException;

    SapsImage getTaskById(String taskId) throws CatalogException, TaskNotFoundException;

    SapsUser getUserByEmail(String userEmail) throws CatalogException, UserNotFoundException;

    void removeStateChangeTime(String taskId, ImageTaskState state, Timestamp timestamp) throws SQLException;

    List<SapsImage> getTasksByFilters(ImageTaskState state, String region, Date initDate, Date endDate, String inputGathering,
                                                  String inputPreprocessing, String algorithmExecution) throws CatalogException;
}
