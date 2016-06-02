FROM ubuntu:14.04
MAINTAINER Jose Antonio Sanchez <jose.sanchezm@atos.net>
# Prepare repositories
RUN apt-get update && apt-get install -y software-properties-common apt-transport-https ca-certificates
RUN add-apt-repository ppa:webupd8team/java
RUN apt-get update
# Install java and dev environment
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
RUN apt-get install -y oracle-java8-installer maven git
# Install and run syncrepos
WORKDIR /root
COPY .docker-java.properties .docker-java.properties
RUN git clone https://github.com/indigo-dc/java-syncrepos.git
RUN cd java-syncrepos && git pull && mvn compile
WORKDIR .indigo-reposync
COPY reposync.properties reposync.properties
COPY reposync-log.properties reposync-log.properties
RUN mkdir logs
RUN touch repolist
