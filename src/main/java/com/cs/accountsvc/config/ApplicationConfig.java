package com.cs.accountsvc.config;

import java.time.Clock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide bean configuration for infrastructure concerns.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AccountServiceProperties.class)
public class ApplicationConfig {

    /**
     * Provides a UTC clock for timestamp creation and deterministic testing.
     *
     * @return system UTC clock
     */
    @Bean
    public Clock clock() {
        log.info("Configuring system UTC clock");
        return Clock.systemUTC();
    }
}
