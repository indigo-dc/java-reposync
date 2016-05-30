package com.atos.indigo.reposync;

import java.io.IOException;

/**
 * Created by jose on 23/05/16.
 */
public class ConfigurationException extends Exception {

  public static ConfigurationException undefinedProperty(String property) {
    return new ConfigurationException("Mandatory property " + property + " not defined");
  }

  public ConfigurationException(String property) {
    super(property);
  }

  public ConfigurationException(String message, IOException source) {
    super(message,source);
  }

}
