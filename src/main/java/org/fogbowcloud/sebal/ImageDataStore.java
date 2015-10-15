package org.fogbowcloud.sebal;

import java.sql.SQLException;
import java.util.List;

public interface ImageDataStore {

	public final static String NONE = "None";
	public final static int UNLIMITED = -1;

	public void add(String imageName, String downloadLink, int priority) throws SQLException;

	public void update(ImageData imageData) throws SQLException;

	public void updateState(String imageName, ImageState state) throws SQLException;
	
	public List<ImageData> getAll() throws SQLException;

	public List<ImageData> getIn(ImageState state) throws SQLException;

	public List<ImageData> getIn(ImageState state, int limit) throws SQLException;

	public ImageData get(String imageName) throws SQLException;
	
	public void dispose();

	public boolean lock(String imageName) throws SQLException;

	public boolean unlock(String imageName) throws SQLException;
}
