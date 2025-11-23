package org.project.chartservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  @Value("${reporting.service.base-url:http://localhost:8082}")
  private String reportingServiceBaseUrl;

  /**
   * Configuration du WebClient avec LoadBalancer pour le profil dev (Spring Cloud)
   */
  @Bean
  @LoadBalanced
  @Profile("dev")
  public WebClient.Builder loadBalancedWebClientBuilder() {
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(30))
            .doOnConnected(
                conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024));
  }

  /**
   * Configuration du WebClient pour le profil dev utilisant la découverte de services
   */
  @Bean("reportingWebClient")
  @Profile("dev")
  public WebClient reportingWebClient(WebClient.Builder loadBalancedWebClientBuilder) {
    return loadBalancedWebClientBuilder
        .baseUrl("http://reporting-service") // Nom du service au lieu de l'URL
        .build();
  }

  /**
   * Configuration du WebClient traditionnel pour les autres profils (prod, test)
   */
  @Bean("reportingWebClient")
  @Profile("!dev")
  public WebClient reportingWebClientDirect() {
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(30))
            .doOnConnected(
                conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));

    return WebClient.builder()
        .baseUrl(reportingServiceBaseUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
        .build();
  }
}
