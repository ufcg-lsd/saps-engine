package org.fogbowcloud.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class CheckSumMD5ForFile {
	
	public static final Logger LOGGER = Logger.getLogger(CheckSumMD5ForFile.class);

	// FIXME: see if this will be really with a tar.gz of results or with each file
	public static boolean isFileCorrupted(ImageData imageData, File filesDir) {
		String localCheckSum = null;
		FileInputStream fileInputStream = null;
		try {
			String checkSumPath = filesDir + imageData.getName() + "_checksum.md5";
			FileInputStream remoteCheckSumInputStream = new FileInputStream(new File(checkSumPath));
			String remoteCheckSum = DigestUtils.md2Hex(remoteCheckSumInputStream);
			
			String compactedImagesPath = filesDir + "/" + imageData.getName() + "_results.tar.gz";
			fileInputStream = new FileInputStream(new File(compactedImagesPath));
			localCheckSum = DigestUtils.md5Hex(IOUtils
					.toByteArray(fileInputStream));
			if (!localCheckSum.equals(remoteCheckSum)) {
				throw new IOException("Some file in " + filesDir
						+ " is corrupted or present some error.");
			}
			// md5Hex converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
			// The returned array will be double the length of the passed array, as it takes two characters to represent any given byte.
		} catch (IOException e) {
			LOGGER.error(e);
			return true;
		} finally {
			IOUtils.closeQuietly(fileInputStream);
		}
		
		return false;
	}

}
