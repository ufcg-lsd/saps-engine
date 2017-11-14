package org.fogbowcloud.saps.engine.core.util;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.interop.InteropFramework.ProvFormat;
import org.openprovenance.prov.model.Activity;
import org.openprovenance.prov.model.Agent;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.Entity;
import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.ProvFactory;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.StatementOrBundle;
import org.openprovenance.prov.model.WasAssociatedWith;
import org.openprovenance.prov.model.WasGeneratedBy;

public class ProvenanceUtilImpl implements ProvenanceUtil {

	private static final String SAPSPROV_NS = "https://github.com/fogbow/saps-engine";
	private static final String SAPSPROV_PREFIX = "sapsprov";

	private static final String SUBMISSION_DISPATCHER_AGENT = "SubmissionDispatcher";
	private static final String INPUT_DOWNLOADER_AGENT = "InputDownloader";
	private static final String INPUT_PREPROCESSOR_AGENT = "InputPreprocessor";
	private static final String WORKER_AGENT = "Worker";

	private static final String DISPATCH_ACTIVITY = "dispatch";
	private static final String DOWNLOAD_ACTIVITY = "download";
	private static final String PREPROCESS_ACTIVITY = "preprocess";
	private static final String PROCESS_ACTIVITY = "process";

	private static final String LANDSAT_TILE_QN = "LandsatTile";
	private static final String DATE_QN = "Date";
	private static final String INPUT_DOWNLOADER_URL_QN = "InputDownloaderURL";
	private static final String INPUT_PREPROCESSOR_URL_QN = "InputPreprocessorURL";
	private static final String WORKER_URL_QN = "WorkerURL";

	private static final String INPUT_METADATA_QN = "InputMetadata";
	private static final String INPUT_OPERATING_SYSTEM_QN = "InputOperatingSystem";
	private static final String INPUT_KERNEL_VERSION_QN = "InputKernelVersion";

	private static final String PRE_PROCESSING_METADATA_QN = "PreProcessingMetadata";
	private static final String PREPROCESSING_OPERATING_SYSTEM_QN = "PreprocessingOperatingSystem";
	private static final String PREPROCESSING_KERNEL_VERSION_QN = "PreProcessingKernelVersion";

	private static final String OUTPUT_METADATA_QN = "OutputMetadata";
	private static final String OUTPUT_OPERATING_SYSTEM_QN = "OutputOperatingSystem";
	private static final String OUTPUT_KERNEL_VERSION_QN = "OutputKernelVersion";

	public static final Logger LOGGER = Logger.getLogger(ProvenanceUtilImpl.class);

	private final ProvFactory provFactory;
	private final Namespace nameSpace;

	public ProvenanceUtilImpl() {
		this(InteropFramework.newXMLProvFactory());
	}

	public ProvenanceUtilImpl(ProvFactory pFactory) {
		this.provFactory = pFactory;
		nameSpace = new Namespace();
		nameSpace.addKnownNamespaces();
		nameSpace.register(SAPSPROV_PREFIX, SAPSPROV_NS);
	}

	public QualifiedName setQualifiedName(String n) {
		return nameSpace.qualifiedName(SAPSPROV_PREFIX, n, provFactory);
	}

