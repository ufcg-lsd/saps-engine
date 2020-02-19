package org.fogbowcloud.saps.engine.core.archiver.exceptions;

//FIXME Replace this exception with other, more specific exceptions, or replace to RuntimeException
public class ArchiverException extends RuntimeException {

    private static final long serialVersionUID = -2520888793776997437L;

    public ArchiverException(String msg) {
        super(msg);
    }
}