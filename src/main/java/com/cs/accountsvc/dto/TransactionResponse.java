package com.cs.accountsvc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response returned after applying or deduplicating a transaction.
 */
public record TransactionResponse(
        UUID eventId,
        String accountId,
        EventType type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp,
        boolean duplicate,
        Instant appliedAt
) {
}
