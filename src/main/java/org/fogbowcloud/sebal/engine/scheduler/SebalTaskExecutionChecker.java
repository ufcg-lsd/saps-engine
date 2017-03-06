package org.fogbowcloud.sebal.engine.scheduler;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.core.SchedulerInterface;
import org.fogbowcloud.blowout.core.model.TaskProcess;
import org.fogbowcloud.blowout.core.model.TaskProcessImpl;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;

// TODO: Check this to see how timeout is implemented
public class SebalTaskExecutionChecker {
//	private static final Logger LOGGER = Logger.getLogger(SebalTaskExecutionChecker.class);
//	
//	String imageName;	
//	ImageDataStore imageStore;
//
//	public SebalTaskExecutionChecker(TaskProcess tp, Scheduler scheduler, String imageName, ImageDataStore imageStore) {
//		super(tp, scheduler);
//		this.imageName = imageName;
//		this.imageStore = imageStore;
//		try {
//			imageToRunning(this.imageName);
//		} catch (SQLException e) {
//			LOGGER.debug("Could not change image '" + this.imageName + "' state to RUNNING", e);
//		}
//	}
//
//	private void imageToRunning(String image) throws SQLException {
//		ImageData imageData = this.imageStore.getImage(image);
//		
//		// TODO: test this
//		if(!imageData.getState().equals(ImageState.RUNNING)) {
//			imageData.setState(ImageState.RUNNING);
//			imageStore.updateImage(imageData);
//
//			// Inserting update time into stateStamps table in DB
//			imageData.setUpdateTime(imageStore.getImage(imageData.getName())
//					.getUpdateTime());
//			imageStore.addStateStamp(imageData.getName(), imageData.getState(),
//					imageData.getUpdateTime());
//		}
//	}
//
//	private void imageToFinnished(String imageName) throws SQLException {
//		ImageData imageData = this.imageStore.getImage(imageName);
//		imageData.setState(ImageState.FINISHED);
//		imageStore.updateImage(imageData);
//		
//		// Inserting update time into stateStamps table in DB
//		imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
//		imageStore.addStateStamp(imageData.getName(), imageData.getState(), imageData.getUpdateTime());
//	}
//
//	private void imageToFailed(String imageName) throws SQLException {
//		ImageData imageData = this.imageStore.getImage(imageName);
//		imageData.setState(ImageState.QUEUED);
//		imageStore.updateImage(imageData);
//		
//		// Inserting update time into stateStamps table in DB
//		imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
//		imageStore.addStateStamp(imageData.getName(), imageData.getState(), imageData.getUpdateTime());
//	}
//	
//	private void imageToTimedout(String imageName) throws SQLException {
//		ImageData imageData = this.imageStore.getImage(imageName);
//		imageData.setState(ImageState.ERROR);
//		imageData.setImageError("Image " +  imageName + " process timedout");
//		imageStore.updateImage(imageData);
//		
//		// Inserting update time into stateStamps table in DB
//		imageData.setUpdateTime(imageStore.getImage(imageData.getName()).getUpdateTime());
//		imageStore.addStateStamp(imageData.getName(), imageData.getState(), imageData.getUpdateTime());
//	}
//	
//	@Override
//	public void failure(TaskProcess tp) {
//		try {
//			if(tp.getStatus().getNumVal() == TaskProcessImpl.State.TIMEDOUT.getNumVal()) {
//				imageToTimedout(this.imageName);
//			} else {
//				imageToFailed(this.imageName);				
//			}			
//		} catch (SQLException e) {
//			LOGGER.debug("Could not change image '" + this.imageName + "' state to Finnished", e);
//		}
//	}
//
//	@Override
//	public void completion(TaskProcess tp) {
//		try {
//			imageToFinnished(this.imageName);
//		} catch (SQLException e) {
//			LOGGER.debug("Could not change image '" + this.imageName + "' state to Finnished", e);
//		}
//	}
}
