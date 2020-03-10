package org.fogbowcloud.saps.engine.core.archiver.storage.exceptions;

public class TaskNotFoundException extends Exception {

    public TaskNotFoundException(String msg) {
        super(msg);
    }

    public TaskNotFoundException(String msg, Throwable t) {
        super(msg, t);
    }
}