package com.cs.accountsvc.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Lightweight public health response.
 *
 * <p>This payload backs both {@code GET /health} and {@code GET /healthcheck}.
 * It is intentionally smaller than Actuator health output so load balancers,
 * smoke checks, and simple clients can validate service liveness without
 * depending on internal component details.</p>
 *
 * @param status service status
 * @param service service name
 * @param timestamp server-side UTC timestamp
 * @param diagnostics basic component diagnostics
 */
public record HealthResponse(String status, String service, Instant timestamp, Map<String, String> diagnostics) {
}
