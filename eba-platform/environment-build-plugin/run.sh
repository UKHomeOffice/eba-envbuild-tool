#!/bin/sh -x

# Script for running Jenkins with the EBSA Environment Build Plugin

if [ "$MAVEN_CMD" = "" ]
then
	# I need to point mvn at a different settings file
	# so I can do this by setting the environment variable
	# export MAVEN_CMD = 'mvn -s /my/settings.xml'
	# In other cases, just calls maven
	MAVEN_CMD="mvn"
fi

export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=5005,suspend=n"
# export MAVEN_OPTS=""

$MAVEN_CMD hpi:run -Djetty.port=8090
