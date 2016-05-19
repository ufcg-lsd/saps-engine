package org.fogbowcloud.sebal;

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
					fileInputStream = new FileInputStream(outputFile);
					localChecksum = DigestUtils.md5Hex(IOUtils
							.toByteArray(fileInputStream));
				
					String remoteChecksum = "";
					for (File outputMd5File : localFilesDir.listFiles()) {
						if (outputMd5File.getName().startsWith(outputFile.getName()) && outputMd5File.getName().endsWith(".md5")) {
							String[] pieces = outputMd5File.getName().split(".");
							remoteChecksum = pieces[2];
						}
					}
					
					if (!localChecksum.equals(remoteChecksum)) {
						throw new IOException("Some file in " + localFilesDir
								+ " is corrupted or present some error.");
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error(e);
			return true;
		} finally {
			IOUtils.closeQuietly(fileInputStream);
		}
		
		return false;
	}

}
