package org.project.discoveryservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        // Keep Eureka client enabled so required beans are created, but don't call network
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        // Avoid external config lookup during tests
        "spring.cloud.config.enabled=false",
        // Disable Cloud/Boot compatibility check in tests
        "spring.cloud.compatibility-verifier.enabled=false"
    }
)
class DiscoveryServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
