package org.fogbowcloud.sebal.bootstrap;

import java.sql.Connection;
import java.sql.SQLException;

import org.fogbowcloud.sebal.ImageState;

public interface DBUtils {

	public Connection getConnection() throws SQLException;

	public void preparingStatement(Connection c) throws SQLException;

	public void updateState(String imageName, ImageState state)
			throws SQLException;

	public void addImages() throws SQLException;

	public void listImagesInDB() throws SQLException;

	public void listCorruptedImages();

	public void getRegionImages() throws SQLException;

}
