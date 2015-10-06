package org.fogbowcloud.scheduler.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
