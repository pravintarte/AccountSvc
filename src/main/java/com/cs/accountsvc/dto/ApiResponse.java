package com.cs.accountsvc.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard successful API response envelope.
 *
 * @param timestamp server-side UTC timestamp
 * @param status HTTP status code
 * @param code stable application response code
 * @param description human-readable response description
 * @param data response payload
 * @param <T> response payload type
 */
@Schema(description = "Standard successful API response envelope.")
public record ApiResponse<T>(
        @Schema(description = "Server-side UTC timestamp.", example = "2026-06-06T21:00:00Z")
        Instant timestamp,

        @Schema(description = "HTTP status code.", example = "201")
        int status,

        @Schema(description = "Stable application response code.", example = ApiCodes.TRANSACTION_APPLIED)
        String code,

        @Schema(description = "Human-readable response description.", example = "Transaction was applied.")
        String description,

        @Schema(description = "Response payload.")
        T data
) {
}
