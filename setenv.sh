export CATALINA_OPTS="$CATALINA_OPTS -javaagent:./dd-java-agent.jar"
# Appending the class path with the ECS logging jars accessible in the bin directory for java logging (json formatting tomcat / catalina output)
export CLASSPATH="$CLASSPATH:/usr/local/tomcat/bin/ecs-logging-core-1.6.0.jar:/usr/local/tomcat/bin/jul-ecs-formatter-1.6.0.jar"
