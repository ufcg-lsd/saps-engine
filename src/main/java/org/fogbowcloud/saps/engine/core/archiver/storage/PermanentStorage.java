package org.fogbowcloud.saps.engine.core.archiver.storage;

import java.io.IOException;
import java.util.List;
import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.TaskNotFoundException;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

public interface PermanentStorage {

	/**
	 * This function tries to archive a task trying each dirs in order
	 * (inputdownloading -> preprocessing -> processing).
	 *
	 * @param task task to be archived
	 * @return boolean representation, success (true) or failure (false) in to
	 *         archive the three dirs.
	 */
	boolean archive(SapsImage task) throws IOException;

	/**
	 * This function delete all files from task in Permanent Storage.
	 *
	 * @param task task with files information to be deleted
	 * @return boolean representation, success (true) or failure (false) to delete
	 *         files
	 * @throws Exception
	 */
	boolean delete(SapsImage task) throws IOException;

	/**
	 *
	 * @param taskId The Task's unique identifier
	 * @return Empty list if the task not contains files
	 * @throws IOException If a request error occurs with a service or system
	 * @throws TaskNotFoundException If task was not found
	 */
	List<AccessLink> generateAccessLinks(String taskId) throws TaskNotFoundException, IOException;

}
