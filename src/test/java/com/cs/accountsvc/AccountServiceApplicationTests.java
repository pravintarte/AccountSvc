package com.cs.accountsvc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import lombok.extern.slf4j.Slf4j;

/**
 * Verifies that the complete Spring application context can be created.
 *
 * <p>This smoke test catches invalid configuration properties, missing beans,
 * broken JPA mappings, datasource pool configuration errors, controller wiring
 * failures, and incompatible auto-configuration before the application is run
 * manually.</p>
 */
@Slf4j
@DisplayName("AccountServiceApplication full Spring Boot context startup verification")
@SpringBootTest(properties = {
        "eureka.client.enabled=false"
})
class AccountServiceApplicationTests {

    /**
     * Loads the production component graph with Eureka disabled so the test does
     * not require an external service registry.
     */
    @Test
    @DisplayName("contextLoads starts the complete Account Service Spring context with local test properties")
    void contextLoads_whenApplicationConfigurationIsValid_startsCompleteSpringBootContext() {
        log.info("Verified Account Service Spring application context loaded successfully");
    }
}
