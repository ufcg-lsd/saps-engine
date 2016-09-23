package org.fogbowcloud.sebal.engine.scheduler;

import java.sql.SQLException;

import org.fogbowcloud.blowout.scheduler.core.Scheduler;
import org.fogbowcloud.blowout.scheduler.core.TaskExecutionChecker;
import org.fogbowcloud.blowout.scheduler.core.model.TaskProcess;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageDataStore;
import org.fogbowcloud.sebal.engine.sebal.ImageState;

public class SebalTaskExecutionChecker extends TaskExecutionChecker {

	String imageName;
	
	ImageDataStore imageStore;

	public SebalTaskExecutionChecker(TaskProcess tp, Scheduler scheduler, String imageName, ImageDataStore imageStore) {
		super(tp, scheduler);
		this.imageName = imageName;
		this.imageStore = imageStore;
		try {
			imageToRunning(this.imageName);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void imageToRunning(String image) throws SQLException {
		ImageData imageData = this.imageStore.getImage(image);
		imageData.setState(ImageState.RUNNING_R);
		imageStore.updateImage(imageData);
	}

	private void imageToFinnished(String imageName2) throws SQLException {
		ImageData imageData = this.imageStore.getImage(imageName2);
		imageData.setState(ImageState.FINISHED);
		imageStore.updateImage(imageData);
	}

	@Override
	public void failure(TaskProcess tp) {

	}

	@Override
	public void completion(TaskProcess tp) {
		try {
			imageToFinnished(this.imageName);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
