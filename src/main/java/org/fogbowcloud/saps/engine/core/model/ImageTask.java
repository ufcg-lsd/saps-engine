package org.fogbowcloud.saps.engine.core.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class ImageTask implements Serializable {

	private static final DateFormat DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd");

	public static final String AVAILABLE = "available";
	public static final String PURGED = "purged";
	public static final String NON_EXISTENT = "NE";

	private String taskId;
	private String dataset;
	private String region;
	private Date imageDate;
	private String downloadLink;
	private ImageTaskState state;
	private String federationMember;
	private int priority;
	private String stationId;
	private String inputGatheringTag;
	private String inputPreprocessingTag;
	private String algorithmExecutionTag;
	private String archiverVersion;
	private String blowoutVersion;
	private Timestamp creationTime;
	private Timestamp updateTime;
	private String status;
	private String error;

	public ImageTask(String taskId, String dataset, String region, Date imageDate,
			String downloadLink, ImageTaskState state, String federationMember, int priority,
			String stationId, String inputGatheringTag, String inputPreprocessingTag,
			String algorithmExecutionTag, String archiverVersion, String blowoutVersion,
			Timestamp creationTime, Timestamp updateTime, String status, String error) {
		this.taskId = taskId;
		this.dataset = dataset;
		this.region = region;
		this.imageDate = imageDate;
		this.downloadLink = downloadLink;
		this.state = state;
		this.federationMember = federationMember;
		this.priority = priority;
		this.stationId = stationId;
		this.inputGatheringTag = inputGatheringTag;
		this.inputPreprocessingTag = inputPreprocessingTag;
		this.algorithmExecutionTag = algorithmExecutionTag;
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

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public String getDataset() {
		return dataset;
	}

	public void setDataset(String dataset) {
		this.dataset = dataset;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public Date getImageDate() {
		return imageDate;
	}

	public void setImageDate(Date imageDate) {
		this.imageDate = imageDate;
	}

	public String getDownloadLink() {
		return downloadLink;
	}

	public void setDownloadLink(String downloadLink) {
		this.downloadLink = downloadLink;
	}

	public ImageTaskState getState() {
		return state;
	}

	public void setState(ImageTaskState state) {
		this.state = state;
	}

	public String getFederationMember() {
		return federationMember;
	}

	public void setFederationMember(String federationMember) {
		this.federationMember = federationMember;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getStationId() {
		return stationId;
	}

	public void setStationId(String stationId) {
		this.stationId = stationId;
	}

	public String getInputGatheringTag() {
		return inputGatheringTag;
	}

	public void setInputGatheringTag(String inputGatheringTag) {
		this.inputGatheringTag = inputGatheringTag;
	}

	public String getInputPreprocessingTag() {
		return inputPreprocessingTag;
	}

	public void setInputPreprocessingTag(String inputPreprocessingTag) {
		this.inputPreprocessingTag = inputPreprocessingTag;
	}

	public String getAlgorithmExecutionTag() {
		return algorithmExecutionTag;
	}

	public void setAlgorithmExecutionTag(String algorithmExecutionTag) {
		this.algorithmExecutionTag = algorithmExecutionTag;
	}

	public String getArchiverVersion() {
		return archiverVersion;
	}

	public void setArchiverVersion(String archiverVersion) {
		this.archiverVersion = archiverVersion;
	}

	public String getBlowoutVersion() {
		return blowoutVersion;
	}

	public void setBlowoutVersion(String blowoutVersion) {
		this.blowoutVersion = blowoutVersion;
	}

	public Timestamp getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Timestamp creationTime) {
		this.creationTime = creationTime;
	}

	public Timestamp getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Timestamp updateTime) {
		this.updateTime = updateTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	// TODO change to the correct format
	public String getName() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(imageDate);
		return dataset + cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR);
	}

	// TODO change to the correct format
	public String getCollectionTierName() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(imageDate);
		return dataset + "_" + cal.get(Calendar.DAY_OF_YEAR) + "_" + cal.get(Calendar.YEAR);
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();

		json.put("taskId", taskId);
		json.put("name", getName());
		json.put("collectionTierName", getCollectionTierName());
		json.put("dataset", dataset);
		json.put("region", region);
		json.put("imageDate", DATE_FORMATER.format(imageDate));
		json.put("downloadLink", downloadLink);
		json.put("state", state.getValue());
		json.put("federationMember", federationMember);
		json.put("priority", priority);
		json.put("stationId", stationId);
		json.put("inputGatheringTag", inputGatheringTag);
		json.put("inputPreprocessingTag", inputPreprocessingTag);
		json.put("algorithmExecutionTag", algorithmExecutionTag);
		json.put("archiverVersion", archiverVersion);
		json.put("blowoutVersion", blowoutVersion);
		json.put("creationTime", creationTime);
		json.put("updateTime", updateTime);
		json.put("status", status);
		json.put("error", error);

		return json;
	}

	@Override
	public String toString() {
		return "ImageTask{" + "taskId='" + taskId + '\'' + ", dataset='" + dataset + '\''
				+ ", region='" + region + '\'' + ", imageDate=" + DATE_FORMATER.format(imageDate)
				+ ", downloadLink='" + downloadLink + '\'' + ", state=" + state
				+ ", federationMember='" + federationMember + '\'' + ", priority=" + priority
				+ ", stationId='" + stationId + '\'' + ", inputGatheringTag='" + inputGatheringTag
				+ '\'' + ", inputPreprocessingTag='" + inputPreprocessingTag + '\''
				+ ", algorithmExecutionTag='" + algorithmExecutionTag + '\'' + ", archiverVersion='"
				+ archiverVersion + '\'' + ", blowoutVersion='" + blowoutVersion + '\''
				+ ", creationTime=" + creationTime + ", updateTime=" + updateTime + ", status='"
				+ status + '\'' + ", error='" + error + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ImageTask imageTask = (ImageTask) o;

		if (priority != imageTask.priority)
			return false;
		if (taskId != null ? !taskId.equals(imageTask.taskId) : imageTask.taskId != null)
			return false;
		if (dataset != null ? !dataset.equals(imageTask.dataset) : imageTask.dataset != null)
			return false;
		if (region != null ? !region.equals(imageTask.region) : imageTask.region != null)
			return false;
		if (imageDate != null) {
			if (imageTask.imageDate == null)
				return false;
			Calendar it = Calendar.getInstance();
			it.setTime(imageDate);
			Calendar oit = Calendar.getInstance();
			oit.setTime(imageTask.imageDate);
			if (it.get(Calendar.DAY_OF_YEAR) != oit.get(Calendar.DAY_OF_YEAR))
				return false;
			if (it.get(Calendar.YEAR) != oit.get(Calendar.YEAR))
				return false;
		} else {
			if (imageTask.imageDate != null)
				return false;
		}
		if (downloadLink != null ? !downloadLink.equals(imageTask.downloadLink)
				: imageTask.downloadLink != null)
			return false;
		if (state != imageTask.state)
			return false;
		if (federationMember != null ? !federationMember.equals(imageTask.federationMember)
				: imageTask.federationMember != null)
			return false;
		if (stationId != null ? !stationId.equals(imageTask.stationId)
				: imageTask.stationId != null)
			return false;
		if (inputGatheringTag != null ? !inputGatheringTag.equals(imageTask.inputGatheringTag)
				: imageTask.inputGatheringTag != null)
			return false;
		if (inputPreprocessingTag != null
				? !inputPreprocessingTag.equals(imageTask.inputPreprocessingTag)
				: imageTask.inputPreprocessingTag != null)
			return false;
		if (algorithmExecutionTag != null
				? !algorithmExecutionTag.equals(imageTask.algorithmExecutionTag)
				: imageTask.algorithmExecutionTag != null)
			return false;
		if (archiverVersion != null ? !archiverVersion.equals(imageTask.archiverVersion)
				: imageTask.archiverVersion != null)
			return false;
		if (blowoutVersion != null ? !blowoutVersion.equals(imageTask.blowoutVersion)
				: imageTask.blowoutVersion != null)
			return false;
		if (creationTime != null ? !creationTime.equals(imageTask.creationTime)
				: imageTask.creationTime != null)
			return false;
		if (updateTime != null ? !updateTime.equals(imageTask.updateTime)
				: imageTask.updateTime != null)
			return false;
		if (status != null ? !status.equals(imageTask.status) : imageTask.status != null)
			return false;
		return error != null ? error.equals(imageTask.error) : imageTask.error == null;
	}

	@Override
	public int hashCode() {
		int result = taskId != null ? taskId.hashCode() : 0;
		result = 31 * result + (dataset != null ? dataset.hashCode() : 0);
		result = 31 * result + (region != null ? region.hashCode() : 0);
		result = 31 * result + (imageDate != null ? imageDate.hashCode() : 0);
		result = 31 * result + (downloadLink != null ? downloadLink.hashCode() : 0);
		result = 31 * result + (state != null ? state.hashCode() : 0);
		result = 31 * result + (federationMember != null ? federationMember.hashCode() : 0);
		result = 31 * result + priority;
		result = 31 * result + (stationId != null ? stationId.hashCode() : 0);
		result = 31 * result + (inputGatheringTag != null ? inputGatheringTag.hashCode() : 0);
		result = 31 * result
				+ (inputPreprocessingTag != null ? inputPreprocessingTag.hashCode() : 0);
		result = 31 * result
				+ (algorithmExecutionTag != null ? algorithmExecutionTag.hashCode() : 0);
		result = 31 * result + (archiverVersion != null ? archiverVersion.hashCode() : 0);
		result = 31 * result + (blowoutVersion != null ? blowoutVersion.hashCode() : 0);
		result = 31 * result + (creationTime != null ? creationTime.hashCode() : 0);
		result = 31 * result + (updateTime != null ? updateTime.hashCode() : 0);
		result = 31 * result + (status != null ? status.hashCode() : 0);
		result = 31 * result + (error != null ? error.hashCode() : 0);
		return result;
	}
}
