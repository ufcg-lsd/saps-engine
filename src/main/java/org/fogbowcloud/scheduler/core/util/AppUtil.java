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
	
	public static Object instantiateClass(String className) throws Exception{
		
		Class clazz = Class.forName(className);
		return clazz.newInstance();
		
	}
}
