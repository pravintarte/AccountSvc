package com.cs.accountsvc.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Runtime settings for account transaction reads.
 *
 * @param recentLimit maximum number of recent transactions returned by account detail reads
 */
@Validated
@ConfigurationProperties(prefix = "account-service.transactions")
public record AccountServiceProperties(
        @Min(1) int recentLimit
) {
}
