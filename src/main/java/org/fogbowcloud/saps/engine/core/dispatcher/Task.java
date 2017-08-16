package org.fogbowcloud.saps.engine.core.dispatcher;

import org.fogbowcloud.saps.engine.core.model.ImageTask;

public class Task {

	private String id;
	private ImageTask imageData;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ImageTask getImageTask() {
		return imageData;
	}

	public void setImageTask(ImageTask imageTask) {
		this.imageData = imageTask;
	}
}