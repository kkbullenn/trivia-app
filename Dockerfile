# Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Copy everything into /app
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build the app (generates target/*.jar or *.war)
RUN mvn clean package -DskipTests

# Run the application
FROM eclipse-temurin:17-jdk-alpine

# Create app directory
WORKDIR /app

# Copy the built file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose a port (change if using Tomcat or Spring Boot)
EXPOSE 8080

# Start the app (If this is a standalone app)
#CMD ["java", "-jar", "app.jar"]
