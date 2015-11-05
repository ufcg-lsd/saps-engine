package org.fogbowcloud.scheduler.restlet.resource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map.Entry;

import org.fogbowcloud.scheduler.core.model.Job.TaskState;
import org.fogbowcloud.scheduler.core.model.Task;
import org.fogbowcloud.scheduler.core.model.TaskImpl;
import org.fogbowcloud.scheduler.restlet.SebalScheduleApplication;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.representation.ObjectRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class TaskResource  extends ServerResource {

	@Get
	public Representation getEvents() throws Exception{
		
		String taskId = (String) getRequest().getAttributes().get("taskId");
		String var = (String) getRequest().getAttributes().get("var");
		
		
		if (taskId != null) {

			Task task = ((SebalScheduleApplication) getApplication()).getTaskById(taskId);

			if (var != null) {
				//TODO validate if task is completed and has image related to var
				//Mount fileName.
				String fileName = "";
				return this.getImagemAsRepresentation(fileName);
			}

			JSONObject jsonTask = new JSONObject();
			jsonTask.put("taskId", task.getId());
			jsonTask.put("resultingFile", task.getMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER));
			StringRepresentation sr = new StringRepresentation(jsonTask.toString(), MediaType.TEXT_PLAIN);
			return sr;
		} else {
			List<Task> tasks = ((SebalScheduleApplication) getApplication()).getAllCompletedTasks();
			JSONArray jasonTasks = new JSONArray();
			for (Task e : tasks) {
				JSONObject jsonTask = new JSONObject();
				jsonTask.put("taskId", e.getId());
				jsonTask.put("resultingFile", e.getMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER));
				jasonTasks.put(jsonTask);
			}

			StringRepresentation sr = new StringRepresentation(jasonTasks.toString(), MediaType.TEXT_PLAIN);
			return sr;
		}
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
