package com.cs.accountsvc.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import javax.sql.DataSource;

import com.cs.accountsvc.dto.ApiCodes;
import com.cs.accountsvc.dto.ApiResponse;
import com.cs.accountsvc.dto.HealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public health endpoints matching the gateway contract.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final Clock clock;
    private final DataSource dataSource;

    @Value("${spring.application.name:account-service}")
    private String serviceName;

    /**
     * Returns a lightweight health response for clients/load balancers.
     *
     * @return health response
     */
    @GetMapping({"/health", "/healthcheck"})
    public ResponseEntity<ApiResponse<HealthResponse>> health() {
        log.debug("Received health request");
        Instant now = Instant.now(clock);
        String databaseStatus = databaseStatus();
        String serviceStatus = "UP".equals(databaseStatus) ? "UP" : "DOWN";
        ResponseEntity<ApiResponse<HealthResponse>> response = ResponseEntity.ok(new ApiResponse<>(
                now,
                HttpStatus.OK.value(),
                ApiCodes.HEALTH_OK,
                "Service is healthy.",
                new HealthResponse(serviceStatus, serviceName, now, Map.of("database", databaseStatus))
        ));
        log.debug("Completed health request serviceName={} status={} databaseStatus={} timestamp={}",
                serviceName, serviceStatus, databaseStatus, now);
        return response;
    }

    private String databaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1) ? "UP" : "DOWN";
        } catch (SQLException ex) {
            log.warn("Health database check failed", ex);
            return "DOWN";
        }
    }
}
