package org.project.jirafetchservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@EnableDiscoveryClient
public class JiraFetchServiceApplication {

  public static void main(String[] args) {
    // Charger le fichier .env avant de démarrer l'application
    loadEnvFile();
    SpringApplication.run(JiraFetchServiceApplication.class, args);
  }

  private static void loadEnvFile() {
    // Chercher le fichier .env dans différents emplacements
    String[] possiblePaths = {".env", "../.env", "../../.env"};

    for (String pathStr : possiblePaths) {
      Path envPath = Paths.get(pathStr);
      if (Files.exists(envPath)) {
        try (BufferedReader reader = Files.newBufferedReader(envPath, StandardCharsets.UTF_8)) {
          String line;
          while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
              String[] parts = line.split("=", 2);
              if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                // Définir la variable système si elle n'existe pas déjà
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                  System.setProperty(key, value);
                }
              }
            }
          }
          System.out.println("Fichier .env chargé depuis: " + envPath.toAbsolutePath());
          break; // Sortir dès qu'un fichier .env est trouvé
        } catch (IOException e) {
          System.err.println("Erreur lors du chargement du fichier .env: " + e.getMessage());
        }
      }
    }
  }
}