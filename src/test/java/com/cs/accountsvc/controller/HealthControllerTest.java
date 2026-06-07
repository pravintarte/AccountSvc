package com.cs.accountsvc.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.cs.accountsvc.dto.ApiResponse;
import com.cs.accountsvc.dto.HealthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for public health endpoints.
 */
class HealthControllerTest {

    @Test
    void health_returnsUpEnvelope() {
        HealthController controller = new HealthController(Clock.fixed(
                Instant.parse("2026-06-06T21:00:00Z"),
                ZoneOffset.UTC
        ));

        ResponseEntity<ApiResponse<HealthResponse>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("HEALTH_OK");
        assertThat(response.getBody().data().status()).isEqualTo("UP");
    }
}
