package org.fogbowcloud.saps.engine.core.preprocessor;

import org.fogbowcloud.saps.engine.core.model.ImageTask;

public interface PreProcessor {
	
	public void preProcessImage(ImageTask imageTask);
	
	public void exec();

}
