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

RUN wget -O dd-java-agent.jar https://dtdg.co/latest-java-tracer
COPY setenv.sh /usr/local/tomcat/bin

COPY --from=build /usr/src/mymaven/dss-demo-webapp/target/dss-demo-webapp-*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
