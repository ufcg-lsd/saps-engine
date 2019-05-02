if [ "$EUID" -ne 0 ]
  then echo "Please run as root"
  exit 1
fi

SAPS_DISPATCHER_HOME=`pwd`

if [ -d "$SAPS_DISPATCHER_HOME" ]; then
  java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n -Dlog4j.configuration="file:$SAPS_DISPATCHER_HOME/config/log4j.properties" -cp "$SAPS_DISPATCHER_HOME/target/saps-engine-0.0.1-SNAPSHOT.jar:$SAPS_DISPATCHER_HOME/target/lib/*" org.fogbowcloud.saps.engine.core.dispatcher.SubmissionDispatcherMain "$SAPS_DISPATCHER_HOME/config/dispatcher.conf"
fi
