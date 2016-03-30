package org.fogbowcloud.sebal;

import java.util.HashMap;
import java.util.Map;

public class ElevationData {
	private String name;
	private String downloadLink;
	private ElevationState state;
	private String federationMember;
	private int priority;
	private Map<String, Integer> tasksStatesCount = new HashMap<String, Integer>();

	public ElevationData(String name, String downloadLink, ElevationState state, String federationMember,
			int priority) {
		this.name = name;
		this.downloadLink = downloadLink;
		this.state = state;
		this.federationMember = federationMember;
		this.priority = priority;
	}

	public String getName() {
		return name;
	}
	
	public String getDownloadLink() {
		return downloadLink;
	}

	public ElevationState getState() {
		return state;
	}

	public String getFederationMember() {
		return federationMember;
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

	public void setState(ElevationState state) {
		this.state = state;
	}

	public void setFederationMember(String federationMember) {
		this.federationMember = federationMember;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String toString() {
		return name + ", " + downloadLink + ", " + state.getValue() + ", " + federationMember
				+ ", " + priority;
	}
	
	public Map<String, Integer> getTasksStatesCount() {
		return tasksStatesCount;
	}

	public void setTasksStatesCount(Map<String, Integer> tasksStatesCount) {
		this.tasksStatesCount = tasksStatesCount;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ElevationData) {
			ElevationData other = (ElevationData) o;
			return getName().equals(other.getName())
					&& getDownloadLink().equals(other.getDownloadLink())
					&& getState().equals(other.getState()) && getPriority() == other.getPriority()
					&& getFederationMember().equals(other.getFederationMember());
		}
		return false;
	}
}
