package org.fogbowcloud.sebal;


public enum ImageState {

	NOT_DOWNLOADED("not_downloaded"), DOWNLOADED("downloaded"), DOWNLOADING(
			"downloading"), RUNNING_F1("running_f1"), READY_FOR_PHASE_C(
			"ready_for_phase_c"), RUNNING_C("running_c"), READY_FOR_PHASE_F2(
			"ready_for_phase_f2"), RUNNING_F2("running_f2"), RUNNING_R("running_r"), FINISHED("finished"), FETCHING(
			"fetching"), FETCHED("fetched"), CORRUPTED("corrupted"), TO_PURGE(
			"to_purge"), PURGED("purged");

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
