package org.fogbowcloud.scheduler.core.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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
	
	Map<String, String> requirements = new HashMap<String, String>();

	public Specification(String image, String username, String publicKey, String privateKeyFilePath) {
		this.image = image;
		this.username = username;
		this.publicKey = publicKey;
		this.privateKeyFilePath = privateKeyFilePath;
	}
	
	public void addRequitement(String key, String value){
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
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Image: "+image);
		sb.append("- PublicKey: "+publicKey);
		if(contextScript != null && !contextScript.isEmpty()){
			sb.append("\nContextScript: "+contextScript);
		}
		if(requirements != null && !requirements.isEmpty()){
			sb.append("\nRequiriments:{");
			for(Entry<String, String> entry : requirements.entrySet()){
				sb.append("\n\t"+entry.getKey()+": "+entry.getValue());
			}
			sb.append("\n}");
		}
		return sb.toString();
	}
}
