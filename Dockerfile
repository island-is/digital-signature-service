FROM maven:3.8.1-jdk-11 as BUILD

# COPY dss-demo-bundle/src /usr/src/mymaven/dss-demo-bundle/src

COPY pom.xml /usr/src/mymaven/
COPY dss-demo-bundle/pom.xml /usr/src/mymaven/dss-demo-bundle/
COPY dss-rest-doc-generation/pom.xml /usr/src/mymaven/dss-rest-doc-generation/
COPY sscd-mocca-adapter/pom.xml /usr/src/mymaven/sscd-mocca-adapter/

COPY dss-mock-tsa/pom.xml /usr/src/mymaven/dss-mock-tsa/
COPY dss-mock-tsa/src /usr/src/mymaven/dss-mock-tsa/src

COPY dss-demo-webapp/pom.xml /usr/src/mymaven/dss-demo-webapp/
COPY dss-demo-webapp/src /usr/src/mymaven/dss-demo-webapp/src

COPY dss-standalone-app/pom.xml /usr/src/mymaven/dss-standalone-app/
COPY dss-standalone-app/src /usr/src/mymaven/dss-standalone-app/src

COPY dss-standalone-app-package/pom.xml /usr/src/mymaven/dss-standalone-app-package/
COPY dss-standalone-app-package/src /usr/src/mymaven/dss-standalone-app-package/src

WORKDIR /usr/src/mymaven

RUN mvn clean package -pl dss-mock-tsa,dss-standalone-app,dss-standalone-app-package,dss-demo-webapp


FROM tomcat:9

RUN apt-get update && apt-get upgrade -y

RUN wget -O dd-java-agent.jar https://dtdg.co/latest-java-tracer
COPY setenv.sh /usr/local/tomcat/bin

COPY --from=BUILD /usr/src/mymaven/dss-demo-webapp/target/dss-demo-webapp-*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
