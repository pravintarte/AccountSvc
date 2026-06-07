package com.cs.accountsvc.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Runtime settings for account transaction reads.
 *
 * <p>The values in this record are bound from {@code application.yml} using
 * Spring Boot configuration properties. Keeping these values in a typed record
 * makes runtime configuration explicit and validates important limits before
 * the service starts accepting requests.</p>
 *
 * @param recentLimit maximum number of recent transactions returned by account
 *                    detail reads; must be positive so account detail responses
 *                    always return a deterministic bounded history window
 */
@Validated
@ConfigurationProperties(prefix = "account-service.transactions")
public record AccountServiceProperties(
        @Min(1) int recentLimit
) {
}
