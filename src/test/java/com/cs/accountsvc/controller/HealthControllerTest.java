package com.cs.accountsvc.controller;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import com.cs.accountsvc.dto.ApiResponse;
import com.cs.accountsvc.dto.HealthResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for public health endpoints.
 */
@Slf4j
@DisplayName("HealthController public health response behavior")
class HealthControllerTest {

    @Test
    @DisplayName("health returns HTTP 200, HEALTH_OK, UP status, and database diagnostics")
    void health_whenDatabaseConnectionIsValid_returnsOkEnvelopeWithHealthOkCodeAndUpPayload() throws Exception {
        log.info("Testing public health endpoint response envelope");
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(1)).thenReturn(true);
        HealthController controller = new HealthController(
                Clock.fixed(Instant.parse("2026-06-06T21:00:00Z"), ZoneOffset.UTC),
                dataSource
        );
        ReflectionTestUtils.setField(controller, "serviceName", "account-service");

        ResponseEntity<ApiResponse<HealthResponse>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("HEALTH_OK");
        assertThat(response.getBody().data().service()).isEqualTo("account-service");
        assertThat(response.getBody().data().status()).isEqualTo("UP");
        assertThat(response.getBody().data().diagnostics()).containsEntry("database", "UP");
        log.info("Verified public health endpoint response envelope");
    }
}
