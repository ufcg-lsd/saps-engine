package org.fogbowcloud.saps.engine.core.model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class ImageTask implements Serializable {

	private String taskId;
	private String name;
	private String collectionTierName;
	private String dataSet;
	private String region;
	private Date imageDate;
	private String downloadLink;
	private ImageTaskState state;
	private String federationMember;
	private int priority;
	private String stationId;
	private String downloaderContainerRepository;
	private String downloaderContainerTag;
	private String preProcessorContainerRepository;
	private String preProcessorContainerTag;
	private String workerContainerRepository;
	private String workerContainerTag;
	private String archiverVersion;
	private String blowoutVersion;
	private Timestamp creationTime;
	private Timestamp updateTime;
	private String status;
	private String error;

	private Map<String, Integer> tasksStatesCount = new HashMap<String, Integer>();

	public static final String AVAILABLE = "available";
	public static final String PURGED = "purged";

	public static final String NON_EXISTENT = "NE";

	public ImageTask(String taskId, String dataSet, String region, Date imageDate,
			String downloadLink, ImageTaskState state, String federationMember, int priority,
			String stationId, String downloaderContainerRepository, String downloaderContainerTag,
			String preProcessorContainerRepository, String preProcessorContainerTag,
			String workerContainerRepository, String workerContainerTag, String archiverVersion,
			String blowoutVersion, Timestamp creationTime, Timestamp updateTime, String status,
			String error) {
		this.taskId = taskId;
		this.dataSet = dataSet;
		this.region = region;
		this.imageDate = imageDate;
		this.downloadLink = downloadLink;
		this.state = state;
		this.federationMember = federationMember;
		this.priority = priority;
		this.stationId = stationId;
		this.downloaderContainerRepository = downloaderContainerRepository;
		this.downloaderContainerTag = downloaderContainerTag;
		this.preProcessorContainerRepository = preProcessorContainerRepository;
		this.preProcessorContainerTag = preProcessorContainerTag;
		this.workerContainerRepository = workerContainerRepository;
		this.workerContainerTag = workerContainerTag;
		this.archiverVersion = archiverVersion;
		this.blowoutVersion = blowoutVersion;
		this.creationTime = creationTime;
		this.updateTime = updateTime;
		this.status = status;
		this.error = error;
	}

	public String getTaskId() {
		return taskId;
	}

	public String getName() {
		return name;
	}

	public String getCollectionTierName() {
		return collectionTierName;
	}

	public String getDataSet() {
		return dataSet;
	}

	public String getRegion() {
		return region;
	}

	public Date getImageDate() {
		return imageDate;
	}

	public String getDownloadLink() {
		return downloadLink;
	}

	public ImageTaskState getState() {
		return state;
	}

	public String getFederationMember() {
		return federationMember;
	}

	public int getPriority() {
		return priority;
	}

	public String getStationId() {
		return stationId;
	}

	public String getDownloaderContainerRepository() {
		return downloaderContainerRepository;
	}

	public String getDownloaderContainerTag() {
		return downloaderContainerTag;
	}

	public String getPreProcessorContainerRepository() {
		return preProcessorContainerRepository;
	}

	public String getPreProcessorContainerTag() {
		return preProcessorContainerTag;
	}

	public String getWorkerContainerRepository() {
		return workerContainerRepository;
	}

	public String getWorkerContainerTag() {
		return workerContainerTag;
	}

	public String getArchiverVersion() {
		return archiverVersion;
	}

	public String getBlowoutVersion() {
		return blowoutVersion;
	}

	public Timestamp getCreationTime() {
		return creationTime;
	}

	public Timestamp getUpdateTime() {
		return updateTime;
	}

	public String getImageStatus() {
		return status;
	}

	public String getImageError() {
		return error;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCollectionTierName(String collectionTierName) {
		this.collectionTierName = collectionTierName;
	}

	public void setDataSet(String dataSet) {
		this.dataSet = dataSet;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public void setImageDate(Date imageDate) {
		this.imageDate = imageDate;
	}

	public void setDownloadLink(String downloadLink) {
		this.downloadLink = downloadLink;
	}

	public void setState(ImageTaskState state) {
		this.state = state;
	}

	public void setFederationMember(String federationMember) {
		this.federationMember = federationMember;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public void setStationId(String stationId) {
		this.stationId = stationId;
	}

	public void setDownloaderContainerRepository(String downloaderContainerRepository) {
		this.downloaderContainerRepository = downloaderContainerRepository;
	}

	public void setDownloaderContainerTag(String downloaderContainerTag) {
		this.downloaderContainerTag = downloaderContainerTag;
	}

	public void setPreProcessorContainerRepository(String preProcessorContainerRepository) {
		this.preProcessorContainerRepository = preProcessorContainerRepository;
	}

	public void setPreProcessorContainerTag(String preProcessorContainerTag) {
		this.preProcessorContainerTag = preProcessorContainerTag;
	}

	public void setWorkerContainerRepository(String workerContainerRepository) {
		this.workerContainerRepository = workerContainerRepository;
	}

	public void setWorkerContainerTag(String workerContainerTag) {
		this.workerContainerTag = workerContainerTag;
	}

	public void setArchiverVersion(String archiverVersion) {
		this.archiverVersion = archiverVersion;
	}

	public void setBlowoutVersion(String blowoutVersion) {
		this.blowoutVersion = blowoutVersion;
	}

	public void setCreationTime(Timestamp creationTime) {
		this.creationTime = creationTime;
	}

	public void setUpdateTime(Timestamp updateTime) {
		this.updateTime = updateTime;
	}

	public void setImageStatus(String status) {
		this.status = status;
	}

	public void setImageError(String error) {
		this.error = error;
	}

	public String toString() {
		return "[" + taskId + ", " + dataSet + ", " + region + ", " + imageDate.toString() + ", "
				+ downloadLink + ", " + state.getValue() + ", " + federationMember + ", " + priority
				+ ", " + stationId + ", " + downloaderContainerRepository + ", "
				+ downloaderContainerTag + ", " + preProcessorContainerRepository + ", "
				+ preProcessorContainerTag + ", " + workerContainerRepository + ", "
				+ workerContainerTag + ", " + ", " + archiverVersion + ", " + blowoutVersion + ", "
				+ creationTime + ", " + updateTime + ", " + status + ", " + error + "]";
	}

	public String formatedToString() {
		return "[ TaskId = " + taskId + " ]\n" + "[ DataSet = " + dataSet + " ]\n" + "[ Region = "
				+ region + " ]\n" + "[ ImageDate = " + imageDate.toString() + " ]\n"
				+ "[ DownloadLink = " + downloadLink + " ]\n" + "[ ImageState = " + state.getValue()
				+ " ]\n" + "[ FederationMember = " + federationMember + " ]\n" + "[ Priority = "
				+ priority + " ]\n" + "[ StationId = " + stationId + " ]\n"
				+ "[ DownloaderContainerRepository = " + downloaderContainerRepository + " ]\n"
				+ "[ DownloaderContainerTag = " + downloaderContainerTag + " ]\n"
				+ "[ PreProcessorContainerRepository = " + preProcessorContainerRepository + " ]\n"
				+ "[ PreProcessorContainerTag = " + preProcessorContainerTag + " ]\n"
				+ "[ WorkerContainerRepository = " + workerContainerRepository + " ]\n"
				+ "[ WorkerContainerTag = " + workerContainerTag + " ]\n" + "[ ArchiverVersion = "
				+ archiverVersion + " ]\n" + "[ BlowoutVersion = " + blowoutVersion + " ]\n"
				+ " ]\n" + "[ CreationTime = " + creationTime + " ]\n" + "[ UpdateTime = "
				+ updateTime + " ]\n" + "[ Status = " + status + " ]\n" + "[ Error = " + error
				+ " ]";
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();

		json.put("taskId", taskId);
		json.put("dataSet", dataSet);
		json.put("region", region);
		json.put("imageDate", imageDate.toString());
		json.put("downloadLink", downloadLink);
		json.put("state", state.getValue());
		json.put("federationMember", federationMember);
		json.put("priority", priority);
		json.put("stationId", stationId);
		json.put("downloaderContainerRepository", downloaderContainerRepository);
		json.put("downloaderContainerTag", downloaderContainerTag);
		json.put("preProcessorContainerRepository", preProcessorContainerRepository);
		json.put("preProcessorContainerTag", preProcessorContainerTag);
		json.put("workerContainerRepository", workerContainerRepository);
		json.put("workerContainerTag", workerContainerTag);
		json.put("archiverVersion", archiverVersion);
		json.put("blowoutVersion", blowoutVersion);
		json.put("creationTime", creationTime);
		json.put("updateTime", updateTime);
		json.put("status", status);
		json.put("error", error);

		return json;
	}

	public Map<String, Integer> getTasksStatesCount() {
		return tasksStatesCount;
	}

	public void setTasksStatesCount(Map<String, Integer> tasksStatesCount) {
		this.tasksStatesCount = tasksStatesCount;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ImageTask) {
			ImageTask other = (ImageTask) o;
			return getTaskId().equals(other.getTaskId())
					&& getDownloadLink().equals(other.getDownloadLink())
					&& getState().equals(other.getState()) && getPriority() == other.getPriority()
					&& getFederationMember().equals(other.getFederationMember())
					&& getStationId().equals(other.getStationId())
					&& getDownloaderContainerRepository()
							.equals(other.getDownloaderContainerRepository())
					&& getDownloaderContainerTag().equals(other.getDownloaderContainerTag())
					&& getPreProcessorContainerRepository()
							.equals(other.getPreProcessorContainerRepository())
					&& getPreProcessorContainerTag().equals(other.getPreProcessorContainerTag())
					&& getWorkerContainerRepository().equals(other.getWorkerContainerRepository())
					&& getWorkerContainerTag().equals(other.getWorkerContainerTag())
					&& getArchiverVersion().equals(other.getArchiverVersion())
					&& getBlowoutVersion().equals(other.getBlowoutVersion())
					&& getCreationTime().equals(other.getCreationTime())
					&& getUpdateTime().equals(other.getUpdateTime())
					&& getImageStatus().equals(other.getImageStatus())
					&& getImageError().equals(other.getImageError());
		}
		return false;
	}

}
