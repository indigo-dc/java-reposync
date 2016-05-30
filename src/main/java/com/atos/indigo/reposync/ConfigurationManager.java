package com.atos.indigo.reposync;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by jose on 23/05/16.
 */
public class ConfigurationManager {

  public static final String[] BASE_PROPERTIES = new String[]{
    ReposyncTags.REPOSYNC_TOKEN, ReposyncTags.REPOSYNC_REST_ENDPOINT,
    ReposyncTags.REPOSYNC_BACKEND, ReposyncTags.REPOSYNC_REPO_LIST_FILE};
  public static final String[] OPENSTACK_PROPERTIES = new String[]{
    "OS_PROJECT_DOMAIN_NAME", "OS_USER_DOMAIN_NAME", "OS_PROJECT_NAME",
    "OS_USERNAME", "OS_PASSWORD", "OS_AUTH_URL",
    "OS_IDENTITY_API_VERSION", "OS_IMAGE_API_VERSION", "OS_CACERT"};
  public static final String[] OPENNEBULA_PROPERTIES = new String[]{"ONE_XMLRPC", "ONE_AUTH"};
  private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
  // Base URI the Grizzly HTTP server will listen on
  private static final String HOME_PATH = System.getProperty("user.home");
  static final String CONFIG_PATH = HOME_PATH + "/.reposync.properties";

  private static ObjectMapper mapper = new ObjectMapper();

  private static Properties readConfig() {
    Properties result = new Properties();
    File configFile = new File(ConfigurationManager.CONFIG_PATH);
    if (configFile.exists()) {
      try {
        result.load(new FileReader(configFile));
      } catch (IOException e) {
        logger.error("Error trying to load config file", e);
      }
    }
    return result;
  }

  private static void readSyncRepoList(String repoListFile) throws ConfigurationException {
    List<String> repoList = new ArrayList<>();
    File repoFile = new File(repoListFile);
    try {
      LineIterator iterator = FileUtils.lineIterator(repoFile);
      while (iterator.hasNext()) {
        repoList.add(iterator.nextLine().trim());
      }
      System.setProperty(ReposyncTags.REPOSYNC_REPO_LIST, mapper.writeValueAsString(repoList));
    } catch (IOException e) {
      throw new ConfigurationException("Can't read repository list file",e);
    }
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
      throw ConfigurationException.undefinedProperty(ReposyncTags.REPOSYNC_BACKEND);
    }
    String repoList = getProperty(ReposyncTags.REPOSYNC_REPO_LIST_FILE);
    if (repoList != null) {
      readSyncRepoList(repoList);
    }
  }

  public static void loadConfig() throws ConfigurationException {
    loadConfig(readConfig());
  }

  /**
   * Gets a property either from the system environment or the system properties.
   * @param property property name.
   * @return the system environment variable or system property found.
   */
  public static String getProperty(String property) {
    String prop = System.getenv(property);
    if (prop != null) {
      return prop;
    } else {
      return System.getProperty(property);
    }
  }
}
