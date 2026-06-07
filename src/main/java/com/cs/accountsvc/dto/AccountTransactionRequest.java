package com.cs.accountsvc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Transaction request accepted from Event Gateway.
 *
 * @param eventId upstream event identifier
 * @param type CREDIT or DEBIT
 * @param amount positive transaction amount
 * @param currency ISO-style currency code
 * @param eventTimestamp original event occurrence time
 * @param metadata optional upstream context
 */
public record AccountTransactionRequest(
        @NotNull UUID eventId,
        @NotNull EventType type,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull Instant eventTimestamp,
        Map<String, Object> metadata
) {
}
