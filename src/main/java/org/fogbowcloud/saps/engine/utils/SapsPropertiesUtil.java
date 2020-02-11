package org.fogbowcloud.saps.engine.utils;

import org.apache.log4j.Logger;

import java.util.Properties;

public class SapsPropertiesUtil {

    public static final Logger LOGGER = Logger.getLogger(SapsPropertiesUtil.class);

    public static boolean checkProperties(Properties properties, String[] propertiesSet) {
        if (properties == null) {
            LOGGER.error("Properties arg must not be null.");
            return false;
        }

        for(String property : propertiesSet){
            if (!properties.containsKey(property)) {
                LOGGER.error("Required property " + property + " was not set");
                return false;
            }
        }

        LOGGER.debug("All properties are set");
        return true;
    }
}
