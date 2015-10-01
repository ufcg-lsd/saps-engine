package org.fogbowcloud.sebal;


public enum ImageState {

	NOT_DOWNLOADED("not_downloaded"), DOWNLOADED("downloaded"), DOWNLOADING("downloading"), RUNNING(
			"running"), REDUCING("reducing"), UPLOADING("uploading"), UPLOADED("uploaded"), FINISHED(
			"finished");

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
			if (currentState.getValue().equals(stateStr)) {
				return currentState;
			}
		}
		return null;
	}
}
