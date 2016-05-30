package org.fogbowcloud.sebal.bootstrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
	private NASARepository nasaRepository;
	Properties properties;

	private static final Logger LOGGER = Logger.getLogger(DBBootstrap.class);

	public DBBootstrap(Properties properties, String imageStoreIP, String imageStorePort) {
		if (properties == null) {
			throw new IllegalArgumentException("The properties must not bu null.");
		}
		this.properties = properties;
		imageStore = new JDBCImageDataStore(properties, imageStoreIP, imageStorePort);
		nasaRepository = new NASARepository(properties);
	}

	public void fillDB(String firstYear, String lastYear, String regionsFilePath)
			throws ClientProtocolException, UnsupportedEncodingException,
			IOException {
		int fYear = Integer.parseInt(firstYear);
		int lYear = Integer.parseInt(lastYear);

		List<String> regions = getRegions(regionsFilePath);
		LOGGER.debug("Regions: " + regions);

		int priority = 0;
		for (String region : regions) {
			for (int year = fYear; year <= lYear; year++) {
				String imageList = createImageList(region, year);

//				System.out.println(imageList);
				
				File imageListFile = new File("images-" + year + ".txt");
				FileUtils.write(imageListFile, imageList);

				Map<String, String> imageAndDownloadLink = getNasaRepository()
						.checkExistingImages(imageListFile);
				
				imageListFile.delete();
				
				for (String imageName : imageAndDownloadLink.keySet()) {
					try {
						getImageStore().addImage(imageName,
								imageAndDownloadLink.get(imageName), priority);
					} catch (SQLException e) {
						// TODO do we need to do something?
						LOGGER.error("Error while adding image at data base.", e);
					}
				}
			}
			priority++;
		}
	}

	protected String createImageList(String region, int year) {
		StringBuilder imageList = new StringBuilder();
		for (int day = 1; day < 366; day++) {
			NumberFormat formatter = new DecimalFormat("000");
			String imageName = "LT5" + region + year + formatter.format(day) + "CUB00";
			imageList.append(imageName + "\n");					
		}
		return imageList.toString().trim();
	}

	protected List<String> getRegions(String filePath) {
		List<String> regions = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String line;
			while ((line = br.readLine()) != null) {
				regions.add(line);
			}
			br.close();
		} catch (IOException e) {
			LOGGER.error("Error while reading regions from file path "
					+ filePath, e);
		}
		return regions;
	}

	public JDBCImageDataStore getImageStore() {
		return imageStore;
		
	}
	
	protected void setNasaRepository(NASARepository nasaRepository) {
		this.nasaRepository = nasaRepository;
	}
	
	protected NASARepository getNasaRepository() {
		return nasaRepository;
	}

	public static String getImageRegionFromName(String imageName) {
		return imageName.substring(3, 9);
	}
}
