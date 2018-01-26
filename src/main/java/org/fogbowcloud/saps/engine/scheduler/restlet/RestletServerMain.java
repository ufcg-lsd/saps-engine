package org.fogbowcloud.saps.engine.scheduler.restlet;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.model.SapsUser;

import java.io.FileInputStream;
import java.util.Properties;

public class RestletServerMain {

    private static final String ADMIN_EMAIL = "admin_email";
    private static final String ADMIN_USER = "admin_user";
    private static final String ADMIN_PASSWORD = "admin_password";
    private static final Logger LOGGER = Logger.getLogger(RestletServerMain.class);

    public static void main(String[] args) throws Exception {

        String confPath = args[0];

        final Properties properties = new Properties();
        FileInputStream input = new FileInputStream(confPath);
        properties.load(input);

        DatabaseApplication databaseApplication = new DatabaseApplication(properties);
        databaseApplication.startServer();

        String userEmail = properties.getProperty(ADMIN_EMAIL);
        SapsUser user = databaseApplication.getUser(userEmail);
        if (user == null) {
            String userName = properties.getProperty(ADMIN_USER);
            String userPass = DigestUtils.md5Hex(properties.getProperty(ADMIN_PASSWORD));

            try {
                databaseApplication.createUser(userEmail, userName, userPass, true, false, true);
            } catch (Exception e) {
                LOGGER.error("Error while creating user", e);
            }
        }
    }

}
