package org.fogbowcloud.saps.engine.core.archiver.storage.exceptions;

public class InvalidPropertyException extends Exception {

    public InvalidPropertyException(String message) {
        super(message);
    }

    public InvalidPropertyException(String message, Throwable cause) {
        super(message, cause);
    }
}
