package com.cs.accountsvc.config;

import java.time.Clock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide bean configuration for infrastructure concerns.
 *
 * <p>This configuration currently exposes a UTC {@link Clock}. Centralizing the
 * clock keeps controller/service timestamp creation consistent in production
 * and easy to replace in tests if deterministic time is required.</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({AccountServiceProperties.class, InternalAccessProperties.class})
public class ApplicationConfig {

    /**
     * Provides a UTC clock for timestamp creation and deterministic testing.
     *
     * <p>All API response envelopes, health responses, and persisted transaction
     * creation timestamps should use this clock rather than calling
     * {@link java.time.Instant#now()} directly. That keeps time behavior
     * consistent and testable across the service.</p>
     *
     * @return system UTC clock
     */
    @Bean
    public Clock clock() {
        log.info("Configuring system UTC clock");
        return Clock.systemUTC();
    }
}
