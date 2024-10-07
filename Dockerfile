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

FROM openjdk:17

VOLUME /tmp

ARG JAR_FILE=/workspace/app/whiteRabbitService/target/*.jar

RUN microdnf module enable postgresql:13
RUN microdnf install postgresql.x86_64

COPY --chown=docker:docker --chmod=711 init.sh .
COPY --from=build ${JAR_FILE} app.jar

EXPOSE 8000

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar ${0} ${@}"]