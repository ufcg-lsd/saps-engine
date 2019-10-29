package org.fogbowcloud.saps.engine.core.scheduler.retry.catalog;

import java.sql.SQLException;

public interface CatalogRetry<T> {

	public T run() throws SQLException;
}
