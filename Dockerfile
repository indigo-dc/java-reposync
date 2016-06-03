FROM ubuntu:16.04
MAINTAINER Jose Antonio Sanchez <jose.sanchezm@atos.net>

# Install java and dev environment
RUN apt-get update && apt-get install -y ca-certificates maven git openjdk-8-jdk-headless

# Prepare java-docker configuration
WORKDIR /root
COPY .docker-java.properties .docker-java.properties

# Install syncrepo
RUN git clone https://github.com/indigo-dc/java-syncrepos.git
RUN cd java-syncrepos && git pull && mvn compile

# Prepare default configuration
WORKDIR .indigo-reposync
COPY reposync.properties reposync.properties
COPY reposync-log.properties reposync-log.properties
RUN mkdir logs
RUN touch repolist
WORKDIR /root/java-syncrepos
