package org.fogbowcloud.sebal;

import java.util.HashMap;
import java.util.Map;

public class ImageData {

	private String name;
	private String downloadLink;
	private ImageState state;
	private String federationMember;
	private int priority;
	private String remoteRepositoryIP;
	private Map<String, Integer> tasksStatesCount = new HashMap<String, Integer>();

	public ImageData(String name, String downloadLink, ImageState state, String federationMember,
			int priority, String siteIP) {
		this.name = name;
		this.downloadLink = downloadLink;
		this.state = state;
		this.federationMember = federationMember;
		this.priority = priority;
		this.remoteRepositoryIP = siteIP;
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
	
	public String getRemoteRepositoryIP() {
		return remoteRepositoryIP;
	}

	public int getPriority() {
		return priority;
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
	
	public void setRemoteRepositoryIP(String remoteRepositoryIP) {
		this.remoteRepositoryIP = remoteRepositoryIP;
	}

	public String toString() {
		return name + ", " + downloadLink + ", " + state.getValue() + ", " + federationMember
				+ ", " + priority + ", " + remoteRepositoryIP;
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
					&& getRemoteRepositoryIP().equals(other.getRemoteRepositoryIP());
		}
		return false;
	}

}
