package org.fogbowcloud.saps.engine.utils.retry.catalog.exceptions;

public class CatalogRetryException extends RuntimeException {

    private static final long serialVersionUID = -2520888793776997437L;

    public CatalogRetryException(String msg) {
        super(msg);
    }
}
