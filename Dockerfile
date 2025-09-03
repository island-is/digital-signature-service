# First stage: build the application with maven
FROM maven:3.9.11-eclipse-temurin-21 AS build

COPY . /home/island-is/app
WORKDIR /home/island-is/app
RUN mvn package -P quick

# Second stage: create and run JAVA app
FROM eclipse-temurin:21

RUN useradd -m island-is -d /home/island-is
WORKDIR /home/island-is/app

ADD 'https://dtdg.co/latest-java-tracer' dd-java-agent.jar
COPY --from=build /home/island-is/app/target/*.jar app.jar

RUN chown -R island-is:island-is /home/island-is/app

USER island-is

ENTRYPOINT ["java", "-javaagent:dd-java-agent.jar", "-jar", "app.jar"]