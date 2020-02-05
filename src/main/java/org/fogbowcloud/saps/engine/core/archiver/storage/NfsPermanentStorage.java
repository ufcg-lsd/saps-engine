package org.fogbowcloud.saps.engine.core.archiver.storage;

import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.PermanentStorageException;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

public class NfsPermanentStorage implements PermanentStorage {

	@Override
	public boolean archive(SapsImage task) throws PermanentStorageException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delete(SapsImage task) throws PermanentStorageException {
		// TODO Auto-generated method stub
		return false;
	}

}
