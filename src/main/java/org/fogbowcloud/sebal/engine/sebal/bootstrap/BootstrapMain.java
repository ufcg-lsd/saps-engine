package org.fogbowcloud.sebal.engine.sebal.bootstrap;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.engine.sebal.ImageData;
import org.fogbowcloud.sebal.engine.sebal.ImageState;

public class BootstrapMain {

    private static final Logger LOGGER = Logger.getLogger(BootstrapMain.class);

    private static final String DATASTORE_USERNAME = "datastore_username";
    private static final String DATASTORE_PASSWORD = "datastore_password";
    private static final String DATASTORE_DRIVER = "datastore_driver";
    private static final String DATASTORE_URL_PREFIX = "datastore_url_prefix";

    static String SELECT_ALL_IMAGES_SQL = "SELECT * FROM nasa_images ORDER BY priority, image_name";

    /**
     * @param args args[0] path to properties file
     *             args[1] database IP
     *             args[2] database port
     * @throws IOException
     * @throws SQLException
     */
    public static void main(String[] args) throws IOException {

        //FIXME: check parameters to fail gracefully
        final Properties properties = new Properties();
        FileInputStream input = new FileInputStream(args[0]);
        properties.load(input);

        String sqlIP = args[1];
        String sqlPort = args[2];

        BasicDataSource connectionPool = new BasicDataSource();

        connectionPool.setUsername(properties.getProperty(DATASTORE_USERNAME));
        connectionPool.setPassword(properties.getProperty(DATASTORE_PASSWORD));
        connectionPool.setDriverClassName(properties.getProperty(DATASTORE_DRIVER));
        connectionPool.setUrl(properties.getProperty(DATASTORE_URL_PREFIX) + sqlIP + ":" + sqlPort);
        connectionPool.setInitialSize(1);


        try {
            Connection c = connectionPool.getConnection();
            PreparedStatement selectStatement = null;
            selectStatement = c.prepareStatement(SELECT_ALL_IMAGES_SQL);

            ResultSet rs = selectStatement.executeQuery();

            while (rs.next()) {
                ImageData imageData = new ImageData(
                        rs.getString("image_name"),
                        rs.getString("download_link"),
                        ImageState.getStateFromStr(rs.getString("state")),
                        rs.getString("federation_member"),
                        rs.getInt("priority"),
                        rs.getString("station_id"),
                        rs.getString("sebal_version"),
                        rs.getString("sebal_engine_version"),
                        rs.getString("blowout_version"),
                        rs.getDate("ctime"),
                        rs.getDate("utime"),
                        rs.getString("error_msg"));
                System.out.println(imageData);
            }

        } catch (Throwable e) {
            LOGGER.error("Error while handling catalog database", e);
            System.exit(1);
        }
    }

}
