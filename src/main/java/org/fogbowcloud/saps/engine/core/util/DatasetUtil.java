package org.fogbowcloud.saps.engine.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;

public class DatasetUtil {

	/**
	 * Returns a list of Satellites in operation by a given year.
	 * 
	 * Note that some Satellites (Landsat7 and Landsat8) are still in operation on
	 * this date.
	 * 
	 * @param year
	 *            - integer that represents the year.
	 * @return - a list of Satellites names in the Saps definition.
	 */
	public static ArrayList<String> getSatsInOperationByYear(int year) {

		Map<String, Integer> satYearBegin = new HashMap<String, Integer>();
		Map<String, Integer> satYearEnd = new HashMap<String, Integer>();

		satYearBegin.put(SapsPropertiesConstants.DATASET_LT5_TYPE, new Integer(1984));
		satYearEnd.put(SapsPropertiesConstants.DATASET_LT5_TYPE, new Integer(2013));

		satYearBegin.put(SapsPropertiesConstants.DATASET_LE7_TYPE, new Integer(1999));
		satYearEnd.put(SapsPropertiesConstants.DATASET_LE7_TYPE, new Integer(Integer.MAX_VALUE));

		satYearBegin.put(SapsPropertiesConstants.DATASET_LC8_TYPE, new Integer(2013));
		satYearEnd.put(SapsPropertiesConstants.DATASET_LC8_TYPE, new Integer(Integer.MAX_VALUE));

		ArrayList<String> sats = new ArrayList<String>();

		for (String sat : satYearBegin.keySet()) {
			Integer yearBegin = satYearBegin.get(sat);
			Integer yearEnd = satYearEnd.get(sat);
			if (year >= yearBegin && year <= yearEnd) {
				sats.add(sat);
			}
		}

		return sats;
	}

}