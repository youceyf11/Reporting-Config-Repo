package org.project.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "eureka.client.enabled=false",
      "eureka.client.register-with-eureka=false",
      "eureka.client.fetch-registry=false",
      "spring.cloud.discovery.enabled=false",
      "spring.cloud.discovery.reactive.enabled=false",
      "spring.cloud.gateway.discovery.locator.enabled=false",
      "spring.cloud.service-registry.auto-registration.enabled=false",
      "spring.main.web-application-type=reactive"
    })
class GatewayServiceApplicationTests {

  @Test
  void contextLoads() {}
}
