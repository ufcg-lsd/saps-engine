package org.fogbowcloud.saps.engine.core.archiver.storage.swift;

import org.fogbowcloud.saps.engine.core.archiver.storage.exceptions.PermanentStorageException;

// FIXME Replace this exception with other, more specific exceptions
public class SwiftPermanentStorageException extends PermanentStorageException {

    private static final long serialVersionUID = -2520888793776997437L;

    public SwiftPermanentStorageException(String msg) {
        super(msg);
    }
}
