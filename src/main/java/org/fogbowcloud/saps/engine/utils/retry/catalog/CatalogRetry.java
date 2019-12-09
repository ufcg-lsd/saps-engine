package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;

public interface CatalogRetry<T> {

	public T run() throws SQLException;
}
