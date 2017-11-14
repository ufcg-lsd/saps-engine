package org.fogbowcloud.saps.engine.core.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.openprovenance.prov.model.Document;

public class ProvenanceUtilImplTest {

	@Test
	public void testMakeDocumentNotNull() {
		String landsatTile = "fake-landsat-tile";
		String date = "fake-date";
		String downloaderURL = "fake-downloader-url";
		String preprocessorURL = "fake-preprocessor-url";
		String workerURL = "fake-worker-url";
		String inputMetadata = "fake-input-metadata";
		String inputOperatingSystem = "fake-input-OS";
		String inputKernelVersion = "fake-input-kernel-version";
		String preprocessingMetadata = "fake-preprocessing-metadata";
		String preprocessingOperatingSystem = "fake-preprocessing-OS";
		String preprocessingKernelVersion = "fake-preprocessing-kernel-version";
		String outputMetadata = "fake-output-metadata";
		String outputOperatingSystem = "fake-output-OS";
		String outputKernelVersion = "fake-output-kernel-version";

		ProvenanceUtilImpl provUtilImpl = new ProvenanceUtilImpl();

		Document document = provUtilImpl.makeDocument(landsatTile, date, downloaderURL,
				preprocessorURL, workerURL, inputMetadata, inputOperatingSystem, inputKernelVersion,
				preprocessingMetadata, preprocessingOperatingSystem, preprocessingKernelVersion,
				outputMetadata, outputOperatingSystem, outputKernelVersion);

		Assert.assertNotNull(document);
	}

	@Test
	public void testWriteProvFileExists() throws IOException {
		String landsatTile = "fake-landsat-tile";
		String date = "fake-date";
		String downloaderURL = "fake-downloader-url";
		String preprocessorURL = "fake-preprocessor-url";
		String workerURL = "fake-worker-url";
		String inputMetadata = "fake-input-metadata";
		String inputOperatingSystem = "fake-input-OS";
		String inputKernelVersion = "fake-input-kernel-version";
		String preprocessingMetadata = "fake-preprocessing-metadata";
		String preprocessingOperatingSystem = "fake-preprocessing-OS";
		String preprocessingKernelVersion = "fake-preprocessing-kernel-version";
		String outputMetadata = "fake-output-metadata";
		String outputOperatingSystem = "fake-output-OS";
		String outputKernelVersion = "fake-output-kernel-version";

		ProvenanceUtilImpl provUtilImpl = new ProvenanceUtilImpl();

		Document document = provUtilImpl.makeDocument(landsatTile, date, downloaderURL,
				preprocessorURL, workerURL, inputMetadata, inputOperatingSystem, inputKernelVersion,
				preprocessingMetadata, preprocessingOperatingSystem, preprocessingKernelVersion,
				outputMetadata, outputOperatingSystem, outputKernelVersion);

		String outputFile = "/tmp/prov-test-file.provn";
		provUtilImpl.writePROVNProvenanceFile(document, outputFile);

		BufferedReader br = new BufferedReader(new FileReader(outputFile));

		Assert.assertNotNull(br.readLine());

		br.close();
	}
}
