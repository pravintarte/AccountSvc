package com.cs.accountsvc.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response returned after applying or deduplicating a transaction.
 *
 * <p>This payload is returned by {@code POST /accounts/{accountId}/transactions}.
 * It echoes the transaction facts that were stored or matched and indicates
 * whether the request was a safe duplicate retry instead of a new ledger write.</p>
 *
 * @param eventId upstream event id used as the idempotency key
 * @param accountId account id receiving the transaction
 * @param type transaction direction
 * @param amount transaction amount
 * @param currency normalized transaction currency
 * @param eventTimestamp original business timestamp from the upstream event
 * @param duplicate {@code true} when an exact duplicate event was acknowledged
 * @param appliedAt server timestamp when the original transaction was stored
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
