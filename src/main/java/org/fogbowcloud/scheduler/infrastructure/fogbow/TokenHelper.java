package org.fogbowcloud.scheduler.infrastructure.fogbow;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.fogbowcloud.manager.core.plugins.identity.openstack.KeystoneIdentityPlugin;
import org.fogbowcloud.manager.occi.model.Token;

public class TokenHelper {

	private static final String DEFAULT_USER = "user";


	public static Token createNewTokenFromFile(String certificateFilePath) throws FileNotFoundException, IOException{
		
		String certificate = IOUtils.toString(new FileInputStream(certificateFilePath)).replaceAll("\n", "");
		Date date = new Date(System.currentTimeMillis() + (long)Math.pow(10,9));

		return new Token(certificate, DEFAULT_USER, date, new HashMap<String, String>());
	}
	
	
	public static Token createNewKeystoneToken(Map<String, String> credentials) throws FileNotFoundException, IOException{
		return new KeystoneIdentityPlugin(new Properties()).createToken(credentials);
	}
}
