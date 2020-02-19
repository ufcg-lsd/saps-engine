package org.fogbowcloud.saps.engine.core.catalog.jdbc.exceptions;

//FIXME Remove this class
public class JDBCCatalogException extends Exception {

    private static final long serialVersionUID = -2520888793776997437L;

    public JDBCCatalogException(String msg, Exception e) {
        super(msg, e);
    }
}
