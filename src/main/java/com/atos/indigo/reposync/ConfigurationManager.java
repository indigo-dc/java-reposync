package com.atos.indigo.reposync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;

/**
 * Created by jose on 23/05/16.
 */
public class ConfigurationManager {

  private static final Properties properties = new Properties();
  private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
  // Base URI the Grizzly HTTP server will listen on
  private static final String HOME_PATH = System.getProperty("user.home");
  private static final String DEFAULT_CONFIG_PATH = "/etc";

  private static final String HOME_CONFIG_DIR = HOME_PATH + "/.indigo-reposync";
  private static final String DEFAULT_CONFIG_DIR = DEFAULT_CONFIG_PATH + "/indigo-reposync";

  private static final String CONFIG_FILE = "reposync.properties";
  private static final String REPOLIST_FILE = "repolist";
  private static final String LOG_FILE = "reposync-log.properties";
  private static final String DOCKER_CONFIG_FILE = "docker-java.properties";

  private static ObjectMapper mapper = new ObjectMapper();

  private static File getConfigFile(String fileName) {
    File result = new File(HOME_CONFIG_DIR + "/" + fileName);
    if (result != null && result.exists()) {
      logger.debug("Found " + fileName + " at " + result.getAbsolutePath() );
      return result;
    } else {
      logger.debug("Trying default file " + fileName + " at "
          + DEFAULT_CONFIG_DIR + "/" + fileName );
      return new File(DEFAULT_CONFIG_DIR + "/" + fileName);
    }
  }

  /**
   * Read the configuration from a provided source. Used mainly for testing purposes.
   * @param reader A reader containig the configuration file.
   * @throws ConfigurationException If something goes wrong
   */
  public static void readConfig(Reader reader) throws ConfigurationException {
    try {
      properties.load(reader);
    } catch (IOException e) {
      logger.error("Error trying to load config file", e);
      throw new ConfigurationException("Error reading configuration file",e);
    }
  }

  private static void readConfig() throws ConfigurationException {
    File configFile = getConfigFile(ConfigurationManager.CONFIG_FILE);
    if (configFile.exists()) {
      try {
        readConfig(new FileReader(configFile));
      } catch (IOException e) {
        logger.error("Error trying to load config file", e);
        throw new ConfigurationException("Error reading configuration file",e);
      }
    } else {
      throw new ConfigurationException("Can't read configuration file " + CONFIG_FILE);
    }
  }

  private static void readSyncRepoList() throws ConfigurationException {
    File repoList = getConfigFile(REPOLIST_FILE);
    System.setProperty(ReposyncTags.REPOSYNC_REPO_LIST_FILE, repoList.getAbsolutePath());
    if (repoList != null) {
      readSyncRepoList(repoList);
    }
  }

  private static void readSyncRepoList(File repoFile) throws ConfigurationException {
    List<String> repoList = new ArrayList<>();
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

  private static void loadLoggingConfig() {
    File logFile = getConfigFile(LOG_FILE);
    if (logFile.exists()) {
      try {
        LogManager.getLogManager().readConfiguration(new FileInputStream(logFile));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * Read the configuration from the default location in the filesystem.
   * @throws ConfigurationException If something goes wrong.
   */
  public static void loadConfig() throws ConfigurationException {
    loadLoggingConfig();
    readConfig();
  }

  /**
   * Gets a property either from the system environment or the system properties.
   * @param property property name.
   * @return the system environment variable or system property found.
   */
  public static String getProperty(String property) {
    String prop = System.getenv(property);
    if (prop == null) {
      return properties.getProperty(property);
    } else {
      return null;
    }
  }

  /**
   * Get the list of repositories in repolist file.
   * @return List of repositories.
   */
  public static List<String> getRepoList() {
    try {
      readSyncRepoList();
      String repoListStr = getProperty(ReposyncTags.REPOSYNC_REPO_LIST);
      if (repoListStr != null) {
        return mapper.readValue(repoListStr, new TypeReference<List<String>>(){});
      } else {
        return new ArrayList<>();
      }
    } catch (ConfigurationException e) {
      logger.error("Error getting sync repolist", e);
      return new ArrayList<>();
    } catch (IOException e) {
      logger.error("Error reading sync repolist value", e);
      return new ArrayList<>();
    }

  }

  /**
   * Check if debug mode is enabled in the configuration.
   * @return True if debug mode is enabled.
   */
  public static boolean isDebugMode() {
    String debug = getProperty(ReposyncTags.REPOSYNC_DEBUG_MODE);
    if (debug != null) {
      return new Boolean(debug);
    }
    return false;
  }

  /**
   * Get the Docker configuration from the configuration folder.
   * @return The docker configuration properties.
   */
  public static Properties getDockerProperties() {
    Properties result = new Properties();
    File dockerFile = getConfigFile(DOCKER_CONFIG_FILE);
    if (dockerFile != null && dockerFile.exists()) {
      try {
        result.load(new FileInputStream(dockerFile));
      } catch (IOException e) {
        logger.error("Error reading docker configuration",e);
      }
    }
    return result;
  }
}
