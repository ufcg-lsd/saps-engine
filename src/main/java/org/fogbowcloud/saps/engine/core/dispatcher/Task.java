package org.fogbowcloud.saps.engine.core.dispatcher;

import org.fogbowcloud.saps.engine.core.model.ImageTask;

public class Task {

	private String id;
	private ImageTask imageTask;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ImageTask getImageTask() {
		return imageTask;
	}

	public void setImageTask(ImageTask imageTask) {
		this.imageTask = imageTask;
	}
}