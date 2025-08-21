package org.project.gatewayservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "eureka.client.enabled=false",
      "spring.cloud.gateway.discovery.locator.enabled=false"
    })
class GatewayConfigurationTest {

  @Autowired private ApplicationContext applicationContext;

  @Autowired private GatewayProperties gatewayProperties;

  @Test
  void shouldLoadApplicationContext() {
    assertThat(applicationContext).isNotNull();
  }

  @Test
  void shouldHaveGatewayProperties() {
    assertThat(gatewayProperties).isNotNull();
  }

  @Test
  void shouldHaveGatewayBeans() {
    assertThat(applicationContext.containsBean("routePredicateHandlerMapping")).isTrue();
    assertThat(applicationContext.containsBean("gatewayProperties")).isTrue();
  }

  @Test
  void shouldConfigureDefaultRoutes() {
    var routes = gatewayProperties.getRoutes();
    assertThat(routes).isNotNull();
    // Routes configuration should be available even if empty
  }

  @Test
  void shouldConfigureDefaultFilters() {
    var filters = gatewayProperties.getDefaultFilters();
    assertThat(filters).isNotNull();
    // Filters configuration should be available even if empty
  }
}
