package org.project.excelservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ReportingClientConfig {

  @Value("${reporting.base-url:http://localhost:8082}")
  private String reportingBaseUrl;

  /**
   * Configuration du WebClient avec LoadBalancer pour le profil dev (Spring Cloud)
   */
  @Bean
  @LoadBalanced
  @Profile("dev")
  public WebClient.Builder loadBalancedWebClientBuilder() {
    return WebClient.builder();
  }

  /**
   * Configuration du WebClient pour le profil dev utilisant la découverte de services
   */
  @Bean
  @Profile("dev")
  public WebClient reportingWebClient(WebClient.Builder loadBalancedWebClientBuilder) {
    return loadBalancedWebClientBuilder
        .baseUrl("http://reporting-service") // Nom du service au lieu de l'URL
        .build();
  }

  /**
   * Configuration du WebClient traditionnel pour les autres profils (prod, test)
   */
  @Bean
  @Profile("!dev")
  public WebClient reportingWebClientProd() {
    return WebClient.builder().baseUrl(reportingBaseUrl).build();
  }
}
