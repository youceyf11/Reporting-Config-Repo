FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/issue-events-0.0.1-SNAPSHOT.jar issue-events.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "issue-events.jar"]