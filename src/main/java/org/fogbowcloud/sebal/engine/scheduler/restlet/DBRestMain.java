package org.fogbowcloud.sebal.engine.scheduler.restlet;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.sebal.bootstrap.DBUtilsImpl;
import org.fogbowcloud.sebal.engine.sebal.model.SebalUser;

public class DBRestMain {
	
	private static final Logger LOGGER = Logger.getLogger(DBRestMain.class);
	
	public static void main(String[] args) throws Exception {
		
		String confPath = args[0];
		
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(confPath);
		properties.load(input);
		
		DBUtilsImpl dbUtilsImpl = new DBUtilsImpl(properties);
		
		DatabaseApplication databaseApplication = new DatabaseApplication(dbUtilsImpl);
		databaseApplication.startServer();
		
		String userEmail = "admin@admin.com";
		SebalUser user = databaseApplication.getUser(userEmail);
		if(user == null) {			
			String userName = "admin";
			String userPass = DigestUtils.md5Hex("adminadmin");
			
			try {
				databaseApplication.createUser(userEmail, userName, userPass, true,
						false, true);
			} catch (Exception e) {
				LOGGER.error("Error while creating user", e);
			}
		}
	}

}