	@Override
	public Document makeDocument(String landsatTile, String date, String downloaderURL,
			String preprocessorURL, String workerURL, String inputMetadata,
			String inputOperatingSystem, String inputKernelVersion, String preprocessingMetadata,
			String preprocessingOperatingSystem, String preprocessingKernelVersion,
			String outputMetadata, String outputOperatingSystem, String outputKernelVersion) {
		LOGGER.debug("LandsatTile: " + landsatTile + " date: " + date + "InputDownloaderURL: "
				+ downloaderURL + " InputPreprocessorURL: " + preprocessorURL + " WorkerURL: "
				+ workerURL + " InputMetadata: " + inputMetadata + " InputOS: "
				+ inputOperatingSystem + " InputKernel: " + inputKernelVersion
				+ " PreProcessorMetadata: " + preprocessingMetadata + " PreProcessorOS: "
				+ preprocessingOperatingSystem + " PreProcessorKernel: "
				+ preprocessingKernelVersion + " OutputMetadata: " + outputMetadata + " OutputOS: "
				+ outputOperatingSystem + " OutputKernel: " + outputKernelVersion);

		Agent submissionDispatcher = provFactory
				.newAgent(setQualifiedName(SUBMISSION_DISPATCHER_AGENT));
		Agent inputDownloader = provFactory.newAgent(setQualifiedName(INPUT_DOWNLOADER_AGENT));
		Agent inputPreprocessor = provFactory.newAgent(setQualifiedName(INPUT_PREPROCESSOR_AGENT));
		Agent worker = provFactory.newAgent(setQualifiedName(WORKER_AGENT));

		Activity dispatch = provFactory.newActivity(setQualifiedName(DISPATCH_ACTIVITY));
		Activity download = provFactory.newActivity(setQualifiedName(DOWNLOAD_ACTIVITY));
		Activity preprocess = provFactory.newActivity(setQualifiedName(PREPROCESS_ACTIVITY));
		Activity process = provFactory.newActivity(setQualifiedName(PROCESS_ACTIVITY));

		// Dispatcher entities
		Entity landsatTileEntity = provFactory.newEntity(setQualifiedName(LANDSAT_TILE_QN));
		landsatTileEntity
				.setValue(provFactory.newValue(landsatTile, provFactory.getName().XSD_STRING));

		Entity dateEntity = provFactory.newEntity(setQualifiedName(DATE_QN));
		dateEntity.setValue(provFactory.newValue(date, provFactory.getName().XSD_STRING));

		Entity inputDownloaderURLEntity = provFactory
				.newEntity(setQualifiedName(INPUT_DOWNLOADER_URL_QN));
		inputDownloaderURLEntity
				.setValue(provFactory.newValue(downloaderURL, provFactory.getName().XSD_STRING));

		Entity inputPreprocessorURLEntity = provFactory
				.newEntity(setQualifiedName(INPUT_PREPROCESSOR_URL_QN));
		inputPreprocessorURLEntity
				.setValue(provFactory.newValue(preprocessorURL, provFactory.getName().XSD_STRING));

		Entity workerURLEntity = provFactory.newEntity(setQualifiedName(WORKER_URL_QN));
		workerURLEntity.setValue(provFactory.newValue(workerURL, provFactory.getName().XSD_STRING));

		// Input Downloader entities
		Entity inputMetadataEntity = provFactory.newEntity(setQualifiedName(INPUT_METADATA_QN));
		inputMetadataEntity
				.setValue(provFactory.newValue(inputMetadata, provFactory.getName().XSD_STRING));

		Entity inputOperatingSystemEntity = provFactory
				.newEntity(setQualifiedName(INPUT_OPERATING_SYSTEM_QN));
		inputOperatingSystemEntity.setValue(
				provFactory.newValue(inputOperatingSystem, provFactory.getName().XSD_STRING));

		Entity inputKernelVersionEntity = provFactory
				.newEntity(setQualifiedName(INPUT_KERNEL_VERSION_QN));
		inputKernelVersionEntity.setValue(
				provFactory.newValue(inputKernelVersion, provFactory.getName().XSD_STRING));

		// Input Preprocessor entities
		Entity preprocessedMetadataEntity = provFactory
				.newEntity(setQualifiedName(PRE_PROCESSING_METADATA_QN));
		preprocessedMetadataEntity.setValue(
				provFactory.newValue(preprocessingMetadata, provFactory.getName().XSD_STRING));

		Entity preprocessingOperatingSystemEntity = provFactory
				.newEntity(setQualifiedName(PREPROCESSING_OPERATING_SYSTEM_QN));
		preprocessingOperatingSystemEntity.setValue(provFactory
				.newValue(preprocessingOperatingSystem, provFactory.getName().XSD_STRING));

		Entity preprocessingKernelVersionEntity = provFactory
				.newEntity(setQualifiedName(PREPROCESSING_KERNEL_VERSION_QN));
		preprocessingKernelVersionEntity.setValue(
				provFactory.newValue(preprocessingKernelVersion, provFactory.getName().XSD_STRING));

		// Worker entities
		Entity outputMetadataEntity = provFactory.newEntity(setQualifiedName(OUTPUT_METADATA_QN));
		outputMetadataEntity
				.setValue(provFactory.newValue(outputMetadata, provFactory.getName().XSD_STRING));

		Entity outputOperatingSystemEntity = provFactory
				.newEntity(setQualifiedName(OUTPUT_OPERATING_SYSTEM_QN));
		outputOperatingSystemEntity.setValue(
				provFactory.newValue(outputOperatingSystem, provFactory.getName().XSD_STRING));

		Entity outputKernelVersionEntity = provFactory
				.newEntity(setQualifiedName(OUTPUT_KERNEL_VERSION_QN));
		outputKernelVersionEntity.setValue(
				provFactory.newValue(outputKernelVersion, provFactory.getName().XSD_STRING));

		return generateAndAssociate(submissionDispatcher, inputDownloader, inputPreprocessor,
				worker, dispatch, download, preprocess, process, landsatTileEntity, dateEntity,
				inputDownloaderURLEntity, inputPreprocessorURLEntity, workerURLEntity,
				inputMetadataEntity, inputOperatingSystemEntity, inputKernelVersionEntity,
				preprocessedMetadataEntity, preprocessingOperatingSystemEntity,
				preprocessingKernelVersionEntity, outputMetadataEntity, outputOperatingSystemEntity,
				outputKernelVersionEntity);
	}

