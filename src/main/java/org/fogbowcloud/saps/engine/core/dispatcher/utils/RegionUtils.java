package org.fogbowcloud.saps.engine.core.dispatcher.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

public class RegionUtils {

	private static final Logger LOGGER = Logger.getLogger(RegionUtils.class);

	/**
	 * This function calling get_wrs (Python script) passing latitude and longitude
	 * as paramater.
	 * 
	 * @param latitude  latitude (point coordinate)
	 * @param longitude longitude (point coordinate)
	 * @return scene path/row from latitude and longitude coordinates
	 * @throws Exception
	 */
	public static String getRegionIds(String latitude, String longitude) throws Exception {

		LOGGER.debug("Calling get_wrs.py and passing (" + latitude + ", " + longitude + ") as parameter");
		Process builder = new ProcessBuilder("python", "./scripts/get_wrs.py", latitude, longitude).start();

		LOGGER.debug("Waiting for the process for execute command [" + builder.toString() + "] ...");
		builder.waitFor();
		LOGGER.debug("Process ended.");

		String result = null;

		if (builder.exitValue() != 0)
			throw new Exception("Process output exit code: " + builder.exitValue());

		BufferedReader reader = new BufferedReader(new InputStreamReader(builder.getInputStream()));
		StringBuilder builderS = new StringBuilder();

		String line;
		while ((line = reader.readLine()) != null) {
			builderS.append(line);
			builderS.append(System.getProperty("line.separator"));
		}

		result = builderS.toString();

		LOGGER.debug("Process output (regions ID's): \n" + result);

		return result;
	}

	/**
	 * This function returns regions set from area.
	 * 
	 * @param lowerLeftLatitude   lower left latitude (coordinate)
	 * @param lowerLeftLongitude  lower left longitude (coordinate)
	 * @param upperRightLatitude  upper right latitude (coordinate)
	 * @param upperRightLongitude upper right longitude (coordinate)
	 * @return string set (regions set)
	 */
	public static Set<String> regionsFromArea(String lowerLeftLatitude, String lowerLeftLongitude, String upperRightLatitude,
			String upperRightLongitude) {

		String regionLowerLeft, regionUpperRight;
		Set<String> regionsFound = new HashSet<String>();

		try {
			regionLowerLeft = RegionUtils.getRegionIds(lowerLeftLatitude, lowerLeftLongitude).trim();
			regionUpperRight = RegionUtils.getRegionIds(upperRightLatitude, upperRightLongitude).trim();

			int pathRegionLL = Integer.parseInt(regionLowerLeft.substring(0, 3));
			int rowRegionLL = Integer.parseInt(regionLowerLeft.substring(4, 6));

			int pathRegionUR = Integer.parseInt(regionUpperRight.substring(0, 3));
			int rowRegionUR = Integer.parseInt(regionUpperRight.substring(4, 6));

			LOGGER.info("pathRegionLL: " + pathRegionLL + "\n" + "rowRegionLL: " + rowRegionLL + "\n" + "pathRegionUR: "
					+ pathRegionUR + "\n" + "rowRegionUR: " + rowRegionUR + "\n");

			for (int i = pathRegionLL; i >= pathRegionUR; i--) {
				for (int j = rowRegionLL; j >= rowRegionUR; j--)
					regionsFound.add(formatPathRow(i, j));
			}

			LOGGER.info("Regions found: " + regionsFound.toString());

		} catch (Exception e) {
			LOGGER.error("Error while calling the ConvertToWRS script", e);
		}

		return regionsFound;
	}

	/**
	 * Get path and row and create format PPPRRR, where PPP is path of scene RRR is
	 * row of scene
	 */
	private String formatPathRow(int path, int row) {
		String pathAux = Integer.toString(path);
		String rowAux = Integer.toString(row);
		if (rowAux.length() == 1)
			rowAux = "00" + rowAux;
		else if (rowAux.length() == 2)
			rowAux = "0" + rowAux;

		return pathAux + rowAux;
	}
}
