package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;

public interface CatalogRetry<T> {

	T run() throws SQLException;
}
