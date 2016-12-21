# Configuration

The repository synchronization project needs access to:
- One docker installation, prefereably in the same machine as it's running but it might be in any remote location
- One OpenStack or OpenNebula installation which is accessible from the host in which the project is running

## Configuration files
The configuration files should be defined in /etc/indigo-reposync directory however, if the server or client must be run as a user instead of root, they can be stored in $HOME/.indigo-reposync folder.
This is the folder's content:

### docker-java.properties

The configuration properties which are accepted are described in https://github.com/docker-java/docker-java

### reposync.properties

This file is the main configuration one and should contain the following mandatory properties:

- REPOSYNC_TOKEN: The secret token used for authorization which will be used in the DockerHub webhook. It will be used in the rest of the operations as an authorization token as well.
- REPOSYNC_REST_ENDPOINT: Endpoint for the REST server to listen for requests
- REPOSYNC_BACKEND: Backend to use, which should be one value of OpenNebula or OpenStack
- USE_SSL: Use HTTPS for the REST server. By default it's false
- KEYSTORE_LOCATION: When using SSL, the location to a keystore with the server certificate
- KEYSTORE_PASSWORD: When using SSL, the password for the above keystore

Depending on the backend, further properties will have to be defined.

#### OpenStack configuration

When using OpenStack as a target for the synchronization, the following properties should be defined in reposync.properties file:

- OS_PROJECT_DOMAIN_NAME: Project domain in OpenStack
- OS_USER_DOMAIN_NAME: User domain in OpenStack
- OS_PROJECT_NAME: Project name to use
- OS_USERNAME: Username to use to access Keystone
- OS_PASSWORD: Password for the aforementioned user
- OS_AUTH_URL: URL to the Keystone server API. **Please note that only v3 of the API is supported.**
- OS_CACERT: In case of using SSL, path to the .cert file used during server interactions

##### Sharing

On OpenStack there's the possibility of sharing an image with a some other members (tenants). To do so, a configuration file named os-share.json should be created. It must contain a JSON object in which keys are the image names (or image name plus tag) and the values are a list of tenants IDs. For example:

```
{
  "busybox" : ["01e906915cc04fe3970c6bc467811e64","0ebfb625d79747da9d9a4bcfa38affe9"],
  "alpine:3.2" : ["2712d6a5a05f403899e3cd019644e9dc"]
}

```

#### OpenNebula configuration

When using OpenNebula as backend, the following properties should be defined in the configuration file:

- ONE_XMLRPC: URL pointing to the RPC endpoint of the OpenNebula installation
- ONE_AUTH: Path to an authorization file in the filesystem containing the OpenNebula credentials in the format <username>:<password>

### reposync-log.properties

This file is a standard JRE logging configuration file that will be used to log events in the REST service.

### repolist

A file containing a list of repositories to synchronize when the sync operation is executed. It must contain a repository per line.
