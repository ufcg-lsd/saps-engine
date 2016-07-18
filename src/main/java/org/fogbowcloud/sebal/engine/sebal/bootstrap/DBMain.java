package org.fogbowcloud.sebal.engine.sebal.bootstrap;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class DBMain {

	private static final String CONF_PATH = "src/main/resources/sebal.conf";
	private static final Logger LOGGER = Logger
			.getLogger(DBMain.class);

	public static void main(String[] args) throws IOException, SQLException {
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(CONF_PATH);
		properties.load(input);
		
		String dbUseType = args[0];
		String regionsFilePath = args[1];
		String firstYear = args[2];
		String lastYear = args[3];
		String day = args[4];
		String dayOption = args[5];

		DBUtilsImpl dbUtilsImpl = new DBUtilsImpl(properties, firstYear, lastYear, regionsFilePath);

		if (dbUseType.equals("add")) {
			dbUtilsImpl.addImages();
		} else if (dbUseType.equals("list")) {
			dbUtilsImpl.listImagesInDB();
		} else if (dbUseType.equals("list-corrupted")) {
			dbUtilsImpl.listCorruptedImages();
		} else if (dbUseType.equals("get")) {
			dbUtilsImpl.getRegionImages();
		} else if(dbUseType.equals("purge")) {
			dbUtilsImpl.setImagesToPurge(day, dayOption);
		}

		LOGGER.info("Operation done successfully");
	}

}
