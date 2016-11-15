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

When using OpenStack as a target for the synchronization, the following properties should be defined in .reposync.properties file:

- OS_PROJECT_DOMAIN_NAME
- OS_USER_DOMAIN_NAME
- OS_PROJECT_NAME
- OS_USERNAME
- OS_PASSWORD
- OS_AUTH_URL
- OS_CACERT

#### OpenNebula configuration

When using OpenNebula as backend, the following properties should be defined in the configuration file:

- ONE_XMLRPC: URL pointing to the RPC endpoint of the OpenNebula installation
- ONE_AUTH: Path to an authorization file in the filesystem containing the OpenNebula credentials in the format <username>:<password>

### reposync-log.properties

This file is a standard JRE logging configuration file that will be used to log events in the REST service.

### repolist

A file containing a list of repositories to synchronize when the sync operation is executed. It must contain a repository per line.
