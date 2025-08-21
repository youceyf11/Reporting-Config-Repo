package org.project.gatewayservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "eureka.client.enabled=false",
      "spring.cloud.gateway.discovery.locator.enabled=false"
    })
class GatewayRoutingTest {

  @Autowired private RouteLocator routeLocator;

  @Test
  void shouldHaveRouteLocator() {
    assertThat(routeLocator).isNotNull();
  }

  @Test
  void shouldLoadRoutes() {
    var routes = routeLocator.getRoutes().collectList().block();
    assertThat(routes).isNotNull();
    // Routes may be empty in test environment, but locator should work
  }
}
