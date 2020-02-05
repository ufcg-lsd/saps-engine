package org.fogbowcloud.saps.engine.core.archiver.storage.exceptions;

public class PermanentStorageException extends RuntimeException {

    private static final long serialVersionUID = -2520888793776997437L;

    public PermanentStorageException(String msg){
        super(msg);
    }

    public PermanentStorageException(String msg, Throwable t){
        super(msg, t);
    }
}
