package org.project.jirafetchservice.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class EnvLoader implements InitializingBean {

  @Override
  public void afterPropertiesSet() throws Exception {
    // Chercher le fichier .env dans le répertoire parent du projet
    Path envPath = Paths.get(".env");

    // Si .env n'existe pas dans le répertoire courant, chercher dans le parent
    if (!Files.exists(envPath)) {
      envPath = Paths.get("../.env");
    }

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
              // Ne pas écraser les variables déjà définies
              if (System.getProperty(key) == null && System.getenv(key) == null) {
                System.setProperty(key, value);
              }
            }
          }
        }
      } catch (IOException e) {
        System.err.println("Erreur lors du chargement du fichier .env: " + e.getMessage());
      }
    }
  }
}