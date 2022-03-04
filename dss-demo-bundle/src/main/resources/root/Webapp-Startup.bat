@echo off

set JRE_HOME=.\java
set CATALINA_HOME=.\${tomcat.root.folder}
export CATALINA_OPTS="$CATALINA_OPTS -javaagent:/dd-java-agent.jar"
.\${tomcat.root.folder}\bin\startup.bat