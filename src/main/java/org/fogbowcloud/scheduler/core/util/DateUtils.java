package org.fogbowcloud.scheduler.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
	
	public static final String DATE_FORMAT_YYYY_MM_DD_HOUR = "yyyy-MM-dd HH:mm:ss"; 
	
	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	public static Date getDateFromFormat(String value, String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		try {
			return dateFormat.parse(value);
		} catch (Exception e) {
			return null;
		}
	}

	public static String getStringDateFromMiliFormat(long dateMili, String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		String expirationDate = dateFormat.format(new Date(dateMili));
		return expirationDate;
	}
	
	public static String formatElapsedTime(long millis){
		
		long ms = millis % 1000;
		long second = (millis / 1000) % 60;
		long minute = (millis / (1000 * 60)) % 60;
		long hour = (millis / (1000 * 60 * 60)) % 24;
		long days = (millis / (1000 * 60 * 60 * 24));
		
		return String.format("%02d Days %02d Hours %02d Minutes %02d Seconds %04d Ms", days, hour, minute, second, ms);
	}

}
