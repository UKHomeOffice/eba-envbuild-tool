@echo off
set JAR=vCloudClient-1.0.0-SNAPSHOT.jar
set COMMAND="execute"
set USER=559.76.c09964
set PWD=PWD
set DEFINITION=../IPT_ST_COLLAB1.xml
set EXEC=../IPT_ST_COLLAB1_ExecPlan.xml
set CONFIG=config/il2-devtest-basic-noproxy.properties
call java -jar %JAR% -command=%COMMAND% -user=%USER% -password=%PWD%  -definition=%DEFINITION% -executionplan=%EXEC% -config=%CONFIG%