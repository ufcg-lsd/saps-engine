package org.fogbowcloud.sebal.bootstrap;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class DBBootstrapMain {

	private static final Logger LOGGER = Logger
			.getLogger(DBBootstrapMain.class);

	public static void main(String[] args) throws IOException, SQLException {
		final Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);

		String sqlIP = args[1];
		String sqlPort = args[2];
		String dbUserName = args[3];
		String dbUserPass = args[4];
		String dbUseType = args[5];
		String firstYear = args[6];
		String lastYear = args[7];
		String regionsFilePath = args[8];
		String specificRegion = args[9];

		DBUtilsImpl dbUtilsImpl = new DBUtilsImpl(properties, sqlIP, sqlPort,
				dbUserName, dbUserPass, firstYear, lastYear, regionsFilePath,
				specificRegion);

		if (dbUseType.equals("add")) {
			dbUtilsImpl.addImages();
		} else if (dbUseType.equals("list")) {
			dbUtilsImpl.listImagesInDB();
		} else if (dbUseType.equals("list-corrupted")) {
			dbUtilsImpl.listCorruptedImages();
		} else if (dbUseType.equals("get")) {
			dbUtilsImpl.getRegionImages();
		}

		LOGGER.info("Operation done successfully");
	}

}
