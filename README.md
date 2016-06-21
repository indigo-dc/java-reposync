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

### Configuration 
The configuration files should be defined in $HOME/.indigo-reposync directory and it's composed of:

#### reposync.properties

This file is the main configuration one and should contain the following mandatory properties:

- REPOSYNC_TOKEN: The secret token used for authorization which will be used in the DockerHub webhook. It will be used in the rest of the operations as an authorization token as well.
- REPOSYNC_REST_ENDPOINT: Endpoint for the REST server to listen for requests
- REPOSYNC_BACKEND: Backend to use, which should be one value of OpenNebula or OpenStack

and the following optional properties:

- REPOSYNC_REPO_LIST_FILE: Used in case using the sync operation is needed. It should point to a file in the filesystem with a list of repositories to synchronize, one by line. When syncing, this repositories will be explored and their images and tags will be added to the backend image registry.

Depending on the backend, further properties will have to be defined.

##### OpenStack configuration

When using OpenStack as a target for the synchronization, the following properties should be defined in .reposync.properties file:

- OS_PROJECT_DOMAIN_NAME
- OS_USER_DOMAIN_NAME
- OS_PROJECT_NAME
- OS_USERNAME
- OS_PASSWORD
- OS_AUTH_URL
- OS_CACERT

##### OpenNebula configuration

When using OpenNebula as backend, the following properties should be defined in the configuration file:

- ONE_XMLRPC: URL pointing to the RPC endpoint of the OpenNebula installation
- ONE_AUTH: Path to an authorization file in the filesystem containing the OpenNebula credentials in the format <username>:<password>

#### reposync-log.properties

This file is a standard JRE logging configuration file that will be used to log events in the REST service.

#### repolist

A file containing a list of repositories to synchronize when the sync operation is executed. It must contain a repository per line.

## Development

To start developing just clone the repository with `git clone https://github.com/indigo-dc/java-syncrepos.git` and create a valid configuration file. Compile the code with `mvn compile`

To execute the REST server, run `./reposync.sh start`

## Docker image

To easen deployment, a docker file is provided. To create a docker image with the reposync project, execute the following command in the root of java-syncrepos project:

`docker build -t indigo-reposync:v1 .`

It will create an image named indigo-reposync with tag v1. Please, feel free to choose whatever name and tag you may want.

To run it, access to the host docker installation is needed. To do so, please run the recently created image with the command:

`docker run -v /var/run/docker.sock:/var/run/docker.sock  --name reposync -i -t indigo-reposync:v1`

Some stub configuration files are included in the image and they can be filled and used to run the REST service, however, it's recommended to manage them in the host system and mount them as volumes. Also, to be able to access the service from 
outside the host system, port redirection might be needed. A common startup configuration can be:

`docker run -d -v /var/run/docker.sock:/var/run/docker.sock -v /<homedir>/.indigo-reposync:/root/.indigo-reposync -v /<homedir>/.one/one_auth:/root/one_auth -p 80:8085 --name reposyncserver -i -t indigo-reposync:v1`

That way, the configuration is passed from the host system to the container and it can be shared among different instances.

To execute any command, when the container is running execute:

`docker exec reposyncserver reposync <command>` 

The following commands are available in the reposync shell script:

- start: Starts the REST server
- list: List the images in the backend IaaS platform
- pull <image> [tag]: Pull an image with an optional tag from DockerHub into the IaaS repository.
- delete <imageId>: Delete an image from the IaaS repository
- sync: Execute a synchronization operation pulling all images and tags found in the repository list specified in the REPOSYNC_REPO_LIST_FILE file

## DockerHub configuration

In order to benefit from the automatic synchronization of DockerHub repositories, some webhooks are needed that will trigger the pulling of the updated images.
Let's imagine that we run the docker image with the following command:

`docker run -d -v /var/run/docker.sock:/var/run/docker.sock -v /home/ubuntu/.indigo-reposync:/root/.indigo-reposync -v /home/ubuntu/lipcaroot.pem:/root/lipcaroot.pem -p 80:8085 --name reposyncserver -i -t indigodatacloud/reposync:latest`

This will run the server taking the configuration from the directory /home/ubuntu/.indigo-reposync in the host system and let's assume that this configuration makes it listen on port 8085 and that the host system has port 80 open to the outside world. Also, let's assume that the host machine has a DNS name 'reposync.my-idc-cloud.net' and that we've defined a secret token in the configuration file as 'mysecrettoken'.

If we wanted to have the images in DockerHub repository indigodatacloud/ubuntu-sshd synchronized then we would need to create it pointing to the following URL:

http://reposync.my-idc-cloud.net/v1.0/notify?token=mysecrettoken

Now everytime that an image is pushed to the indigodatacloud/ubuntu-sshd repository, the reposync service will receive a notification and execute a pull operation over the image tags that have been updated, integrating and/or updating them in the local image repository (in this case, Glance from OpenStack).
If the reposync service is down when the notification is launched or the network is not working, then a manual pull can be executed by accessing the machine in which the reposync docker container is running and executing `docker exec reposyncserver reposync pull indigodatacloud/ubuntu-sshd <updated_tag>` where \<updated_tag\> is the name of the tag that has just been pushed (and whose notification has been missed).


