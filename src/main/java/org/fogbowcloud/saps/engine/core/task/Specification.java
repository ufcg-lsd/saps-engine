package org.fogbowcloud.saps.engine.core.task;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

public class Specification implements Serializable {

	private static final String IMAGE_STR = "image";

	private static final String REQUIREMENTS_STR = "requirements";

	private static final Logger LOGGER = Logger.getLogger(Specification.class);

	/**
	 *
	 */
	private static final long serialVersionUID = 5255295548723927267L;

	private String image;

	private Map<String, String> requirements;

	public Specification(String image, Map<String, String> requirements){
		this.image = image;
		this.requirements = requirements;
	}

	public String getImage() {
		return image;
	}

	public Map<String, String> getRequirements() {
		return requirements;
	}

	public void setImage(String image){
		this.image = image;
	}

	public void setRequirements (Map<String, String> requirements) {
		this.requirements = requirements;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Image: " + image);

		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((image == null) ? 0 : image.hashCode());
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

		if (image == null) {
			if (other.image != null)
				return false;
		} else if (!image.equals(other.image))
			return false;

		return true;
	}

	public JSONObject toJSON() {
		try {
			JSONObject specification = new JSONObject();
			specification.put(IMAGE_STR, this.getImage());
			specification.put(REQUIREMENTS_STR, this.getRequirements());
			return specification;
		} catch (JSONException e) {
			LOGGER.debug("Error while trying to create a JSON from Specification", e);
			return null;
		}
	}

	public static Specification fromJSON(JSONObject specJSON) {
		Specification specification = new Specification(specJSON.optString(IMAGE_STR), new HashMap<String, String>());
		return specification;
	}

}
