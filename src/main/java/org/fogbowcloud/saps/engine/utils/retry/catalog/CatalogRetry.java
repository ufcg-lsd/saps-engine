package org.fogbowcloud.saps.engine.utils.retry.catalog;

import org.fogbowcloud.saps.engine.utils.retry.catalog.exceptions.CatalogRetryException;

public interface CatalogRetry<T> {

	T run() throws CatalogRetryException;
}
