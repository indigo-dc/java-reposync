# Service Reference Card Template

* Daemons running

There is a daemon that will be run when the service is started named indigo-reposync

* Init scripts and options (start|stop|restart|...)

Starting and stopping the service is distribution dependant

*Ubuntu 14.04*

`service indigo-reposync (start|stop|restart)`

*CentOS 7*

`systemctl (start|stop|status|enable|disable) indigo-reposync`

* Configuration files location with example or template

As explained in the [Configuration section](configuration) the configuration files are stored by default at /etc/indigo-reposync although they can be overriden per user in the folder ~/.indigo-reposync

The files are:
- docker-java.properties with docker configuration
- reposync.properties with the Indigo reposync configuration
- reposync-log.properties with logging configuration
- repolyst with a list of docker images to synchronize

A default configuration and a template for reposync.properties is installed in /etc/indigo-reposync when the packages are installed.

* Logfile locations (and management) and other useful audit information

The logging configuration is managed by the reposync-log.properties file. By default it will log to /var/log/reposync.log

* Open ports

The reposync component will listen in a port configured in reposync.properties file. By default it's 8085.

* Possible unit test of the service

Tests are provided with the source code at https://github.com/indigo-dc/java-reposync by running mvn test

* Where is service state held (and can it be rebuilt)

The service is stateless

* Cron jobs

None

* Security information
  * Access control Mechanism description (authentication & authorization)
  
  A token which is defined in reposync.properties file is needed in the header of every request
  
  * How to block/ban a user
  
  Not applicable
  
  * Network Usage
  
  Network usage is highly dependent of the number and size of the docker images to synchronize
  
  * Firewall configuration
  
  The port configured in reposync.properties file should be open to the outside world to allow requests and WebHooks callback from DockerHub.
  
  * Security recommendations
  
  Change the token in reposync.properties from the default one
