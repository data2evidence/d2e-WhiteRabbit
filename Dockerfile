FROM openjdk:17 as build

WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY lib lib

COPY rabbit-core rabbit-core
COPY rabbitinahat rabbitinahat
COPY whiterabbit whiterabbit
COPY whiteRabbitService whiteRabbitService

RUN tr -d '\015' <./mvnw >./mvnw.sh && mv ./mvnw.sh ./mvnw && chmod 770 mvnw
RUN ./mvnw dependency:go-offline -B
RUN ./mvnw -B clean package

FROM debian:bookworm

# Set DISPLAY variable
ENV DISPLAY=:1

RUN apt-get update && apt-get install -y \
    postgresql-client \
    openjdk-17-jdk \
    xvfb

ARG JAR_FILE=/workspace/app/whiteRabbitService/target/*.jar

COPY --chown=docker:docker --chmod=711 init.sh .
COPY --from=build ${JAR_FILE} app.jar

# COPY whiteRabbitService/target/*.jar app.jar # required for local development

EXPOSE 8000

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar ${0} ${@}"]