# First stage: build the application with maven
FROM maven:3.9.7-eclipse-temurin-21 AS build
COPY . /app
WORKDIR /app
RUN mvn clean install -P quick

# Second stage: create and run JAVA app
FROM eclipse-temurin:21

ADD 'https://dtdg.co/latest-java-tracer' /dd-java-agent.jar

COPY --from=build /app/target/*.jar /app.jar
ENTRYPOINT java -javaagent:/dd-java-agent.jar -jar /app.jar