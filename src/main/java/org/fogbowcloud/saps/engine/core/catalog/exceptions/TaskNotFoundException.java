package org.fogbowcloud.saps.engine.core.catalog.exceptions;

import java.sql.SQLException;

public class TaskNotFoundException extends RuntimeException{

    private static final long serialVersionUID = -2520888793776997437L;

    public TaskNotFoundException(String msg){
        super(msg);
    }

    public TaskNotFoundException(String msg, Exception e){
        super(msg, e);
    }
}