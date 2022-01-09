ARG ARCH=
FROM gradle:7.0.2-jdk11 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN apt-get update
RUN apt-get install haveged -y
RUN gradle build --no-daemon

FROM ${ARCH}openjdk:11-jre-slim

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs/application.jar /app/application.jar

ENTRYPOINT ["java", "-jar","/app/application.jar"]
