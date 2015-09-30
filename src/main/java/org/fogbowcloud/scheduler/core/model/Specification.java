package org.fogbowcloud.scheduler.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Specification {

	String image;
	String publicKey;
	String contextScript;
	
	Map<String, String> requirements = new HashMap<String, String>();

	public Specification(String image, String publicKey) {
		
		super();
		this.image = image;
		this.publicKey = publicKey;
		
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
		sb.append("\nPublicKey: "+publicKey);
		if(contextScript != null && !contextScript.isEmpty()){
			sb.append("\nContextScript: "+contextScript);
		}
		for(Entry<String, String> entry : requirements.entrySet()){
			sb.append("\n"+entry.getKey()+": "+entry.getValue());
		}
		return sb.toString();
	}
}
