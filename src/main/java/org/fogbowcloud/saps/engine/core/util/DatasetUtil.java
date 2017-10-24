package org.fogbowcloud.saps.engine.core.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;

public class DatasetUtil {

	public static Map<String, Entry<Integer, Integer>> getSatsYearsOfOperation() {
		Map<String, Entry<Integer, Integer>> satsYears = new HashMap<String, Entry<Integer, Integer>>();

		int currentYear = getCurrentYear();

		satsYears.put(SapsPropertiesConstants.DATASET_LT5_TYPE,
				new SimpleEntry<>(new Integer(1984), new Integer(2013)));
		satsYears.put(SapsPropertiesConstants.DATASET_LE7_TYPE,
				new SimpleEntry<>(new Integer(1999), new Integer(currentYear)));
		satsYears.put(SapsPropertiesConstants.DATASET_LC8_TYPE,
				new SimpleEntry<>(new Integer(2013), new Integer(currentYear)));

		return satsYears;
	}

	public static List<String> getSatsInOperationByYear(int year) {
		
		List<String> sats = new ArrayList<String>();
		Map<String, Entry<Integer, Integer>> satsYears = getSatsYearsOfOperation();
		
		for(String sat : satsYears.keySet()) {
			Entry<Integer, Integer> years = satsYears.get(sat);
			if(year >= years.getKey() && year <= years.getValue()) {
				sats.add(sat);
			}
		}
		
		return sats;
	}

	public static int getCurrentYear() {
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(new Date());
		return calendar.get(Calendar.YEAR);
	}

}
