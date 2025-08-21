package org.project.discoveryservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "eureka.client.register-with-eureka=false",
      "eureka.client.fetch-registry=false",
      "spring.cloud.config.enabled=false"
    })
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.cloud.compatibility-verifier.enabled=false")
class DiscoveryServerTest {

  @Autowired private HealthEndpoint healthEndpoint;

  @Test
  @DisplayName("Health endpoint should report UP")
  void healthEndpointUp() {
    var health = healthEndpoint.health();
    assertThat(health.getStatus().getCode()).isEqualTo("UP");
  }
}
