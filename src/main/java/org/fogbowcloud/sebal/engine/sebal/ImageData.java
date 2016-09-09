package org.fogbowcloud.sebal.engine.sebal;

import java.io.Serializable;
import java.sql.Timestamp;
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
	private String sebalTag;
	private String crawlerVersion;
	private String fetcherVersion;
	private String blowoutVersion;
	private String fmaskVersion;
	private Timestamp creationTime;
	private Timestamp updateTime;
	private String status;
	private String error;
	private Map<String, Integer> tasksStatesCount = new HashMap<String, Integer>();
	
	public static final String AVAILABLE = "available";
	public static final String PURGED = "purged";

	public ImageData(String name, String downloadLink, ImageState state,
			String federationMember, int priority, String stationId,
			String sebalVersion, String sebalTag, String crawlerVersion,
			String fetcherVersion, String blowoutVersion, String fmaskVersion,
			Timestamp creationTime, Timestamp updateTime, String error) {
		this.name = name;
		this.downloadLink = downloadLink;
		this.state = state;
		this.federationMember = federationMember;
		this.priority = priority;
		this.stationId = stationId;
		this.sebalVersion = sebalVersion;
		this.sebalTag = sebalTag;
		this.crawlerVersion = crawlerVersion;
		this.fetcherVersion = fetcherVersion;
		this.blowoutVersion = blowoutVersion;
		this.fmaskVersion = fmaskVersion;
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
	
	public String getSebalTag() {
		return sebalTag;
	}
	
	public String getCrawlerVersion() {
		return crawlerVersion;
	}
	
	public String getFetcherVersion() {
		return fetcherVersion;
	}
	
	public String getBlowoutVersion() {
		return blowoutVersion;
	}
	
	public String getFmaskVersion() {
		return fmaskVersion;
	}
	
	public Timestamp getCreationTime() {
		return creationTime;
	}
	
	public Timestamp getUpdateTime() {
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
	
	public void setSebalTag(String sebalTag) {
		this.sebalTag = sebalTag;
	}
	
	public void setCrawlerVersion(String crawlerVersion) {
		this.crawlerVersion = crawlerVersion;
	}
	
	public void setFetcherVersion(String fetcherVersion) {
		this.fetcherVersion = fetcherVersion;
	}
	
	public void setBlowoutVersion(String blowoutVersion) {
		this.blowoutVersion = blowoutVersion;
	}
	
	public void setFmaskVersion(String fmaskVersion) {
		this.fmaskVersion = fmaskVersion;
	}
	
	public void setCreationTime(Timestamp creationTime) {
		this.creationTime = creationTime;
	}
	
	public void setUpdateTime(Timestamp updateTime) {
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
				+ sebalVersion + ", " + sebalTag + ", " + crawlerVersion + ", "
				+ fetcherVersion + ", " + blowoutVersion + ", " + fmaskVersion
				+ ", " + creationTime + ", " + updateTime + ", " + status
				+ ", " + error;
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
					&& getSebalTag().equals(other.getSebalTag())
					&& getCrawlerVersion().equals(other.getCrawlerVersion())
					&& getFetcherVersion().equals(other.getFetcherVersion())
					&& getBlowoutVersion().equals(other.getBlowoutVersion())
					&& getFmaskVersion().equals(other.getFmaskVersion())
					&& getCreationTime().equals(other.getCreationTime())
					&& getUpdateTime().equals(other.getUpdateTime())
					&& getImageStatus().equals(other.getImageStatus())
					&& getImageError().equals(other.getImageError());
		}
		return false;
	}

}
