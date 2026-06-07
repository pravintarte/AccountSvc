package com.cs.accountsvc.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.cs.accountsvc.dto.AccountTransactionRequest;
import com.cs.accountsvc.dto.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persisted account transaction keyed by upstream event id.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "account_transaction")
public class AccountTransactionRecord {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "account_id", nullable = false, length = 128)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 16)
    private EventType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static AccountTransactionRecord from(
            String accountId,
            AccountTransactionRequest request,
            String idempotencyKey,
            Instant createdAt
    ) {
        AccountTransactionRecord record = new AccountTransactionRecord();
        record.setEventId(request.eventId());
        record.setAccountId(accountId);
        record.setType(request.type());
        record.setAmount(request.amount());
        record.setCurrency(request.currency().toUpperCase());
        record.setEventTimestamp(request.eventTimestamp());
        record.setIdempotencyKey(idempotencyKey);
        record.setCreatedAt(createdAt);
        return record;
    }
}
