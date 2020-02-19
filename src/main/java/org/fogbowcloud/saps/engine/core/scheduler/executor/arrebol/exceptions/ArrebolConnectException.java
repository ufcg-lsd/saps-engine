package org.fogbowcloud.saps.engine.core.scheduler.executor.arrebol.exceptions;

import java.net.ConnectException;

public class ArrebolConnectException extends RuntimeException {

    public ArrebolConnectException(String s, ConnectException e) {
        super(s, e);
    }
}
