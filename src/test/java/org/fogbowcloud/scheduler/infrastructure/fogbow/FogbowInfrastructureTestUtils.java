package org.fogbowcloud.scheduler.infrastructure.fogbow;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

public class FogbowInfrastructureTestUtils {

	public static final String MEN_SIZE_TAG = "${MEN_SIZE}";
	public static final String CORE_SIZE_TAG = "${CORE}";
	public static final String DISK_SIZE_TAG = "${DISK_SIZE}";
	public static final String LOCATION_TAG = "${LOCATION}";
	public static final String INSTANCE_TAG = "${INSTANCE_ID}";
	public static final String HOST_TAG = "${HOST}";
	public static final String PORT_TAG = "${PORT}";
	public static final String REQUEST_ID_TAG = "${REQUEST_ID}";
	public static final String STATE_TAG = "${STATE}";
	
	
	public static String createHttpWrapperResponseFromFile(String filePath, Map<String, String> params) throws FileNotFoundException, IOException{

		FileInputStream fis = null;
		try{

			fis = new FileInputStream(filePath);

			String response = IOUtils.toString(fis);

			for(Entry<String, String> param : params.entrySet()){
				response = response.replace(param.getKey(), param.getValue());
			}

			return response;
		}finally{
			fis.close();
		}
	}
	
	
}
