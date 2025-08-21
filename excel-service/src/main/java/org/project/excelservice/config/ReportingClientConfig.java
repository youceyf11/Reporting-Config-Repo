package org.project.excelservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ReportingClientConfig {

  @Bean
  public WebClient reportingWebClient(@Value("${reporting.base-url}") String baseUrl) {
    return WebClient.builder().baseUrl(baseUrl).build();
  }
}
