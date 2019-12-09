package org.fogbowcloud.saps.engine.core.scheduler.arrebol.retry;

import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetCountsSlotsException;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetJobException;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.SubmitJobException;

public interface ArrebolRetry<T> {

	public T run() throws Exception, SubmitJobException, GetJobException, GetCountsSlotsException;

}
