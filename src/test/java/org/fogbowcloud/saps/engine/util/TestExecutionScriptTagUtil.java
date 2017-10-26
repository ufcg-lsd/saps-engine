package org.fogbowcloud.saps.engine.util;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestExecutionScriptTagUtil {

	private final String TEST_EXEC_SCRIPT_FILE="src/test/resources/test_execution_script_tags.json";  
	
	@Test
	public void testGetJsonScriptTags() throws Exception {
		JSONObject jsonScriptTags = ExecutionScriptTagUtil.getJsonExecutionScriptTag();
		
		Assert.assertNotNull(jsonScriptTags);
	}
	
	@Test
	public void testGetScritpTag() throws Exception {
		ExecutionScriptTagUtil.setExecScriptTagJsonPath(TEST_EXEC_SCRIPT_FILE);
		
		ExecutionScriptTag scritpTag = ExecutionScriptTagUtil.getExecutionScritpTag("test_name", ExecutionScriptTagUtil.INPUT_DOWNLOADER);
		
		Assert.assertEquals("test_name", scritpTag.getName());
		Assert.assertEquals("test_tag", scritpTag.getDockerTag());
		Assert.assertEquals("test_repository", scritpTag.getDockerRepository());
	}
	
	@Test
	public void testGetScritpTagWithWrongType() throws Exception {
		try {
			ExecutionScriptTagUtil.setExecScriptTagJsonPath(TEST_EXEC_SCRIPT_FILE);
			
			ExecutionScriptTagUtil.getExecutionScritpTag("test_name", "worng_type");			
		} catch (Exception e) {
			Assert.assertEquals(e.getMessage(), ExecutionScriptTagUtil.ERROR_MSG__TYPE_NOT_FOUND);
		}
	}	
	
	@Test
	public void testGetScriptTagJsonPath() {
		String scriptTagJsonPath = ExecutionScriptTagUtil.getExecScriptTagJsonPath();
		Assert.assertEquals(ExecutionScriptTagUtil.DEFAULT_SCRIPT_TAG_JSON_PATH, scriptTagJsonPath);
	}
	
	@Test
	public void testIsValid() throws Exception {
		ExecutionScriptTagUtil.isValidJsonScriptTag();
	}
	
	@Test(expected=Exception.class)
	public void testIsInvalid() throws Exception {
		ExecutionScriptTagUtil.setExecScriptTagJsonPath("/dev/null");
		ExecutionScriptTagUtil.isValidJsonScriptTag();
	}	
	
}
