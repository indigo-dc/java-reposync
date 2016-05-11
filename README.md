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

### OpenStack configuration

When using OpenStack as a target for the synchronization, the following system variables should be defined:
- OS_PROJECT_DOMAIN_NAME
- OS_USER_DOMAIN_NAME
- OS_PROJECT_NAME
- OS_USERNAME
- OS_PASSWORD
- OS_AUTH_URL
- OS_CACERT

Just running the project with the above variables defined with correct values should suffice to be able to synchronize images. An example could be:
```
export OS_PROJECT_DOMAIN_NAME=default
export OS_USER_DOMAIN_NAME=default
export OS_PROJECT_NAME=indigo
export OS_USERNAME=<openstack_admin_user>
export OS_PASSWORD=<openstack_admin_password>
export OS_AUTH_URL=<openstack_keystone_url>
export OS_CACERT=<certificate_location>
```

Please notice that only v3 of the keystone protocol is supported.

## Development

To start developing just clone the repository with git clone https://github.com/indigo-dc/java-syncrepos.git 

To execute the REST server, run mvn exec:java

