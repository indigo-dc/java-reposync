package com.atos.indigo.reposync.beans;

import java.util.List;

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

    private List<String> tags;

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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
