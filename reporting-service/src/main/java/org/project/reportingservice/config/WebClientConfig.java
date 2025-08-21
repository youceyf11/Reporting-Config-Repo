package org.project.reportingservice.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Configuration pour WebClient permettant de communiquer avec jira-fetch-service Configure les
 * timeouts, retry policies et stratégies d'échange
 */
@Configuration
public class WebClientConfig {

  @Value("${jira.fetch.base-url}")
  private String jiraFetchBaseUrl;

  @Value("${jira.fetch.timeout:30s}")
  private Duration timeout;

  @Value("${jira.fetch.max-retries:3}")
  private Integer maxRetries;

  /**
   * Configuration du WebClient principal pour communiquer avec jira-fetch-service
   *
   * @return WebClient configuré avec timeouts et stratégies d'échange
   */
  @Bean
  public WebClient jiraFetchWebClient() {

    // Configuration du pool de connexions
    ConnectionProvider connectionProvider =
        ConnectionProvider.builder("jira-fetch-pool")
            .maxConnections(50)
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofSeconds(60))
            .pendingAcquireTimeout(Duration.ofSeconds(10))
            .build();

    // Configuration du client HTTP
    HttpClient httpClient =
        HttpClient.create(connectionProvider).responseTimeout(timeout).followRedirect(true);

    // Configuration des stratégies d'échange pour gérer les gros payloads
    ExchangeStrategies exchangeStrategies =
        ExchangeStrategies.builder()
            .codecs(
                configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
            .build();

    return WebClient.builder()
        .baseUrl(jiraFetchBaseUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(exchangeStrategies)
        .defaultHeader("Content-Type", "application/json")
        .defaultHeader("Accept", "application/json")
        .build();
  }

  /**
   * Configuration d'un WebClient générique pour d'autres services
   *
   * @return WebClient générique
   */
  @Bean
  public WebClient genericWebClient() {
    return WebClient.builder()
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs(
                    configurer ->
                        configurer.defaultCodecs().maxInMemorySize(8 * 1024 * 1024)) // 8MB
                .build())
        .build();
  }
}
