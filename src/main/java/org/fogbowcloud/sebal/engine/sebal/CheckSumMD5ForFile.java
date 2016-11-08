package org.fogbowcloud.sebal.engine.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class CheckSumMD5ForFile {
	
	public static final Logger LOGGER = Logger.getLogger(CheckSumMD5ForFile.class);

	public static boolean isFileCorrupted(File localFilesDir) {
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
					
					fileInputStream.close();
				}
			}
		} catch (IOException e) {
			LOGGER.error(e);
			return true;
		} finally {
			// TODO: See if this will reamain commented
			//IOUtils.closeQuietly(fileInputStream);
		}
		
		LOGGER.info("Files are not corrupted!");
		LOGGER.info("Proceeding with the execution");		
		return false;
	}
	
//	private String getProcessOutputString(Process p) {
//		
//		BufferedReader reader = new BufferedReader(new InputStreamReader(
//				p.getInputStream()));
//		StringBuilder builder = new StringBuilder();
//		String line = null;
//		try {
//			while ((line = reader.readLine()) != null) {
//				builder.append(line);
//				builder.append(System.getProperty("line.separator"));
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		return builder.toString();
//	}

	// For test only
//	public static void main(String[] args) {
//		
//		while (true) {
//			String fileDirPath = "/home/esdras/checksumTest";
//			String filePath = fileDirPath + File.separator + "file.nc";
//
//			File fileDir = new File(fileDirPath);
//
//			String checksum;
//
//			try {
//
//				ProcessBuilder builder = new ProcessBuilder("md5sum", filePath);
//				Process p = builder.start();
//				p.waitFor();
//
//				CheckSumMD5ForFile checkSumMD5ForFile = new CheckSumMD5ForFile();
//				String[] splitOutput = checkSumMD5ForFile
//						.getProcessOutputString(p).split("\\s+");
//				checksum = splitOutput[0];
//
//				System.out.println("checksum for file " + filePath + " is "
//						+ checksum);
//
//				String fileChecksumPath = filePath + "." + checksum + ".md5";
//
//				PrintWriter writer = new PrintWriter(fileChecksumPath, "UTF-8");
//				writer.println(checksum);
//
//				writer.close();
//
//				if (isFileCorrupted(fileDir)) {
//					System.out.println("File " + filePath + " is corrupted");
//				} else {
//					System.out
//							.println("File " + filePath + " is not corrupted");
//				}
//
//				File fileChecksum = new File(fileChecksumPath);
//				fileChecksum.delete();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
////		while (true) {
////			String fileDirPath = "/home/esdras/checksumTest";
////			String filePath = fileDirPath + File.separator + "file.nc";
////
////			File fileDir = new File(fileDirPath);
////			File file = new File(filePath);
////			FileInputStream fileInputStream;
////
////			String checksum;
////
////			try {
////				fileInputStream = new FileInputStream(file);
////
////				checksum = DigestUtils.md5Hex(IOUtils
////						.toByteArray(fileInputStream));
////
////				System.out.println("checksum for file " + filePath + " is "
////						+ checksum);
////
////				String fileChecksumPath = filePath + "." + checksum
////						+ ".md5";
////
////				PrintWriter writer = new PrintWriter(fileChecksumPath, "UTF-8");
////				writer.println(checksum);
////				
////				writer.close();				
////				fileInputStream.close();
////				
////				if(isFileCorrupted(fileDir)) {
////					System.out.println("File " + filePath + " is corrupted");
////				} else {
////					System.out.println("File " + filePath + " is not corrupted");
////				}
////				
////				File fileChecksum = new File(fileChecksumPath);				
////				fileChecksum.delete();
////			} catch (Exception e) {
////				e.printStackTrace();
////			}
////		}
//	}
}
