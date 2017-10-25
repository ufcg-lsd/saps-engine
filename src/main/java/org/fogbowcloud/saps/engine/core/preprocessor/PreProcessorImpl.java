package org.fogbowcloud.saps.engine.core.preprocessor;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;

public class PreProcessorImpl implements PreProcessor {

	private ImageDataStore imageDataStore;
	
	public static final Logger LOGGER = Logger.getLogger(PreProcessorImpl.class);

	public PreProcessorImpl(Properties properties) throws SQLException {
		this.imageDataStore = new JDBCImageDataStore(properties);
	}

	@Override
	public void preProcessImage(ImageTask imageTask) {
		// TODO Auto-generated method stub

	}

	@Override
	public void exec() {
		while (true) {
			try {
				List<ImageTask> downloadedImages = this.imageDataStore.getIn(ImageTaskState.DOWNLOADED);
				for(ImageTask image : downloadedImages) {
					
				}
			} catch (SQLException e) {
				LOGGER.error("Failed in execute PreProcessor", e);
			}
		}
		// TODO Auto-generated method stub

	}

}
