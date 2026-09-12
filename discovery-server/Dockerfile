# Build context is the repo root (see docker-compose.yml). Maven needs every module's pom.xml
# on disk to resolve the reactor even though -pl restricts what actually gets *built* - so
# every Dockerfile in this project copies the whole source tree, then narrows the build with
# `-pl <module> -am` ("also make" any modules it depends on).
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-events ./common-events
COPY discovery-server ./discovery-server
COPY config-server ./config-server
COPY order-service ./order-service
COPY inventory-service ./inventory-service
COPY notification-service ./notification-service
RUN mvn -q -pl discovery-server -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/discovery-server/target/discovery-server.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app.jar"]
