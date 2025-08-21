package org.project.emailservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmailProviderConfig.EmailProviderProperties.class)
public class EmailProviderConfig {

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .build();

    return CircuitBreakerRegistry.of(config);
  }

  @Bean
  public CircuitBreaker gmailCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("gmail");
  }

  @Data
  @ConfigurationProperties(prefix = "email.providers")
  public static class EmailProviderProperties {
    private Gmail gmail = new Gmail();
    private Sendgrid sendgrid = new Sendgrid();

    @Data
    public static class Gmail {
      private String host = "smtp.gmail.com";
      private int port = 587;
      private String username;
      private String password;
      private boolean enabled = true;
    }

    @Data
    public static class Sendgrid {
      private String apiKey;
      private boolean enabled = false;
    }
  }
}
