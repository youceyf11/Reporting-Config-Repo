FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy the built jar (versioned SNAPSHOT) into the image
COPY target/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
