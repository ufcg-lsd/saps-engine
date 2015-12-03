package org.fogbowcloud.scheduler.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.fogbowcloud.scheduler.core.model.Job;
import org.fogbowcloud.scheduler.core.model.Specification;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowRequirementsHelper;

public class JDFMain {

	String label = null;
	ArrayList<String> initCommands = new ArrayList<String>();
	ArrayList<String> remoteCommands= new ArrayList<String>();
	ArrayList<String> finalCommands = new ArrayList<String>();
	String currentPhase = "";
	static Properties properties;
	static Specification genericJobSpec;
	Task currentTask;
	

	public static void main(String[] args) {
		properties = new Properties();
		genericJobSpec = new Specification(properties.getProperty("image"), properties.getProperty("usename"), properties.getProperty("publikey"), properties.getProperty("privatekeypath"));
		
	}



	public Job getJobFromJDFFile(String filePath) throws IOException {
		File jobDescription = new File(filePath);
		BufferedReader fileReader = new BufferedReader(new FileReader(jobDescription));
		String line = null;
		while ((line = fileReader.readLine()) != null) {
			int splitPoint = line.indexOf(":");
			if (splitPoint == -1) {
				addToCurrentPhase(line);
			} else {
				String key = line.substring(0, splitPoint -1);
				if (key != "task"){
					String value = line.substring(splitPoint +1);
					solveLine(key, value);
				} else {
					break;
				}
			}
		}
		while(true) {

			String lastLine = line;			
			line = fileReader.readLine();
			solveTaskLines(lastLine, line);


			if (line == null) break;
		}
		return null;
	}

	private void solveTaskLines(String lastLine, String currentLine) {
		if (lastLine.startsWith("task")){
			int splitPoint = currentLine.indexOf(":");
			if (splitPoint == -1) {
				addToCurrentPhase(currentLine);
				return;
			}
			String key = currentLine.substring(0, splitPoint -1);
		}
	}

	private void solveLine(String key, String value) {
		if (key.equals("label")){
			this.label = value;
		}

		if (key.equals("init")) {
			this.currentPhase = "init";
			this.initCommands.add(value);
		}
		if (key.equals("remote")) {
			this.currentPhase = "remote";
			this.remoteCommands.add(value);
		}
		if (key.equals("final")) {
			this.currentPhase = "final";
			this.finalCommands.add(value);
		}
		if (key.equals("requirements")){
			this.currentPhase = "requirements";
			genericJobSpec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, genericJobSpec.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS) + " && " + value);
		}

	}

	private void addToCurrentPhase(String value) {
		if (this.currentPhase.equals("init")){
			this.initCommands.add(value);
		}
		if (this.currentPhase.equals("remote")){
			this.remoteCommands.add(value);
		}
		if (this.currentPhase.equals("final")){
			this.finalCommands.add(value);
		}
		if (this.currentPhase.equals("requirements")){
			genericJobSpec.addRequirement(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS, genericJobSpec.getRequirementValue(FogbowRequirementsHelper.METADATA_FOGBOW_REQUIREMENTS) + " && " + value);
		}
	}


}
