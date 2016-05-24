package com.atos.indigo.reposync;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by jose on 23/05/16.
 */
public class ConfigurationManager {

    public static final String[] BASE_PROPERTIES = new String[]{
            ReposyncTags.REPOSYNC_TOKEN, ReposyncTags.REPOSYNC_REST_ENDPOINT, ReposyncTags.REPOSYNC_BACKEND};
    public static final String[] OPENSTACK_PROPERTIES = new String[]{
            "OS_PROJECT_DOMAIN_NAME", "OS_USER_DOMAIN_NAME", "OS_PROJECT_NAME", "OS_USERNAME", "OS_PASSWORD", "OS_AUTH_URL",
            "OS_IDENTITY_API_VERSION", "OS_IMAGE_API_VERSION", "OS_CACERT"};
    public static final String[] OPENNEBULA_PROPERTIES = new String[]{"ONE_XMLRPC", "ONE_AUTH"};
    // Base URI the Grizzly HTTP server will listen on
    private static final String HOME_PATH = System.getProperty("user.home");
    static final String CONFIG_PATH = HOME_PATH + "/.reposync.properties";

    private static Properties readConfig() {
        Properties result = new Properties();
        File configFile = new File(ConfigurationManager.CONFIG_PATH);
        if (configFile.exists()) {
            try {
                result.load(new FileReader(configFile));
            } catch (IOException e) {

            }
        }
        return result;
    }

    private static void loadConfigList(Properties properties, String[] propList) {
        for (String prop : propList) {
            String currentValue = System.getProperty(prop);
            if (currentValue == null || currentValue.isEmpty()) {
                System.setProperty(prop, properties.getProperty(prop));
            }
        }
    }

    private static void loadConfig(Properties properties) throws ConfigurationException {
        loadConfigList(properties, BASE_PROPERTIES);
        String backend = System.getProperty(ReposyncTags.REPOSYNC_BACKEND);
        if (backend != null) {
            if (ReposyncTags.REPOSYNC_BACKEND_OS.toLowerCase().equals(backend.toLowerCase())) {
                loadConfigList(properties, OPENSTACK_PROPERTIES);
            } else {
                loadConfigList(properties, OPENNEBULA_PROPERTIES);
            }
        } else {
            throw new ConfigurationException(ReposyncTags.REPOSYNC_BACKEND);
        }
    }

    public static void loadConfig() throws ConfigurationException {
        loadConfig(readConfig());
    }

    public static String getProperty(String property) {
        String prop = System.getenv(property);
        if (prop != null) {
            return prop;
        } else {
            return System.getProperty(property);
        }
    }
}
