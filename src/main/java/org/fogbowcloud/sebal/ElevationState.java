package org.fogbowcloud.sebal;

public enum ElevationState {
	NOT_DOWNLOADED("not_downloaded"), DOWNLOADED("downloaded"), DOWNLOADING(
			"downloading"), READY_FOR_R("ready_for_r"), RUNNING_R("running_r"), FINISHED(
			"finished");

	private String value;

	private ElevationState(String value) {
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
	
	public static ElevationState getStateFromStr(String stateStr) {
		ElevationState[] elements = values();
		for (ElevationState currentState : elements) {
			if (currentState.getValue().equals(stateStr)) {
				return currentState;
			}
		}
		return null;
	}
}
