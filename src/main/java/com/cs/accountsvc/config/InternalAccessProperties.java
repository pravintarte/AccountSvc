package com.cs.accountsvc.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the POC internal caller guard on Account Service endpoints.
 *
 * <p>This is intentionally lightweight for the POC. Requests to
 * {@code /accounts/**} must include the configured caller and token headers.
 * This does not replace production-grade mTLS, OAuth2, or network policy; it
 * simply proves the downstream service denies direct calls that do not present
 * Event Gateway's shared internal credentials.</p>
 *
 * @param callerHeader header name carrying the caller service identity
 * @param tokenHeader header name carrying the shared internal token
 * @param allowedCaller expected caller service identity
 * @param token expected shared internal token
 */
@Validated
@ConfigurationProperties(prefix = "account-service.internal-access")
public record InternalAccessProperties(
        @NotBlank String callerHeader,
        @NotBlank String tokenHeader,
        @NotBlank String allowedCaller,
        @NotBlank String token
) {
}
