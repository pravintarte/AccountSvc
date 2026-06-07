package com.cs.accountsvc.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.cs.accountsvc.dto.ApiResponse;
import com.cs.accountsvc.dto.HealthResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for public health endpoints.
 *
 * <p>The health controller is intentionally lightweight and independent from
 * Actuator. This test verifies the public response envelope used by load
 * balancers, smoke checks, and simple clients.</p>
 */
@Slf4j
@DisplayName("HealthController public health response behavior")
class HealthControllerTest {

    /**
     * Verifies that the health endpoint returns the stable {@code HEALTH_OK}
     * response code and an {@code UP} payload.
     */
    @Test
    @DisplayName("health returns HTTP 200, HEALTH_OK, and UP status in the public response envelope")
    void health_whenServiceIsRunning_returnsOkEnvelopeWithHealthOkCodeAndUpPayload() {
        log.info("Testing public health endpoint response envelope");
        HealthController controller = new HealthController(Clock.fixed(
                Instant.parse("2026-06-06T21:00:00Z"),
                ZoneOffset.UTC
        ));
        ReflectionTestUtils.setField(controller, "serviceName", "account-service");

        ResponseEntity<ApiResponse<HealthResponse>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("HEALTH_OK");
        assertThat(response.getBody().data().service()).isEqualTo("account-service");
        assertThat(response.getBody().data().status()).isEqualTo("UP");
        log.info("Verified public health endpoint response envelope");
    }
}
