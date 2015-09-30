package org.fogbowcloud.scheduler.core.util;

public class AppUtil {

	public static boolean isStringEmpty(String ... values){
		
		for(String s : values){
			if(s == null || s.isEmpty()){
				return true;
			}
		}
		return false;
	}	

}
