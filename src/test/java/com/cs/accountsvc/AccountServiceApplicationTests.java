package com.cs.accountsvc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that the complete Spring application context can be created.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false"
})
class AccountServiceApplicationTests {

    @Test
    void contextLoads_whenApplicationConfigurationIsValid_startsCompleteSpringBootContext() {
    }
}
