FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/chart-service-0.0.1-SNAPSHOT.jar chart-service.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "chart-service.jar"]