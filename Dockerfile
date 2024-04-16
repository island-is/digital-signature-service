FROM maven:3.9.1-eclipse-temurin-11 as build

COPY pom.xml /usr/src/mymaven/
COPY dss-demo-bundle/pom.xml /usr/src/mymaven/dss-demo-bundle/
# COPY dss-demo-bundle/src /usr/src/mymaven/dss-demo-bundle/src

COPY dss-mock-tsa/pom.xml /usr/src/mymaven/dss-mock-tsa/
COPY dss-mock-tsa/src /usr/src/mymaven/dss-mock-tsa/src

COPY dss-demo-webapp/pom.xml /usr/src/mymaven/dss-demo-webapp/
COPY dss-demo-webapp/src /usr/src/mymaven/dss-demo-webapp/src

WORKDIR /usr/src/mymaven

RUN mvn clean package -pl dss-mock-tsa,dss-demo-webapp -P quick


FROM tomcat:9

RUN apt-get update && apt-get upgrade -y

RUN wget -O dd-java-agent.jar https://dtdg.co/latest-java-tracer && \
    wget -O /usr/local/tomcat/bin/jul-ecs-formatter-1.6.0.jar https://repo1.maven.org/maven2/co/elastic/logging/jul-ecs-formatter/1.6.0/jul-ecs-formatter-1.6.0.jar && \
    wget -O /usr/local/tomcat/bin/ecs-logging-core-1.6.0.jar https://repo1.maven.org/maven2/co/elastic/logging/ecs-logging-core/1.6.0/ecs-logging-core-1.6.0.jar

COPY setenv.sh /usr/local/tomcat/bin

COPY --from=build /usr/src/mymaven/dss-demo-webapp/target/dss-demo-webapp-*.war /usr/local/tomcat/webapps/ROOT.war
COPY dss-demo-webapp/src/main/resources/config/logging.properties /usr/local/tomcat/conf/logging.properties

EXPOSE 8080
