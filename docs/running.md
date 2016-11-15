## Using the repository synchronization client

A command line client named indigo-reposync is provided for easy management of the synchronization process. The basic syntax is `indigo-reposync <operation> <args>` The available operations are:

- start: Starts the REST server.
- stops: Stops an already running server.
- list: List the images in the backend IaaS platform
- pull \<image\> \[tag\]: Pull an image with an optional tag from DockerHub into the IaaS repository.
- delete \<imageId\>: Delete an image from the IaaS repository
- sync: Execute a synchronization operation pulling all images and tags found in the repository list specified in the REPOSYNC_REPO_LIST_FILE file

All operations will read the configuration files specified in the [Configuration section](configuration.md)

Additionally, a service named indigo-reposync as well, is installed which can start, stop and restart the synchronization server. Service management is dependent on the distributions and so far only Ubuntu 14.04 and CentOS 7 are supported. To manage the service:

- Ubuntu 14.04: `service indigo-reposync [start, stop, restart]`
- CentOS 7: `systemctl [start, stop, restart, status] indigo-reposync.service`

## Note on security
When the client must connect to the server using HTTPS, the server certificate should be added to the `<jre>\lib\security\cacerts` file as a trusted certificate. To do so, you can use the command `keytool -import -alias <alias> -keystore  <jre_dir>/lib/security/cacerts -file <certificate_file>` where:

- *alias* is a name for the certificate
- *jre_dir* is the folder of the JRE which will execute the client
- *certificate_file* is the certificate file of the REST server