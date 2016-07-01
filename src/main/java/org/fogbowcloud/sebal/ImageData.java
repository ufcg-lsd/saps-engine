package org.fogbowcloud.sebal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ImageData implements Serializable {

	private String name;
	private String downloadLink;
	private ImageState state;
	private String federationMember;
	private int priority;
	private String stationId;
	private String sebalVersion;
	private String creationTime;
	private String updateTime;
	private String downloadingUpdateTime;
	private String downloadedUpdateTime;
	private String runningRUpdateTime;
	private String finishedUpdateTime;
	private String fetchingUpdateTime;
	private String fetchedUpdateTime;
	private String corruptedUpdateTime;
	private String status;
	private Map<String, Integer> tasksStatesCount = new HashMap<String, Integer>();
	
	public static final String AVAILABLE = "available";
	public static final String PURGED = "purged";

	public ImageData(String name, String downloadLink, ImageState state,
			String federationMember, int priority, String stationId,
			String sebalVersion, String creationTime, String updateTime,
			String downloadingTime, String downloadedTime, String runningRTime,
			String finishedTime, String fetchingTime, String fetchedTime,
			String corruptedTime) {
		this.name = name;
		this.downloadLink = downloadLink;
		this.state = state;
		this.federationMember = federationMember;
		this.priority = priority;
		this.stationId = stationId;
		this.sebalVersion = sebalVersion;
		this.creationTime = creationTime;
		this.updateTime = updateTime;
		this.downloadingUpdateTime = downloadedTime;
		this.downloadedUpdateTime = downloadedTime;
		this.runningRUpdateTime = runningRTime;
		this.finishedUpdateTime = finishedTime;
		this.fetchingUpdateTime = fetchingTime;
		this.fetchedUpdateTime = fetchedTime;
		this.corruptedUpdateTime = corruptedTime;
		this.status = "available";
	}

	public String getName() {
		return name;
	}
	
	public String getDownloadLink() {
		return downloadLink;
	}

	public ImageState getState() {
		return state;
	}

	public String getFederationMember() {
		return federationMember;
	}

	public int getPriority() {
		return priority;
	}
	
	public String getStationId() {
		return stationId;
	}
	
	public String getSebalVersion() {
		return sebalVersion;
	}
	
	public String getCreationTime() {
		return creationTime;
	}
	
	public String getUpdateTime() {
		return updateTime;
	}
	
	public String getImageStatus() {
		return status;
	}
	
	public String getDownloadingUpdateTime() {
		return downloadingUpdateTime;
	}
	
	public String getDownloadedUpdateTime() {
		return downloadedUpdateTime;
	}
	
	public String getRunningRUpdateTime() {
		return runningRUpdateTime;
	}
	
	public String getFinishedUpdateTime() {
		return finishedUpdateTime;
	}
	
	public String getFetchingUpdateTime() {
		return fetchingUpdateTime;
	}
	
	public String getFetchedUpdateTime() {
		return fetchedUpdateTime;
	}
	
	public String getCorruptedUpdateTime() {
		return corruptedUpdateTime;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setDownloadLink(String downloadLink) {
		this.downloadLink = downloadLink;
	}

	public void setState(ImageState state) {
		this.state = state;
	}

	public void setFederationMember(String federationMember) {
		this.federationMember = federationMember;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	public void setStationId(String stationId) {
		this.stationId = stationId;
	}
	
	public void setSebalVersion(String sebalVersion) {
		this.sebalVersion = sebalVersion;
	}
	
	public void setCreationTime(String creationTime) {
		this.creationTime = creationTime;
	}
	
	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}
	
	public void setImageStatus(String status) {
		this.status = status;
	}
	
	public void setDownloadingUpdateTime(String downloadingUpdateTime) {
		this.downloadingUpdateTime = downloadingUpdateTime;
	}
	
	public void setDownloadedUpdateTime(String downloadedUpdateTime) {
		this.downloadedUpdateTime = downloadedUpdateTime;
	}
	
	public void setRunningRUpdateTime(String runningRUpdateTime) {
		this.runningRUpdateTime = runningRUpdateTime;
	}
	
	public void setFinishedUpdateTime(String finishedUpdateTime) {
		this.finishedUpdateTime = finishedUpdateTime;
	}
	
	public void setFetchingUpdateTime(String fetchingUpdateTime) {
		this.fetchingUpdateTime = fetchingUpdateTime;
	}
	
	public void setFetchedUpdateTime(String fetchedUpdateTime) {
		this.fetchedUpdateTime = fetchedUpdateTime;
	}
	
	public void setCorruptedUpdateTime(String corruptedUpdateTime) {
		this.corruptedUpdateTime = corruptedUpdateTime;
	}

	public String toString() {
		return name + ", " + downloadLink + ", " + state.getValue() + ", "
				+ federationMember + ", " + priority + ", " + stationId + ", "
				+ sebalVersion + ", " + creationTime + ", " + updateTime + ", "
				+ status + ", " + downloadingUpdateTime + ", "
				+ downloadedUpdateTime + ", " + runningRUpdateTime + ", "
				+ finishedUpdateTime + ", " + fetchingUpdateTime + ", "
				+ fetchedUpdateTime + ", " + corruptedUpdateTime;
	}
	
	public Map<String, Integer> getTasksStatesCount() {
		return tasksStatesCount;
	}

	public void setTasksStatesCount(Map<String, Integer> tasksStatesCount) {
		this.tasksStatesCount = tasksStatesCount;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ImageData) {
			ImageData other = (ImageData) o;
			return getName().equals(other.getName())
					&& getDownloadLink().equals(other.getDownloadLink())
					&& getState().equals(other.getState()) && getPriority() == other.getPriority()
					&& getFederationMember().equals(other.getFederationMember())
					&& getStationId().equals(other.getStationId())
					&& getSebalVersion().equals(other.getSebalVersion())
					&& getCreationTime().equals(other.getCreationTime())
					&& getUpdateTime().equals(other.getUpdateTime())
					&& getImageStatus().equals(other.getImageStatus())
					&& getDownloadingUpdateTime().equals(other.getDownloadingUpdateTime())
					&& getDownloadedUpdateTime().equals(other.getDownloadedUpdateTime())
					&& getRunningRUpdateTime().equals(other.getRunningRUpdateTime())
					&& getFinishedUpdateTime().equals(other.getFinishedUpdateTime())
					&& getFetchingUpdateTime().equals(other.getFetchingUpdateTime())
					&& getFetchedUpdateTime().equals(other.getFetchedUpdateTime())
					&& getCorruptedUpdateTime().equals(other.getCorruptedUpdateTime());
		}
		return false;
	}

}
