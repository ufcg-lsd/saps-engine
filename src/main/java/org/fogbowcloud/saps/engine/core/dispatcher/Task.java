package org.fogbowcloud.saps.engine.core.dispatcher;

import org.fogbowcloud.saps.engine.core.model.ImageTask;

public class Task {

	private String id;
	private ImageTask imageTask;
	
	public Task(String id) {
		this.id = id;
	}
	
	public Task(String id, ImageTask imageTask) {
		this.id = id;
		this.imageTask = imageTask;
	}

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
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Task) {
			Task other = (Task) o;
			return getId().equals(other.getId()) && getImageTask().equals(other.getImageTask());
		}
		return false;
	}
}