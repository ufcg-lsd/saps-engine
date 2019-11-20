package org.fogbowcloud.saps.engine.util;

public class ExecutionScriptTag {

	public static String INPUT_DOWNLOADER = "inputdownloading";
	public static String PROCESSING = "processing";
	public static String PRE_PROCESSING = "preprocessing";

	private String type;
	private String name;
	private String dockerRepository;
	private String dockerTag;

	public ExecutionScriptTag(String name, String dockerRepository, String dockerTag, String type) {
		this.name = name;
		this.dockerRepository = dockerRepository;
		this.dockerTag = dockerTag;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getDockerRepository() {
		return dockerRepository;
	}

	public String getDockerTag() {
		return dockerTag;
	}

	public String formatImageDocker() {
		return dockerRepository + ":" + dockerTag;
	}

	@Override
	public String toString() {
		return "ScriptTag [type=" + type + ", name=" + name + ", dockerRepository="
				+ dockerRepository + ", dockerTag=" + dockerTag + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dockerRepository == null) ? 0 : dockerRepository.hashCode());
		result = prime * result + ((dockerTag == null) ? 0 : dockerTag.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExecutionScriptTag other = (ExecutionScriptTag) obj;
		if (dockerRepository == null) {
			if (other.dockerRepository != null)
				return false;
		} else if (!dockerRepository.equals(other.dockerRepository))
			return false;
		if (dockerTag == null) {
			if (other.dockerTag != null)
				return false;
		} else if (!dockerTag.equals(other.dockerTag))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

}