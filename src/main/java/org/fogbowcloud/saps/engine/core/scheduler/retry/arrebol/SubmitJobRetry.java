package org.fogbowcloud.saps.engine.core.scheduler.retry.arrebol;

import org.fogbowcloud.saps.engine.core.model.SapsJob;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.Arrebol;
import org.fogbowcloud.saps.engine.core.scheduler.arrebol.exceptions.SubmitJobException;

public class SubmitJobRetry implements ArrebolRetry<String>{

	private Arrebol arrebol;
	private SapsJob job;
	
	public SubmitJobRetry(Arrebol arrebol, SapsJob job) {
		this.arrebol = arrebol;
		this.job = job;
	}
	
	@Override
	public String run() throws Exception, SubmitJobException {
		return arrebol.addJob(job);
	}

}
