package com.atos.indigo.reposync;

/**
 * Created by jose on 23/05/16.
 */
public class ConfigurationException extends Exception {

    private String property;

    public ConfigurationException(String property) {
        this.property = property;
    }

    @Override
    public String getMessage() {
        return "Mandatory property "+property+" not defined";
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }
}
