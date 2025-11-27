FROM eclipse-temurin:21-jdk
WORKDIR /app
RUN apt-get update && apt-get install -y git
COPY target/config-server-0.0.1-SNAPSHOT.jar config-server.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "config-server.jar"]
