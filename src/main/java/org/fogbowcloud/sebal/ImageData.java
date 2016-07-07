package org.fogbowcloud.sebal;

import java.io.Serializable;
import java.sql.Date;
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
	private Date creationTime;
	private Date updateTime;
	private String status;
	private String error;
	private Map<String, Integer> tasksStatesCount = new HashMap<String, Integer>();
	
	public static final String AVAILABLE = "available";
	public static final String PURGED = "purged";

	public ImageData(String name, String downloadLink, ImageState state,
			String federationMember, int priority, String stationId,
			String sebalVersion, Date creationTime, Date updateTime, String error) {
		this.name = name;
		this.downloadLink = downloadLink;
		this.state = state;
		this.federationMember = federationMember;
		this.priority = priority;
		this.stationId = stationId;
		this.sebalVersion = sebalVersion;
		this.creationTime = creationTime;
		this.updateTime = updateTime;
		this.status = "available";
		this.error = error;
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
	
	public Date getCreationTime() {
		return creationTime;
	}
	
	public Date getUpdateTime() {
		return updateTime;
	}
	
	public String getImageStatus() {
		return status;
	}
	
	public String getImageError() {
		return error;
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
	
	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}
	
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
	
	public void setImageStatus(String status) {
		this.status = status;
	}
	
	public void setImageError(String error) {
		this.error = error;
	}

	public String toString() {
		return name + ", " + downloadLink + ", " + state.getValue() + ", "
				+ federationMember + ", " + priority + ", " + stationId + ", "
				+ sebalVersion + ", " + creationTime + ", " + updateTime + ", "
				+ status + ", " + error;
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
					&& getImageError().equals(other.getImageError());
		}
		return false;
	}

}
