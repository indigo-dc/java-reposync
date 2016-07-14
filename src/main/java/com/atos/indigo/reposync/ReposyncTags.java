package com.atos.indigo.reposync;

/**
 * Created by jose on 23/05/16.
 */
public interface ReposyncTags {
  String REPOSYNC_BACKEND = "REPOSYNC_BACKEND";
  String REPOSYNC_TOKEN = "REPOSYNC_TOKEN";
  String TOKEN_HEADER = "X-Auth-Token";
  String REPOSYNC_BACKEND_OS = "Openstack";
  String REPOSYNC_REST_ENDPOINT = "REPOSYNC_REST_ENDPOINT";
  String REPOSYNC_REPO_LIST_FILE = "REPOSYNC_REPO_LIST_FILE";
  String REPOSYNC_REPO_LIST = "REPOSYNC_REPO_LIST";
  String REPOSYNC_DEBUG_MODE = "REPOSYNC_DEBUG_MODE";

  String DISTRIBUTION_TAG = "eu.indigo-datacloud.distribution";
  String DIST_VERSION_TAG = "eu.indigo-datacloud.version";
}
