package com.cs.accountsvc.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/**
 * Spring Boot test context used by Cucumber step definitions.
 *
 * <p>This configuration starts the Account Service application context with
 * MockMvc so acceptance steps exercise the real controller, validation,
 * service, repository, serialization, structured logging, and datasource
 * configuration paths. Eureka and Zipkin are disabled because acceptance tests
 * should not require external infrastructure.</p>
 */
@CucumberContextConfiguration
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.zipkin.autoconfigure.ZipkinAutoConfiguration"
})
@AutoConfigureMockMvc
class CucumberSpringConfiguration {
}
