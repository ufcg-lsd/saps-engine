package org.fogbowcloud.saps.engine.core.archiver.storage;

import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.PermanentStorageException;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

import java.util.List;

public interface PermanentStorage {

	/**
	 * This function tries to archive a task trying each dirs in order
	 * (inputdownloading -> preprocessing -> processing).
	 * 
	 * @param task task to be archived
	 * @return boolean representation, success (true) or failure (false) in to
	 *         archive the three dirs.
	 */
	boolean archive(SapsImage task) throws PermanentStorageException;

	/**
	 * This function delete all files from task in Permanent Storage.
	 * 
	 * @param task task with files information to be deleted
	 * @return boolean representation, success (true) or failure (false) to delete
	 *         files
	 * @throws Exception
	 */
	boolean delete(SapsImage task) throws PermanentStorageException;

	List<String> generateLink(SapsImage task) throws PermanentStorageException;
}
