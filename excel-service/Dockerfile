FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/excel-service-0.0.1-SNAPSHOT.jar excel-service.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "excel-service.jar"]