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
import lombok.extern.slf4j.Slf4j;

/**
 * Persisted account transaction keyed by upstream event id.
 *
 * <p>This entity is the Account Service ledger table. The {@code eventId}
 * column is the primary key because Event Gateway sends one transaction apply
 * request per upstream event, and Account Service must safely deduplicate
 * retries using that event identity.</p>
 *
 * <p>Only normalized data required for balance calculation and account detail
 * reads is stored here. The original metadata payload is intentionally excluded
 * from this skeleton record because it is not needed to calculate balances.</p>
 */
@Slf4j
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

    /**
     * Builds a persistence entity from a validated transaction request.
     *
     * <p>The factory centralizes normalization rules before persistence. Today
     * that means converting currency to uppercase and carrying the optional
     * Event Gateway idempotency header into the stored record for audit/debug
     * purposes.</p>
     *
     * @param accountId account id from the request path
     * @param request validated transaction request body
     * @param idempotencyKey optional {@code Idempotency-Key} header value
     * @param createdAt timestamp assigned by the service clock
     * @return initialized account transaction entity ready for repository save
     */
    public static AccountTransactionRecord from(
            String accountId,
            AccountTransactionRequest request,
            String idempotencyKey,
            Instant createdAt
    ) {
        log.debug(
                "Creating account transaction record accountId={} eventId={} type={} amount={} currency={} createdAt={}",
                accountId,
                request.eventId(),
                request.type(),
                request.amount(),
                request.currency(),
                createdAt
        );
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
