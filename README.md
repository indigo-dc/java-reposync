## Description

This component can be used to synchronize Docker images in DockerHub to instances of either OpenStack or OpenNebula with Docker support already configured. To do so, it relies on the WebHooks mechanisms offered by DockerHub infrastructure and it provides a REST interface to list the available images already synchronized and some operations to force the synchronization of individual ones.

## Role in INDIGO - DataCloud

The role at INDIGO - DataCloud is to provide a mechanism to synchronize Docker images uploaded to DockerHub (mainly but not limited to images at https://hub.docker.com/u/indigodatacloud/) to local IaaS platforms based on OpenStack and OpenNebula. To do so, it relies on platforms which are configured to register and run Docker images. That functionality is provided by:

- [Nova Docker](https://github.com/openstack/nova-docker) for OpenStack
- [ONEDock](https://github.com/indigo-dc/onedock) for OpenNebula

The images synchronized by this component will be used by the [Cloud Information Provider](https://github.com/indigo-dc/cloud-info-provider) scripts to feed the INDIGO CMDB. 

## Dependencies

This project uses the following elements:
- [OpenStack java bindings](http://www.openstack4j.com/)
- [Docker java bindings](https://github.com/docker-java/docker-java)
- [OpenNebula java bindings](http://docs.opennebula.org/4.12/integration/system_interfaces/java.html)

The later one is not present in any maven repository and so it's included in the project to easen the deployment.

Additionally the REST interface is based on [Jersey 2.X](https://jersey.java.net/) and [Grizzly](https://grizzly.java.net/)

All dependencies save OpenNebula java bindings are managed by maven automatically

## [Configuration](docs/configuration.md)

## Development

To start developing just clone the repository with `git clone https://github.com/indigo-dc/java-syncrepos.git` and create a valid configuration file. Compile the code with `mvn compile`

To execute the REST server, run `./reposync.sh start`

To extend or develop new functionalities, see the [development guide](docs/development.md)

## [Deployment](docs/deployment.md)

