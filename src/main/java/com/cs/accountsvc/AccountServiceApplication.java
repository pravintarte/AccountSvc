package com.cs.accountsvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import lombok.extern.slf4j.Slf4j;

/**
 * Account Service Spring Boot entry point.
 *
 * <p>This class is intentionally small: it delegates all runtime bootstrap
 * behavior to Spring Boot while anchoring component scanning at the
 * {@code com.cs.accountsvc} package. The service exposes the downstream
 * account contract consumed by Event Gateway, including transaction apply,
 * balance reads, account detail reads, structured logging, tracing, Eureka
 * registration, and Hikari-backed persistence.</p>
 */
@Slf4j
@SpringBootApplication
public class AccountServiceApplication {

    /**
     * Starts the Account Service process using Spring Boot auto-configuration.
     *
     * @param args command-line arguments passed by the Java process or container entrypoint
     */
    public static void main(String[] args) {
        log.info("Starting Account Service application bootstrap argumentCount={}", args.length);
        SpringApplication.run(AccountServiceApplication.class, args);
        log.info("Account Service application bootstrap completed");
    }
}
