# Use official Tomcat image with JDK 17
FROM tomcat:10.1-jdk17

# Set working directory
WORKDIR /usr/local/tomcat

# Optional: remove default Tomcat apps (cleaner deployment)
RUN rm -rf webapps/*

# Copy your WAR file (after building with Maven)
# This will place your app inside Tomcat's webapps directory
COPY target/trivia-app.war webapps/trivia-app.war

# Expose the Tomcat default port
EXPOSE 8080

# Normaly you'd want CMD for stand alone apps
# Tomcat image already includes this CMD by default:
# CMD ["catalina.sh", "run"]
