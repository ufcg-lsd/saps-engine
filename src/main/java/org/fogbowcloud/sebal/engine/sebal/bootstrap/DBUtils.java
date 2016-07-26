package org.fogbowcloud.sebal.engine.sebal.bootstrap;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

public interface DBUtils {

	void setImagesToPurge(String day, boolean forceRemoveNonFetched) throws SQLException, ParseException;

	void listImagesInDB() throws SQLException, ParseException;

	void listCorruptedImages() throws ParseException;

	void getRegionImages(int firstYear, int lastYear, String region) throws SQLException, ParseException;

	void fillDB(int firstYear, int lastYear, List<String> regions) throws IOException;
}
