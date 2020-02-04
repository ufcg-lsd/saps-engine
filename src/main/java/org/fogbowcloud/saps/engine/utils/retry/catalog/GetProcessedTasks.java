package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.fogbowcloud.saps.engine.core.database.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

public class GetProcessedTasks implements CatalogRetry<List<SapsImage>> {

	private Catalog imageStore;
	private String region;
	private Date initDate;
	private Date endDate;
	private String inputdownloadingPhaseTag;
	private String preprocessingPhaseTag;
	private String processingPhaseTag;

	public GetProcessedTasks(Catalog imageStore, String region, Date initDate, Date endDate,
			String inputdownloadingPhaseTag, String preprocessingPhaseTag, String processingPhaseTag) {
		this.imageStore = imageStore;
		this.region = region;
		this.initDate = initDate;
		this.endDate = endDate;
		this.inputdownloadingPhaseTag = inputdownloadingPhaseTag;
		this.preprocessingPhaseTag = preprocessingPhaseTag;
		this.processingPhaseTag = processingPhaseTag;
	}

	@Override
	public List<SapsImage> run() throws SQLException {
		return imageStore.getSuccessfullyProcessedTasks(region, initDate, endDate, inputdownloadingPhaseTag, preprocessingPhaseTag,
				processingPhaseTag);
	}

}
