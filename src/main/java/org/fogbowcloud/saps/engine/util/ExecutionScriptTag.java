package org.fogbowcloud.saps.engine.util;

public class ExecutionScriptTag {
	
	public static String INPUT_DOWNLOADER = "input_downloader";
	public static String WORKER = "worker";
	public static String PRE_PROCESSING = "pre_processing";
	
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

	@Override
	public String toString() {
		return "ScriptTag [type=" + type + ", name=" + name + ", dockerRepository="
				+ dockerRepository + ", dockerTag=" + dockerTag + "]";
	}
	
}