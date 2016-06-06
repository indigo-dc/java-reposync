#!/usr/bin/env sh
COMMAND=$1
case $COMMAND in
  start)
    java -cp /lib/reposync.jar com.atos.indigo.reposync.Main -Djava.util.logging.config.file="$HOME/.indigo-reposync/reposync-log.properties"
    ;;

  *)
    ARGS="$@";
    java -cp /lib/reposync.jar com.atos.indigo.reposync.ReposyncClient $@
    ;;
esac
