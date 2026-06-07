package com.cs.accountsvc.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/**
 * Spring Boot test context used by Cucumber step definitions.
 */
@CucumberContextConfiguration
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.zipkin.autoconfigure.ZipkinAutoConfiguration"
})
@AutoConfigureMockMvc
class CucumberSpringConfiguration {
}
