package org.fogbowcloud.saps.engine.core.scheduler.retry.arrebol;

import org.fogbowcloud.saps.engine.core.scheduler.arrebol.Arrebol;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetCountsSlotsException;

public class LenQueueRetry implements ArrebolRetry<Integer> {

	private Arrebol arrebol;

	public LenQueueRetry(Arrebol arrebol) {
		this.arrebol = arrebol;
	}

	@Override
	public Integer run() throws GetCountsSlotsException {
		return arrebol.getCountSlotsInQueue();
	}

}
