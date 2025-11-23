FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/email-service-0.0.1-SNAPSHOT.jar email-service.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "email-service.jar"]