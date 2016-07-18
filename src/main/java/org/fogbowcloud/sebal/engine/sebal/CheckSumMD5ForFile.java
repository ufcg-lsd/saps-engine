package org.fogbowcloud.sebal.engine.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class CheckSumMD5ForFile {
	
	public static final Logger LOGGER = Logger.getLogger(CheckSumMD5ForFile.class);

	public static boolean isFileCorrupted(ImageData imageData, File localFilesDir) {
		String localChecksum = null;
		FileInputStream fileInputStream = null;
		try {
			for (File outputFile : localFilesDir.listFiles()) {
				if(!outputFile.getName().endsWith(".md5")) {
					LOGGER.debug("Generating local checksum for file " + outputFile.getName());
					
					fileInputStream = new FileInputStream(outputFile);
					localChecksum = DigestUtils.md5Hex(IOUtils
							.toByteArray(fileInputStream));
					
					String outputFileName = outputFile.getName();
					String[] outputFileSplit = outputFileName.split("\\.");
					String outputFileInitial = outputFileSplit[0];
				
					String remoteChecksum = "";
					for (File outputMd5File : localFilesDir.listFiles()) {
						if (outputMd5File.getName().startsWith(outputFileInitial) && outputMd5File.getName().endsWith(".md5")) {
							String outputMd5FileName = outputMd5File.getName();
							String[] pieces = outputMd5FileName.split("\\.");
							remoteChecksum = pieces[2];

							LOGGER.debug("Comparing local checksum " + localChecksum + " with remote checksum " + remoteChecksum);
							if (!localChecksum.equals(remoteChecksum)) {
								throw new IOException("Some file in " + localFilesDir
										+ " is corrupted or present some error");
							}
						}
					}					
				}
			}
		} catch (IOException e) {
			LOGGER.error(e);
			return true;
		} finally {
			IOUtils.closeQuietly(fileInputStream);
		}
		
		LOGGER.info("Files are not corrupted!");
		LOGGER.info("Proceeding with the execution");		
		return false;
	}

}
