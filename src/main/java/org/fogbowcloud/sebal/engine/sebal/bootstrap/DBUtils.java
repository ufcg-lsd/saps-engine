package org.fogbowcloud.sebal.engine.sebal.bootstrap;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface DBUtils {

	void setImagesToPurge(String day, boolean forceRemoveNonFetched) throws SQLException;

	void listImagesInDB() throws SQLException;

	void listCorruptedImages();

	void getRegionImages(int firstYear, int lastYear, String region) throws SQLException;

	void fillDB(int firstYear, int lastYear, List<String> regions) throws IOException;
}
