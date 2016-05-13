package org.fogbowcloud.sebal.bootstrap;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestBootstrap {
	
	private Properties properties;
	private DBUtilsImpl dbUtilsImplMock;
	private String sqlIP = "image-store-IP";
	private String sqlPort = "image-store-port";
	private String dbUserName = "db-user-name";
	private String dbUserPass = "db-user-pass";
	private String firstYear = "first-year";
	private String lastYear = "last-year";
	private String regionsFilePath = "regions-file-path";
	private String specificRegion = "specific-region";

	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Test
	public void test() {
		fail("Not yet implemented");
	}

}
