package com.cs.accountsvc.acceptance;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.jupiter.api.DisplayName;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Maven-executable Cucumber acceptance test suite.
 *
 * <p>The suite selects all feature files from {@code src/test/resources/features}
 * and binds them to the Account Service acceptance step package. Report output
 * is configured to match the EventGateway testing strategy: pretty console
 * output plus HTML, JSON, and JUnit XML files under {@code target/cucumber-reports}.</p>
 */
@Suite
@DisplayName("Account Service Cucumber acceptance suite for health, transaction, idempotency, validation, and balance behavior")
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.cs.accountsvc.acceptance")
@ConfigurationParameter(
        key = Constants.PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-reports/cucumber.html, "
                + "json:target/cucumber-reports/cucumber.json, junit:target/cucumber-reports/cucumber.xml"
)
class RunCucumberAcceptanceTest {
}
