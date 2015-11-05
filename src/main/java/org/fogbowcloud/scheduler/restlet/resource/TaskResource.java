package org.fogbowcloud.scheduler.restlet.resource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.model.TaskImpl;
import org.fogbowcloud.scheduler.restlet.SebalScheduleApplication;
import org.fogbowcloud.sebal.SebalTasks;
import org.restlet.data.MediaType;
import org.restlet.representation.ObjectRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.amazonaws.util.json.JSONObject;

public class TaskResource  extends ServerResource {

	List<String> validVariables = new ArrayList<String>();
	


	@Get
	public Representation getEvents() throws Exception{
		fillValidVariables();
		String taskId = (String) getRequest().getAttributes().get("taskId");
		String var = (String) getRequest().getAttributes().get("var");

		if (taskId != null){
			Task task = ((SebalScheduleApplication) getApplication()).getTaskById(taskId);
			JSONObject jsonTask = new JSONObject();

			String preffix = task.getMetadata(SebalTasks.METADATA_LEFT_X) + "." + 
					task.getMetadata(SebalTasks.METADATA_UPPER_Y) + "." +
					task.getMetadata(SebalTasks.METADATA_RIGHT_X) + "." +
					task.getMetadata(SebalTasks.METADATA_LOWER_Y) + "_" +
					task.getMetadata(SebalTasks.METADATA_NUMBER_OF_PARTITIONS) + "_" +
					task.getMetadata(SebalTasks.METADATA_PARTITION_INDEX) + "_new_"
					;
			Map<String, String> varPathMap = filelist(task.getMetadata(SebalTasks.METADATA_RESULTS_MOUNT_POINT) +"/results/" + task.getMetadata(SebalTasks.METADATA_IMAGE_NAME), preffix, validVariables);
			
			for (String key : varPathMap.keySet()){
				jsonTask.put(key, varPathMap.get(key));
			}
		
			StringRepresentation sr = new StringRepresentation(jsonTask.toString(), MediaType.TEXT_PLAIN);
			return sr;
		}
		return null;
	}

	private void fillValidVariables() {
		validVariables.add("ndvi");
		validVariables.add("evi");
		validVariables.add("iaf");
		validVariables.add("ts");
		validVariables.add("alpha");
		validVariables.add("rn");
		validVariables.add("g");
	}

	public Map<String, String> filelist(String parentFolder, String prefix, List<String> validSuffixes){

		Map<String, String> filePathMap = new HashMap<String, String>();

		File parentFolderFile = new File(parentFolder);
		File[] listOfFiles = parentFolderFile.listFiles();

		for (File file : listOfFiles)
		{
			if (file.isFile())
			{
				for (String variable : validSuffixes){
					if ( file.getName().startsWith(prefix+"_"+ variable)) {
						filePathMap.put(variable, file.getAbsolutePath());
					};
				}
			}
		}
		return null;
	}

	private Representation getImagemAsRepresentation(String fileName) throws FileNotFoundException{

		File file = new File(fileName);

		FileInputStream fis = new FileInputStream(file);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		try {
			for (int readNum; (readNum = fis.read(buf)) != -1;) {
				// Writes to this byte array output stream
				bos.write(buf, 0, readNum);
			}
		} catch (IOException ex) {
		}

		byte[] data = bos.toByteArray();


		ObjectRepresentation<byte[]> or=new ObjectRepresentation<byte[]>(data, MediaType.IMAGE_BMP) {
			@Override
			public void write(OutputStream os) throws IOException {
				super.write(os);
				os.write(this.getObject());
			}
		};

		return or; 

	}
}
