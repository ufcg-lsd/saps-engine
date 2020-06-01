package org.fogbowcloud.saps.engine.utils;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.json.JSONArray;
import org.json.JSONObject;

public class ExecutionScriptTagUtil {
		
	private static final Logger LOGGER = Logger.getLogger(ExecutionScriptTagUtil.class);
	
	protected static final String ERROR_MSG__TYPE_NOT_FOUND = "Execution Script Tag type not found.";
	protected static String DEFAULT_SCRIPT_TAG_JSON_PATH = "resources/execution_script_tags.json";
	protected static String DEFAULT_ENDTOEND_SCRIPT_TAG_JSON_PATH = "resources/execution_endtoend_script_tags.json";
	
	public static String INPUT_DOWNLOADER = ExecutionScriptTag.INPUT_DOWNLOADER;
	public static String PROCESSING = ExecutionScriptTag.PROCESSING;
	public static String PRE_PROCESSING = ExecutionScriptTag.PRE_PROCESSING;
	
	private static final String NAME_KEY_JSON = "name";
	private static final String DOCKER_TAG_KEY_JSON = "docker_tag";
	private static final String DOCKER_REPOSITORY_KEY_JSON = "docker_repository";
	
	private static String scriptTagJsonPath;
	
	public static void isValidJsonScriptTag(String scriptTagPath) throws Exception {
		Boolean isValid = true;
		try {
			JSONObject jsonExecutionScriptTags = getJsonExecutionScriptTag(scriptTagPath);
			if (jsonExecutionScriptTags == null) {
				isValid = false;
			}
		} catch (Exception e) {
			isValid = false;
		}
		
		if (!isValid) {
			throw new Exception("Execution Script Tag Json File is invalid.");
		}
	}
	
	public static ExecutionScriptTag getExecutionScriptTag(String name, String type)  {
		LOGGER.debug("Getting Execution Script Tag by name [" + name + "] and type [" + type + "]");
		JSONObject jsonScriptTagFile = null;
		ExecutionScriptTag executionScriptTag = null;
		try {
			jsonScriptTagFile = ExecutionScriptTagUtil.getJsonExecutionScriptTag(DEFAULT_SCRIPT_TAG_JSON_PATH);

			executionScriptTag = findExecutionScriptTag(name, type, jsonScriptTagFile);
			LOGGER.debug("Execution Script Tag Found: " + executionScriptTag.toString());
		} catch (Exception e1) {
			LOGGER.error("Script tag by name [" + name + "] and type [" + type + "] not found in " + DEFAULT_SCRIPT_TAG_JSON_PATH + ". " + e1.getMessage(), e1);
			try {
				LOGGER.info("Searching in endtoend script tag JSON [" + DEFAULT_ENDTOEND_SCRIPT_TAG_JSON_PATH);
				jsonScriptTagFile = ExecutionScriptTagUtil.getJsonExecutionScriptTag(DEFAULT_ENDTOEND_SCRIPT_TAG_JSON_PATH);
				executionScriptTag = findExecutionScriptTag(name, type, jsonScriptTagFile);
                       		LOGGER.debug("Execution Script Tag Found: " + executionScriptTag.toString());
			} catch (Exception e2) {
				LOGGER.error("Script not found." + e2.getMessage(), e2);
			}
		}	
		
		return executionScriptTag;
	}
	
	protected static ExecutionScriptTag findExecutionScriptTag(String name, String type, JSONObject jsonExecScriptTagFile) throws SapsException {
		try {
			JSONArray jArrayScriptTags = jsonExecScriptTagFile.optJSONArray(type);
			if (jArrayScriptTags == null) {
				throw new Exception(ERROR_MSG__TYPE_NOT_FOUND);
			}
			
			for (int i = 0; i < jArrayScriptTags.length(); i++) {
				JSONObject jsonScriptTag = jArrayScriptTags.optJSONObject(i);
				String scriptTagName = jsonScriptTag.optString(NAME_KEY_JSON);
				if (scriptTagName != null && scriptTagName.equals(name)) {
					String dockerTag = jsonScriptTag.optString(DOCKER_TAG_KEY_JSON);
					String dockerRepository = jsonScriptTag.optString(DOCKER_REPOSITORY_KEY_JSON);
					
					return new ExecutionScriptTag(name, dockerRepository, dockerTag, type);
				}
			}
			throw new Exception("Execution Script Tag by name (" + name + ") not found.");
		} catch (Exception e) {
			throw new SapsException(e.getMessage(), e); 
		}
	}
	
	protected static JSONObject getJsonExecutionScriptTag(String scriptTagPath) throws Exception {
		try {
			String scriptTagJsonPath = scriptTagPath;
			InputStream is = new FileInputStream(scriptTagJsonPath);
			String jsonTxt = IOUtils.toString(is, "UTF-8");
			return new JSONObject(jsonTxt);
		} catch (Exception e) {
			String errorMsg = "Error while getting Execution Script Tag file.";
			LOGGER.error(errorMsg, e);
			throw new Exception(errorMsg);
		}
	}
	
	protected static void setExecScriptTagJsonPath(String scriptTagJsonPath) {
		ExecutionScriptTagUtil.scriptTagJsonPath = scriptTagJsonPath;
	}
	
}
