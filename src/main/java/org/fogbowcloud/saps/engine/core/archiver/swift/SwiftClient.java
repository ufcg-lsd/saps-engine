package org.fogbowcloud.saps.engine.core.archiver.swift;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.scheduler.util.SapsPropertiesConstants;
import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AccountFactory;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;

public class SwiftClient {

	private static final String URL_PATH_SEPARATOR = "/";
	private Account account;
	
	public static final Logger LOGGER = Logger.getLogger(SwiftClient.class);

	public SwiftClient(Properties properties) {

		AccountConfig config = new AccountConfig();
		config.setUsername(properties
				.getProperty(SapsPropertiesConstants.SWIFT_USERNAME));
		config.setPassword(properties
				.getProperty(SapsPropertiesConstants.SWIFT_PASSWORD));
		config.setTenantName(properties
				.getProperty(SapsPropertiesConstants.SWIFT_TENANT_NAME));
		config.setAuthUrl(properties
				.getProperty(SapsPropertiesConstants.SWIFT_AUTH_URL));
		config.setAuthenticationMethod(AuthenticationMethod.KEYSTONE);
		account = new AccountFactory(config).createAccount();

	}
	
	public void createContainer(String containerName) {
		try {
			LOGGER.debug("containerName " + containerName);
			Container container = account.getContainer(containerName);
			
			if(!container.exists()) {
				LOGGER.debug("Creating container " + containerName);
				container.create();
			} else {
				LOGGER.debug("Container " + containerName + " already exist");
				// TODO: see how to deal with this
			}
		} catch(Exception e) {
			LOGGER.error(e);
		}
	}
	
	public void deleteContainer(String containerName) {
		try {
			LOGGER.debug("containerName " + containerName);
			Container container = account.getContainer(containerName);
			
			if(container.exists()) {
				LOGGER.debug("Deleting container " + containerName);
				container.delete();
			} else {
				LOGGER.debug("Container " + containerName + " does not exist");
				// TODO: see how to deal with this
			}
		} catch(Exception e) {
			LOGGER.error(e);
		}
	}
	
	public boolean isContainerEmpty(String containerName) {
		try {
			LOGGER.debug("containerName " + containerName);
			Container container = account.getContainer(containerName);
			
			if(container.exists()) {
				LOGGER.debug("Deleting container " + containerName);
				if(container.getBytesUsed() <= 0) {
					return true;
				}
			} else {
				LOGGER.debug("Container " + containerName + " does not exist");
				return false;
			}
		} catch(Exception e) {
			LOGGER.error(e);
		}
		
		return false;
	}

	public void uploadFile(String containerName, File file, String pseudFolder)
			throws Exception {
		try {
			LOGGER.debug("containerName " + containerName);
			LOGGER.debug("pseudFolder " + pseudFolder + " before normalize");
			Container container = account.getContainer(containerName);

			String completeFileName;
			if (pseudFolder != null && !pseudFolder.isEmpty()) {
				pseudFolder = this.normalizePseudFolder(pseudFolder);
				LOGGER.debug("Pseud folder " + pseudFolder + " after normalize");
				
				completeFileName = pseudFolder + file.getName();
			} else {
				completeFileName = file.getName();
			}

			LOGGER.debug("completedFileName " + completeFileName);
			StoredObject storedObject = container.getObject(completeFileName);
			storedObject.uploadObject(file);
		} catch (Exception e) {
			throw new Exception("Error while trying to upload file "
					+ file.getAbsolutePath(), e);
		}
	}

	public byte[] downloadFile(String containerName, String fileName,
			String pseudFolder) {
		
		LOGGER.debug("fileName " + fileName);
		LOGGER.debug("containerName " + containerName);
		LOGGER.debug("pseudFolder " + pseudFolder + " before normalize");

		Container container = account.getContainer(containerName);

		String completeFileName;
		if (pseudFolder != null && !pseudFolder.isEmpty()) {
			pseudFolder = this.normalizePseudFolder(pseudFolder);
			LOGGER.debug("Pseudo folder " + pseudFolder + " after normalize");
			
			completeFileName = pseudFolder + fileName;
		} else {
			completeFileName = fileName;
		}

		LOGGER.debug("Complete file name " + completeFileName);
		StoredObject storedObject = container.getObject(completeFileName);
		return storedObject.downloadObject();

	}
	
	// TODO: review
	public void deleteFile(String containerName, String pseudFolder, String fileName) {
		LOGGER.debug("fileName " + fileName);
		LOGGER.debug("containerName " + containerName);
		
		Container container = account.getContainer(containerName);
		
		String completeFileName;
		if (pseudFolder != null && !pseudFolder.isEmpty()) {
			pseudFolder = this.normalizePseudFolder(pseudFolder);
			LOGGER.debug("Pseudo folder " + pseudFolder + " after normalize");
			
			completeFileName = pseudFolder + fileName;
		} else {
			completeFileName = fileName;
		}
		
		LOGGER.debug("Complete file name " + completeFileName);
		container.getObject(completeFileName).delete();
		
		LOGGER.debug("Object " + completeFileName + " deleted successfully");
	}
	
	
	public int numberOfFilesInContainer(String containerName) {
		LOGGER.debug("containerName " + containerName);
		
		Container container = account.getContainer(containerName);
		
		Collection<StoredObject> objects = container.list();
		
		return objects.size();
	}
	
	private String normalizePseudFolder(String value) {
		StringBuilder normalizedPath = new StringBuilder();
		// Path cannot have separator "/" in begin.
		if (value.startsWith(URL_PATH_SEPARATOR)) {
			value = value.substring(1, value.length());
		}
		normalizedPath.append(value);
		if (!value.endsWith(URL_PATH_SEPARATOR)) {
			normalizedPath.append(URL_PATH_SEPARATOR);
		}
		return normalizedPath.toString();
	}

}
