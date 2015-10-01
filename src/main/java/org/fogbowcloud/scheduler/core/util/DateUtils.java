package org.fogbowcloud.scheduler.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {
	
	public static final String DATE_FORMAT_YYYY_MM_DD_HOUR = "yyyy-MM-dd HH:mm:ss"; 
	
	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	public static Date getDateFromFormat(String value, String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			return dateFormat.parse(value);
		} catch (Exception e) {
			return null;
		}
	}

	public static String getStringDateFromMiliFormat(long dateMili, String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		String expirationDate = dateFormat.format(new Date(dateMili));
		return expirationDate;
	}

}
