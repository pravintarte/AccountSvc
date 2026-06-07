package com.cs.accountsvc.controller;

import java.time.Clock;
import java.time.Instant;

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
        ResponseEntity<ApiResponse<HealthResponse>> response = ResponseEntity.ok(new ApiResponse<>(
                now,
                HttpStatus.OK.value(),
                ApiCodes.HEALTH_OK,
                "Service is healthy.",
                new HealthResponse("UP", serviceName, now)
        ));
        log.debug("Completed health request serviceName={} status={} timestamp={}", serviceName, "UP", now);
        return response;
    }
}
