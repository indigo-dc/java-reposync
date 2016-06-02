# java-syncrepos
This project is a continuation of the discontinued https://github.com/indigo-dc/python-syncrepos

## Dependencies

This project uses the following elements:
- [OpenStack java bindings](http://www.openstack4j.com/)
- [Docker java bindings](https://github.com/docker-java/docker-java)
- [OpenNebula java bindings](http://docs.opennebula.org/4.12/integration/system_interfaces/java.html)

The later one is not present in any maven repository and so it's included in the project to easen the deployment.

Additionally the REST interface is based on [Jersey 2.X](https://jersey.java.net/) and [Grizzly](https://grizzly.java.net/)

All dependencies save OpenNebula java bindings are managed by maven automatically

## Configuration

The repository synchronization project needs access to:
- One docker installation, prefereably in the same machine as it's running but it might be in any remote location
- One OpenStack or OpenNebula installation which is accessible from the host in which the project is running

### Docker configuration

The configuration process is described in https://github.com/docker-java/docker-java

All the described options are accepted but it's prefereable to have a $HOME/.docker-java.properties file configured to simplify the process

### Configuration file

A configuration file should be created in $HOME/.reposync.properties of the user which starts the REST server. This file should contain the following mandatory properties:

- REPOSYNC_TOKEN: The secret token used for authorization which will be used in the DockerHub webhook. It will be used in the rest of the operations as an authorization token as well.
- REPOSYNC_REST_ENDPOINT: Endpoint for the REST server to listen for requests
- REPOSYNC_BACKEND: Backend to use, which should be one value of OpenNebula or OpenStack

and the following optional properties:

- REPOSYNC_REPO_LIST_FILE: Used in case using the sync operation is needed. It should point to a file in the filesystem with a list of repositories to synchronize, one by line. When syncing, this repositories will be explored and their images and tags will be added to the backend image registry.

Depending on the backend, further properties will have to be defined.

### OpenStack configuration

When using OpenStack as a target for the synchronization, the following properties should be defined in .reposync.properties file:

- OS_PROJECT_DOMAIN_NAME
- OS_USER_DOMAIN_NAME
- OS_PROJECT_NAME
- OS_USERNAME
- OS_PASSWORD
- OS_AUTH_URL
- OS_CACERT

### OpenNebula configuration

When using OpenNebula as backend, the following properties should be defined in the configuration file:

- ONE_XMLRPC: URL pointing to the RPC endpoint of the OpenNebula installation
- ONE_AUTH: Path to an authorization file in the filesystem containing the OpenNebula credentials in the format <username>:<password>

## Development

To start developing just clone the repository with `git clone https://github.com/indigo-dc/java-syncrepos.git` and create a valid configuration file. Compile the code with `mvn compile`

To execute the REST server, run `./reposync.sh start`

## Docker image

To easen deployment, a docker file is provided. To create a docker image with the reposync project, execute the following command in the root of java-syncrepos project:

`docker build -t indigo-reposync:v1 .`

It will create an image named indigo-reposync with tag v1. Please, feel free to choose whatever name and tag you may want.

To run it, access to the host docker installation is needed. To do so, please run the recently created image with the command:

`docker run -v /var/run/docker.sock:/var/run/docker.sock  --name reposync -i -t indigo-reposync:v1 /bin/bash`

Once in the bash, create a valid configuration file in /root folder and enter in /root/java-reposync folder

The following commands are available in the reposync.sh shell script:

- start: Starts the REST server
- list: List the images in the backend IaaS platform
- pull <image> [tag]: Pull an image with an optional tag from DockerHub into the IaaS repository.
- delete <imageId>: Delete an image from the IaaS repository
- sync: Execute a synchronization operation pulling all images and tags found in the repository list specified in the REPOSYNC_REPO_LIST_FILE file



