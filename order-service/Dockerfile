# Build stage
FROM maven:3.9-eclipse-temurin-24-alpine AS build
WORKDIR /app

# Copy parent POM
COPY pom.xml .

# Copy all module POMs and sources
COPY common/pom.xml ./common/
COPY common/src ./common/src
COPY order-service/pom.xml ./order-service/
COPY order-service/src ./order-service/src
COPY payment-service/pom.xml ./payment-service/
COPY payment-service/src ./payment-service/src
COPY api-gateway/pom.xml ./api-gateway/
COPY api-gateway/src ./api-gateway/src

# Build only the order-service module and its dependencies
RUN mvn clean package -DskipTests -pl order-service -am

# Runtime stage
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/order-service/target/order-service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
