# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Run stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S apilens && adduser -S apilens -G apilens
COPY --from=build /app/target/*.jar app.jar
USER apilens
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
