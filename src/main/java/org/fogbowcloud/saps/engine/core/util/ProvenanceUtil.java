package org.fogbowcloud.saps.engine.core.util;

import org.openprovenance.prov.model.Document;

public interface ProvenanceUtil {
	
	Document makeDocument(String landsatTile, String date, String downloaderURL,
			String preprocessorURL, String workerURL, String inputMetadata,
			String inputOperatingSystem, String inputKernelVersion, String preprocessingMetadata,
			String preprocessingOperatingSystem, String preprocessingKernelVersion,
			String outputMetadata, String outputOperatingSystem, String outputKernelVersion);

	void writePROVNProvenanceFile(Document document, String filePath);
}
