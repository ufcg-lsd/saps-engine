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

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.fogbowcloud.scheduler.core.model.TaskImpl;
import org.restlet.data.MediaType;
import org.restlet.representation.ObjectRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class TaskResource  extends ServerResource {

	List<String> validVariables = new ArrayList<String>();
	
	
	
	@Get
	public Representation getEvents() throws Exception{

		String taskId = (String) getRequest().getAttributes().get("taskId");
		
		if (taskId != null){
			Task task = ((SebalScheduleApplication) getApplication()).getTaskById(taskId);
			JSONObject jsonTask = new JSONObject();

			String preffix = task.getMetadata(SebalTasks.)
			
			jsonTask.put("resultingFile", task.getMetadata(TaskImpl.METADATA_LOCAL_OUTPUT_FOLDER));
			StringRepresentation sr = new StringRepresentation(jsonTask.toString(), MediaType.TEXT_PLAIN);
			ProcessBuilder builder = new ProcessBuilder(LOCAL_COMMAND_INTERPRETER, "-c",
					"commandodemontagem");
			return sr;
		}
		return null;
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

	@GET
	@Produces("application/bitmap")
	public Response getImageBitMap(String fileName)
	{
		System.out.println("File requested is : " + fileName);

		//Put some validations here such as invalid file name or missing file name
		if(fileName == null || fileName.isEmpty())
		{
			ResponseBuilder response = Response.status(Status.BAD_REQUEST);
			return response.build();
		}

		//Prepare a file object with file to return
		File file = new File("c:/demoPDFFile.pdf");

		ResponseBuilder response = Response.ok((Object) file);
		response.header("Content-Disposition", "attachment; filename="+fileName);
		return response.build();
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
