FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-events ./common-events
COPY discovery-server ./discovery-server
COPY config-server ./config-server
COPY order-service ./order-service
COPY inventory-service ./inventory-service
COPY notification-service ./notification-service
RUN mvn -q -pl config-server -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/config-server/target/config-server.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app.jar"]
