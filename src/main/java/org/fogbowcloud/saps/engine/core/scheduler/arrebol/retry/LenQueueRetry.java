package org.fogbowcloud.saps.engine.core.scheduler.arrebol.retry;

import org.fogbowcloud.saps.engine.core.scheduler.arrebol.Arrebol;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.GetCountsSlotsException;

public class LenQueueRetry implements ArrebolRetry<Integer> {

	private Arrebol arrebol;
	private String queueId;

	public LenQueueRetry(Arrebol arrebol, String queueId) {
		this.arrebol = arrebol;
		this.queueId = queueId;
	}

	@Override
	public Integer run() throws GetCountsSlotsException {
		return arrebol.getCountSlotsInQueue(queueId);
	}

}
