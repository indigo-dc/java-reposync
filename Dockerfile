FROM ubuntu:14.04
MAINTAINER Jose Antonio Sanchez <jose.sanchezm@atos.net>
# Prepare repositories
RUN apt-get update && apt-get install -y software-properties-common apt-transport-https ca-certificates
RUN apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
RUN echo 'deb https://apt.dockerproject.org/repo ubuntu-trusty main' > /etc/apt/sources.list.d/docker.list
RUN add-apt-repository ppa:webupd8team/java
RUN apt-get update
# Install java and dev environment
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
RUN apt-get install -y oracle-java8-installer maven git
# Install and run syncrepos
RUN cd /root && git clone https://github.com/indigo-dc/java-syncrepos.git
RUN echo 'REPOSYNC_REST_ENDPOINT=http://0.0.0.0:8085\nREPOSYNC_BACKEND=OpenNebula' > /root/.reposync.properties
RUN cd /root/java-syncrepos && git pull && mvn compile