	private Document generateAndAssociate(Agent submissionDispatcher, Agent inputDownloader,
			Agent inputPreprocessor, Agent worker, Activity dispatch, Activity download,
			Activity preprocess, Activity process, Entity landsatTileEntity, Entity dateEntity,
			Entity inputDownloaderURLEntity, Entity inputPreprocessorURLEntity,
			Entity workerURLEntity, Entity inputMetadataEntity, Entity inputOperatingSystemEntity,
			Entity inputKernelVersionEntity, Entity preprocessedMetadataEntity,
			Entity preprocessingOperatingSystemEntity, Entity preprocessingKernelVersionEntity,
			Entity outputMetadataEntity, Entity outputOperatingSystemEntity,
			Entity outputKernelVersionEntity) {
		// Activities-Entities generation
		WasGeneratedBy dispatchGeneratedOne = provFactory.newWasGeneratedBy(landsatTileEntity, null,
				dispatch);
		WasGeneratedBy dispatchGeneratedTwo = provFactory.newWasGeneratedBy(dateEntity, null,
				dispatch);
		WasGeneratedBy dispatchGeneratedThree = provFactory
				.newWasGeneratedBy(inputDownloaderURLEntity, null, dispatch);
		WasGeneratedBy dispatchGeneratedFour = provFactory
				.newWasGeneratedBy(inputPreprocessorURLEntity, null, dispatch);
		WasGeneratedBy dispatchGeneratedFive = provFactory.newWasGeneratedBy(workerURLEntity, null,
				dispatch);
		WasGeneratedBy inputGeneratedOne = provFactory.newWasGeneratedBy(inputMetadataEntity, null,
				download);
		WasGeneratedBy inputGeneratedTwo = provFactory.newWasGeneratedBy(inputOperatingSystemEntity,
				null, download);
		WasGeneratedBy inputGeneratedThree = provFactory.newWasGeneratedBy(inputKernelVersionEntity,
				null, download);
		WasGeneratedBy preprocessorGeneratedOne = provFactory
				.newWasGeneratedBy(preprocessedMetadataEntity, null, preprocess);
		WasGeneratedBy preprocessorGeneratedTwo = provFactory
				.newWasGeneratedBy(preprocessingOperatingSystemEntity, null, preprocess);
		WasGeneratedBy preprocessorGeneratedThree = provFactory
				.newWasGeneratedBy(preprocessingKernelVersionEntity, null, preprocess);
		WasGeneratedBy workerGeneratedOne = provFactory.newWasGeneratedBy(outputMetadataEntity,
				null, process);
		WasGeneratedBy workerGeneratedTwo = provFactory
				.newWasGeneratedBy(outputOperatingSystemEntity, null, process);
		WasGeneratedBy workerGeneratedThree = provFactory
				.newWasGeneratedBy(outputKernelVersionEntity, null, process);

		// Agents-Activities associations
		WasAssociatedWith associatedOne = provFactory.newWasAssociatedWith(
				setQualifiedName("associatedOne"), dispatch.getId(), submissionDispatcher.getId());
		WasAssociatedWith associatedTwo = provFactory.newWasAssociatedWith(
				setQualifiedName("associatedTwo"), download.getId(), inputDownloader.getId());
		WasAssociatedWith associatedThree = provFactory.newWasAssociatedWith(
				setQualifiedName("associatedThree"), preprocess.getId(), inputPreprocessor.getId());
		WasAssociatedWith associatedFour = provFactory.newWasAssociatedWith(
				setQualifiedName("associatedFour"), process.getId(), worker.getId());

		Document document = provFactory.newDocument();
		document.getStatementOrBundle().addAll(Arrays.asList(new StatementOrBundle[] {
				submissionDispatcher, inputDownloader, inputPreprocessor, worker, dispatch,
				download, preprocess, process, landsatTileEntity, dateEntity,
				inputDownloaderURLEntity, inputPreprocessorURLEntity, workerURLEntity,
				inputMetadataEntity, inputOperatingSystemEntity, inputKernelVersionEntity,
				preprocessedMetadataEntity, preprocessingOperatingSystemEntity,
				preprocessingKernelVersionEntity, outputMetadataEntity, outputOperatingSystemEntity,
				outputKernelVersionEntity, dispatchGeneratedOne, dispatchGeneratedTwo,
				dispatchGeneratedThree, dispatchGeneratedFour, dispatchGeneratedFive,
				inputGeneratedOne, inputGeneratedTwo, inputGeneratedThree, preprocessorGeneratedOne,
				preprocessorGeneratedTwo, preprocessorGeneratedThree, workerGeneratedOne,
				workerGeneratedTwo, workerGeneratedThree, associatedOne, associatedTwo,
				associatedThree, associatedFour }));
		document.setNamespace(nameSpace);

		return document;
	}

	@Override
	public void writePROVNProvenanceFile(Document document, String filePath) {
		InteropFramework intF = new InteropFramework();
		intF.writeDocument(filePath, document);
		intF.writeDocument(System.out, ProvFormat.PROVN, document);
	}
}
