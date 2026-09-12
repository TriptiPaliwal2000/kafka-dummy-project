FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common-events ./common-events
COPY discovery-server ./discovery-server
COPY config-server ./config-server
COPY order-service ./order-service
COPY inventory-service ./inventory-service
COPY notification-service ./notification-service
# -am ("also make") builds common-events first and installs it into the local repo so
# order-service's own build can resolve it as a dependency.
RUN mvn -q -pl common-events,order-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/order-service/target/order-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
