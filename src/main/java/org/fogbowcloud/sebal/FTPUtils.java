package org.fogbowcloud.sebal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

public class FTPUtils {
	
	private static String ftpServerIP;
	private static int ftpServerPort;
	private static String ftpUser;
	private static String ftpPass;
	private static Properties properties;
	
	public static final Logger LOGGER = Logger.getLogger(FTPUtils.class);
	
	public FTPUtils(Properties properties, String ftpServerIP, String ftpServerPort) {
		this.properties = properties;
		this.ftpServerIP = ftpServerIP;
		this.ftpServerPort = Integer.parseInt(ftpServerPort);
		this.ftpUser = properties.getProperty("ftp_server_user");
		this.ftpPass = properties.getProperty("ftp_server_pass");
	}
	
	public static void init(ImageData imageData) {
		FTPClient ftpClient = new FTPClient();
		connect(ftpClient);
		
		downloadFromFTPServer(imageData, ftpClient);
	}
	
    public static void connect(FTPClient ftpClient) {
        try {
            ftpClient.connect(ftpServerIP, ftpServerPort);
            showServerReply(ftpClient);
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                System.out.println("Operation failed. Server reply code: " + replyCode);
                return;
            }
            boolean success = ftpClient.login(ftpUser, ftpPass);
            showServerReply(ftpClient);
            if (!success) {
                System.out.println("Could not login to the server");
                return;
            } else {
                System.out.println("LOGGED IN SERVER");
            }
        } catch (IOException ex) {
            System.out.println("Oops! Something wrong happened");
            ex.printStackTrace();
        }
    }
    
	private static void showServerReply(FTPClient ftpClient) {
        String[] replies = ftpClient.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies) {
                System.out.println("SERVER: " + aReply);
            }
        }
    }
	
	public static void downloadFromFTPServer(ImageData imageData, FTPClient ftpClient) {
        try {
			String remoteResultsPath = properties
					.getProperty("sebal_export_path")
					+ "/results/"
					+ imageData.getName();
			
			File remoteResultsDir = new File(remoteResultsPath);
			
			if(!remoteResultsDir.exists() && !remoteResultsDir.isDirectory()) {
				LOGGER.error("This folder doesn't exist or is not a directory.");
				return;
			}
			
			OutputStream outputStream = null;
			boolean success = false;
			File downloadFile = null;

			List<File> files = (List<File>) FileUtils.listFiles(
					remoteResultsDir, TrueFileFilter.INSTANCE,
					TrueFileFilter.INSTANCE);

			for (File file : files) {
				downloadFile = new File(properties.get("sebal_mount_point") + "/"  + file.getName());
				outputStream = new BufferedOutputStream(new FileOutputStream(
						downloadFile));
				success = ftpClient.retrieveFile(
						remoteResultsPath + "/" + file.getName(), outputStream);
			}
			outputStream.close();

			if (success) {
				System.out.println("Image " + imageData.getName()
						+ " has been downloaded successfully.");
			}
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
