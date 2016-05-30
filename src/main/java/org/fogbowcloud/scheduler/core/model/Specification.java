package org.fogbowcloud.scheduler.core.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;

public class Specification {

	String image;
	String username;
	String privateKeyFilePath;
	String publicKey;
	String contextScript;
	String userDataFile;
	String userDataType;
	
	Map<String, String> requirements = new HashMap<String, String>();

	public Specification(String image, String username, String publicKey,
			String privateKeyFilePath, String userDataFile, String userDataType) {
		this.image = image;
		this.username = username;
		this.publicKey = publicKey;
		this.privateKeyFilePath = privateKeyFilePath;
		this.userDataFile = userDataFile;
		this.userDataType = userDataType;
	}
	
	public void addRequirement(String key, String value){
		requirements.put(key, value);
	}
	
	public String getRequirementValue(String key){
		return requirements.get(key);
	}

	public void putAllRequirements(Map<String, String> requirements){
		for(Entry<String, String> e : requirements.entrySet()){
			requirements.put(e.getKey(), e.getValue());
		}
	}
	
	public Map<String, String> getAllRequirements(){
		return requirements;
	}
	
	public void removeAllRequirements(){
		requirements = new HashMap<String, String>();
	}
	
	public static List<Specification> getSpecificationsFromJSonFile(String jsonFilePath) throws IOException{
		

		List<Specification> specifications = new ArrayList<Specification>();
		if (jsonFilePath != null && !jsonFilePath.isEmpty()) {
			
			BufferedReader br = new BufferedReader(new FileReader(jsonFilePath));

			Gson gson = new Gson();
			specifications = Arrays.asList(gson.fromJson(br, Specification[].class));
			br.close();
		}
		return specifications;
	}
	
	public boolean parseToJsonFile(String jsonDestFilePath){
		
		List<Specification> spec = new ArrayList<Specification>();
		spec.add(this);
		return Specification.parseSpecsToJsonFile(spec, jsonDestFilePath);
	} 
	
	public static boolean parseSpecsToJsonFile(List<Specification> specs, String jsonDestFilePath){
		
		if (jsonDestFilePath != null && !jsonDestFilePath.isEmpty()) {

			BufferedWriter bw;
			try {
				bw = new BufferedWriter(new FileWriter(jsonDestFilePath));
				Gson gson = new Gson();
				String spectString = gson.toJson(specs);
				bw.write(spectString);
				bw.close();
				return true;
			} catch (IOException e) {
				return false;
			}
		}else{
			return false;
		}
	}
	
	// ----- GETTERS and SETTERS ------ //
	
	
	public String getImage() {
		return image;
	}

	public String getUsername() {
		return username;
	}

	public String getPrivateKeyFilePath() {
		return privateKeyFilePath;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public String getContextScript() {
		return contextScript;
	}

	public void setContextScript(String contextScript) {
		this.contextScript = contextScript;
	}
	
	public String getUserDataFile() {
		return userDataFile;
	}

	public void setUserDataFile(String userDataFile) {
		this.userDataFile = userDataFile;
	}
	
	public String getUserDataType() {
		return userDataType;
	}
	
	public void setUserDataType(String userDataType) {
		this.userDataType = userDataType;
	} 
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Image: " + image);
		sb.append("- PublicKey: " + publicKey);
		if (contextScript != null && !contextScript.isEmpty()) {
			sb.append("\nContextScript: " + contextScript);
		}
		if(userDataFile != null && !userDataFile.isEmpty()) {
			sb.append("\nUserDataFile:" + userDataFile);
		}
		if(userDataType != null && !userDataType.isEmpty()) {
			sb.append("\nUserDataType:" + userDataType);
		}
		if (requirements != null && !requirements.isEmpty()) {
			sb.append("\nRequiriments:{");
			for (Entry<String, String> entry : requirements.entrySet()) {
				sb.append("\n\t" + entry.getKey() + ": " + entry.getValue());
			}
			sb.append("\n}");
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contextScript == null) ? 0 : contextScript.hashCode());
		result = prime * result + ((image == null) ? 0 : image.hashCode());
		result = prime * result + ((privateKeyFilePath == null) ? 0 : privateKeyFilePath.hashCode());
		result = prime * result + ((publicKey == null) ? 0 : publicKey.hashCode());
		result = prime * result + ((userDataFile == null) ? 0 : userDataFile.hashCode());
		result = prime * result + ((userDataType == null) ? 0 : userDataType.hashCode());
		result = prime * result + ((requirements == null) ? 0 : requirements.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
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
		Specification other = (Specification) obj;
		if (contextScript == null) {
			if (other.contextScript != null)
				return false;
		} else if (!contextScript.equals(other.contextScript))
			return false;
		if (image == null) {
			if (other.image != null)
				return false;
		} else if (!image.equals(other.image))
			return false;
		if (privateKeyFilePath == null) {
			if (other.privateKeyFilePath != null)
				return false;
		} else if (!privateKeyFilePath.equals(other.privateKeyFilePath))
			return false;
		if (publicKey == null) {
			if (other.publicKey != null)
				return false;
		} else if (!publicKey.equals(other.publicKey))
			return false;
		if(userDataFile == null) {
			if(other.userDataFile != null)
				return false;
		} else if(!userDataFile.equals(other.userDataFile))
			return false;
		if(userDataType == null) {
			if(other.userDataType != null)
				return false;
		} else if(!userDataType.equals(other.userDataType))
			return false;
		if (requirements == null) {
			if (other.requirements != null)
				return false;
		} else if (!requirements.equals(other.requirements))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}
	
	
}
