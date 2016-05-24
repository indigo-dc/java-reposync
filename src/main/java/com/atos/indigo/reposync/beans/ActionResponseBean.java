package com.atos.indigo.reposync.beans;

/**
 * Created by jose on 11/05/16.
 */
public class ActionResponseBean {

  private boolean success;
  private String errorMessage;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
