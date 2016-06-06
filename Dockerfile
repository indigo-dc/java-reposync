FROM alpine:latest
MAINTAINER Jose Antonio Sanchez <jose.sanchezm@atos.net>

# Install java
RUN apk --no-cache --update add openjdk8-jre-base

# Prepare java-docker configuration
WORKDIR /root
COPY .docker-java.properties .docker-java.properties

# Prepare default configuration
WORKDIR /root/.indigo-reposync
COPY reposync.properties reposync.properties
COPY reposync-log.properties reposync-log.properties
RUN mkdir logs
RUN touch repolist

# Install syncrepo
COPY target/reposync-1.0-SNAPSHOT-jar-with-dependencies.jar /lib/reposync.jar
COPY reposync.sh /bin/reposync
ENTRYPOINT ["reposync", "start"]

