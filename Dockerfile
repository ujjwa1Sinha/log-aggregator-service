# Stage 1: Build the application
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create runtime image
FROM eclipse-temurin:17-jdk
VOLUME /tmp
WORKDIR /app

# Copy built JAR
COPY --from=build /app/target/*.jar app.jar

# Expose port (change to your Spring Boot port if needed)
EXPOSE 8080

# Entry point
ENTRYPOINT ["java", "-jar", "app.jar"]
