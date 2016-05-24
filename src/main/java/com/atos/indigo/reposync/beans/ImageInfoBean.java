package com.atos.indigo.reposync.beans;

/**
 * Created by jose on 9/05/16.
 */
public class ImageInfoBean {

  public enum ImageType {
    VM,
    DOCKER
  }

  private String id;

  private String name;

  private ImageType type;

  private String dockerId;

  private String dockerName;

  private String dockerTag;

  private String comment;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ImageType getType() {
    return type;
  }

  public void setType(ImageType type) {
    this.type = type;
  }

  public String getDockerTag() {
    return dockerTag;
  }

  public void setDockerTag(String dockerTag) {
    this.dockerTag = dockerTag;
  }

  public String getDockerId() {
    return dockerId;
  }

  public void setDockerId(String dockerId) {
    this.dockerId = dockerId;
  }

  public String getDockerName() {
    return dockerName;
  }

  public void setDockerName(String dockerName) {
    this.dockerName = dockerName;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }
}
