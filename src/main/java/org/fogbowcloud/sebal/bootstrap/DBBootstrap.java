package org.fogbowcloud.sebal.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.JDBCImageDataStore;
import org.fogbowcloud.sebal.NASARepository;

public class DBBootstrap {

	private JDBCImageDataStore imageStore;
	Properties properties;
	private NASARepository nasaRepository;

	private static final Logger LOGGER = Logger.getLogger(DBBootstrap.class);

	public DBBootstrap(Properties properties) {
		if (properties == null) {
			throw new IllegalArgumentException("The properties must not bu null.");
		}
		this.properties = properties;
		imageStore = new JDBCImageDataStore(properties);
		nasaRepository = new NASARepository(properties);
	}

	public void fillDB() throws ClientProtocolException, UnsupportedEncodingException, IOException {
		int firstYear = Integer.parseInt(properties.getProperty("first_year"));
		int lastYear = Integer.parseInt(properties.getProperty("last_year"));

		List<String> regions = getRegions(properties.getProperty("regions_file_path"));
		LOGGER.debug("Regions: " + regions);

		int priority = 0;
		for (String region : regions) {
			for (int year = firstYear; year <= lastYear; year++) {
				String imageList = createImageList(region, year);

//				System.out.println(imageList);
				
				File imageListFile = new File("images-" + year + ".txt");
				FileUtils.write(imageListFile, imageList);

				Map<String, String> imageAndDownloadLink = nasaRepository
						.checkExistingImages(imageListFile);
				
				imageListFile.delete();
				
				for (String imageName : imageAndDownloadLink.keySet()) {
					try {
						//TODO: See how site IP will fit here
						imageStore.addImage(imageName, imageAndDownloadLink.get(imageName), priority, "");
					} catch (SQLException e) {
						// TODO do we need to do something?
						LOGGER.error("Error while adding image at data base.", e);
					}
				}
			}
			priority++;
		}
	}

	private String createImageList(String region, int year) {
		StringBuilder imageList = new StringBuilder();
		for (int day = 1; day < 366; day++) {
			NumberFormat formatter = new DecimalFormat("000");
			String imageName = "LT5" + region + year + formatter.format(day) + "CUB00";
			imageList.append(imageName + "\n");					
		}
		return imageList.toString().trim();
	}

	private List<String> getRegions(String filePath) {
		List<String> regions = new ArrayList<String>();
		try {
			for (String line : Files.readAllLines(Paths.get(filePath), Charset.defaultCharset())) {
				regions.add(line);
			}
		} catch (IOException e) {
			LOGGER.error("Error while reading regions from file path " + filePath, e);
		}
		return regions;
	}

	public JDBCImageDataStore getImageStore() {
		return imageStore;
		
	}
	
	protected void setNasaRepository(NASARepository nasaRepository) {
		this.nasaRepository = nasaRepository;
	}

	public static String getImageRegionFromName(String imageName) {
		return imageName.substring(3, 9);
	}
}
