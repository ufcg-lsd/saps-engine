package org.fogbowcloud.sebal;

import java.sql.SQLException;
import java.util.List;

public interface DataStore {

	public final static String NONE = "None";
	public final static int UNLIMITED = -1;

	public void addImage(String imageName, String downloadLink, int priority) throws SQLException;
	
	public void addElevation(String elevationName, String downloadLink, int priority) throws SQLException;

	public void updateImage(ImageData imageData) throws SQLException;

	public void updateImageState(String imageName, ImageState state) throws SQLException;
	
	public void updateElevation(ElevationData elevationData) throws SQLException;

	public void updateElevationState(String elevationName, ElevationState state) throws SQLException;
	
	public List<ImageData> getAllImages() throws SQLException;

	public List<ImageData> getImageIn(ImageState state) throws SQLException;

	public List<ImageData> getImageIn(ImageState state, int limit) throws SQLException;
	
	public List<ElevationData> getAllElevation() throws SQLException;

	public List<ElevationData> getElevationIn(ElevationState state) throws SQLException;

	public List<ElevationData> getElevationIn(ElevationState state, int limit) throws SQLException;

	public ImageData getImage(String imageName) throws SQLException;
	
	public ElevationData getElevation(String elevationName) throws SQLException;
	
	public void dispose();

	public boolean lockImage(String imageName) throws SQLException;

	public boolean unlockImage(String imageName) throws SQLException;
	
	public boolean lockElevation(String elevationName) throws SQLException;

	public boolean unlockElevation(String elevationName) throws SQLException;
}
