package org.fogbowcloud.sebal.engine.sebal;

import java.util.ArrayList;
import java.util.List;

public enum ImageState {

	NOT_DOWNLOADED("not_downloaded"), DOWNLOADED("downloaded"), DOWNLOADING(
			"downloading"),QUEUED("queued"), RUNNING_R("running_r"), FINISHED("finished"), FETCHING(
			"fetching"), FETCHED("fetched"), CORRUPTED("corrupted"), ERROR("error");

	private String value;

	private ImageState(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	
	public boolean in(ImageState... imageStates) {
		for (ImageState imageState : imageStates) {
			if (imageState.equals(this)){
				return true;
			}
		}
		return false;
	}
	
	public static ImageState getStateFromStr(String stateStr) {
		ImageState[] elements = values();
		for (ImageState currentState : elements) {
			if (currentState.getValue().equals(stateStr) ||
					currentState.name().equals(stateStr)) {
				return currentState;
			}
		}
		return null;
	}
	
	public static List<String> getAllValues(){
		
		List<String> values = new ArrayList<String>();
		
		for (ImageState currentState : values()) {
			values.add(currentState.name());
		}
		
		return values;
	}
	
}
