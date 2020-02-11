package org.fogbowcloud.saps.engine.core.catalog.exceptions;

public class UserNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -2520888793776997437L;

    public UserNotFoundException(String msg) {
        super(msg);
    }
}
