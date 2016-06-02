#!/usr/bin/env bash
COMMAND=$1
URL_BASE="http://localhost:8085/v1.0"
case $COMMAND in
  start)
    mvn exec:java -Dexec.mainClass="com.atos.indigo.reposync.Main" -Djava.util.logging.config.file="$HOME/.indigo-reposync/reposync-log.config"&
    ;;

  *)
    ARGS="$@";
    mvn -q exec:java -Dexec.mainClass="com.atos.indigo.reposync.ReposyncClient" -Dexec.args="$ARGS"
    ;;
esac
