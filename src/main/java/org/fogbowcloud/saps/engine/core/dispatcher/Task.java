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
	public ImageTask getImageData() {
		return imageData;
	}
	public void setImageData(ImageTask imageData) {
		this.imageData = imageData;
	}
}