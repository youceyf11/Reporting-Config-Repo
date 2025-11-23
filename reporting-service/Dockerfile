FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/reporting-service-0.0.1-SNAPSHOT.jar reporting-service.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "reporting-service.jar"]