#!/usr/bin/env sh
COMMAND=$1
case $COMMAND in
  start)
    java -cp /usr/share/reposync/lib/reposync.jar com.atos.indigo.reposync.Main
    ;;

  test)
    java -cp /usr/share/reposync/lib/reposync.jar com.atos.indigo.reposync.IntegrationTest
    ;;

  *)
    ARGS="$@";
    java -cp /usr/share/reposync/lib/reposync.jar com.atos.indigo.reposync.ReposyncClient $@
    ;;
esac
