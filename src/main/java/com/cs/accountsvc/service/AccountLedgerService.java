package com.cs.accountsvc.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.cs.accountsvc.config.AccountServiceProperties;
import com.cs.accountsvc.dto.AccountDetailsResponse;
import com.cs.accountsvc.dto.AccountTransactionRequest;
import com.cs.accountsvc.dto.BalanceResponse;
import com.cs.accountsvc.dto.EventType;
import com.cs.accountsvc.dto.TransactionResponse;
import com.cs.accountsvc.entity.AccountTransactionRecord;
import com.cs.accountsvc.exception.AccountNotFoundException;
import com.cs.accountsvc.exception.DuplicateTransactionConflictException;
import com.cs.accountsvc.repository.AccountTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates Account Service ledger operations for the Event Gateway contract.
 *
 * <p>This service owns the core skeleton behavior for account transactions:
 * idempotent transaction application by upstream event id, duplicate-conflict
 * detection when an event id is reused with different payload data, balance
 * calculation from persisted transactions, and recent transaction projection
 * for account detail reads.</p>
 *
 * <p>This service does not use circuit breakers because its current dependency
 * is the local configured datasource, not another remote downstream service.
 * Concurrency and wait behavior are controlled by the Hikari connection pool
 * configured under {@code spring.datasource.hikari}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLedgerService {

    private final AccountTransactionRepository repository;
    private final AccountServiceProperties properties;
    private final Clock clock;

    /**
     * Applies a transaction idempotently using the upstream event id as the
     * durable idempotency key.
     *
     * <p>If the event id has not been seen before, the transaction is stored
     * and included in future balance calculations. If the event id already
     * exists with identical account id, type, amount, currency, and event
     * timestamp, the existing transaction is returned as a duplicate without a
     * second write. If the same event id is reused with different transaction
     * data, a {@link DuplicateTransactionConflictException} is raised.</p>
     *
     * @param accountId account id path variable
     * @param request transaction request
     * @param idempotencyKey optional idempotency header from Event Gateway
     * @return transaction response
     */
    @Transactional
    public TransactionResponse applyTransaction(
            String accountId,
            AccountTransactionRequest request,
            String idempotencyKey
    ) {
        log.info(
                "Starting account transaction apply accountId={} eventId={} type={} amount={} currency={} "
                        + "eventTimestamp={} idempotencyKeyPresent={}",
                accountId,
                request.eventId(),
                request.type(),
                request.amount(),
                request.currency(),
                request.eventTimestamp(),
                idempotencyKey != null && !idempotencyKey.isBlank()
        );
        TransactionResponse response = repository.findById(request.eventId())
                .map(existing -> duplicateOrConflict(existing, accountId, request))
                .orElseGet(() -> persist(accountId, request, idempotencyKey));
        log.info(
                "Completed account transaction apply accountId={} eventId={} duplicate={} appliedAt={}",
                response.accountId(),
                response.eventId(),
                response.duplicate(),
                response.appliedAt()
        );
        return response;
    }

    /**
     * Returns the current account balance computed from all stored transactions.
     *
     * <p>Credits increase the balance and debits decrease the balance. This
     * skeleton assumes a single account currency and returns the currency from
     * the first persisted account transaction.</p>
     *
     * @param accountId account id path variable
     * @return balance response
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId) {
        log.info("Starting account balance lookup accountId={}", accountId);
        List<AccountTransactionRecord> records = accountRecords(accountId);
        BigDecimal computedBalance = balance(records);
        BalanceResponse response = new BalanceResponse(accountId, computedBalance, records.getFirst().getCurrency());
        log.info(
                "Completed account balance lookup accountId={} transactionCount={} balance={} currency={}",
                accountId,
                records.size(),
                response.balance(),
                response.currency()
        );
        return response;
    }

    /**
     * Returns account details with computed balance and recent transactions.
     *
     * <p>The balance is computed from all persisted account records. Recent
     * transactions are returned separately using the configured
     * {@code account-service.transactions.recent-limit} value and descending
     * business event timestamp order.</p>
     *
     * @param accountId account id path variable
     * @return account details response
     */
    @Transactional(readOnly = true)
    public AccountDetailsResponse getAccount(String accountId) {
        log.info("Starting account detail lookup accountId={} recentLimit={}", accountId, properties.recentLimit());
        List<AccountTransactionRecord> records = accountRecords(accountId);
        List<TransactionResponse> recentTransactions = repository
                .findByAccountIdOrderByEventTimestampDesc(accountId, PageRequest.of(0, properties.recentLimit()))
                .stream()
                .map(record -> toResponse(record, false))
                .toList();
        BigDecimal computedBalance = balance(records);
        AccountDetailsResponse response = new AccountDetailsResponse(
                accountId,
                computedBalance,
                records.getFirst().getCurrency(),
                recentTransactions
        );
        log.info(
                "Completed account detail lookup accountId={} transactionCount={} recentTransactionCount={} "
                        + "balance={} currency={}",
                accountId,
                records.size(),
                response.recentTransactions().size(),
                response.balance(),
                response.currency()
        );
        return response;
    }

    /**
     * Persists a new account transaction record after idempotency lookup has
     * confirmed no existing transaction uses the same event id.
     *
     * @param accountId account receiving the transaction
     * @param request validated transaction request body
     * @param idempotencyKey optional Event Gateway idempotency header value
     * @return response DTO representing the stored transaction
     */
    private TransactionResponse persist(String accountId, AccountTransactionRequest request, String idempotencyKey) {
        log.debug(
                "Persisting new account transaction accountId={} eventId={} idempotencyKeyPresent={}",
                accountId,
                request.eventId(),
                idempotencyKey != null && !idempotencyKey.isBlank()
        );
        AccountTransactionRecord saved = repository.save(AccountTransactionRecord.from(
                accountId,
                request,
                idempotencyKey,
                Instant.now(clock)
        ));
        log.info(
                "Persisted new account transaction accountId={} eventId={} createdAt={} type={} amount={} currency={}",
                accountId,
                saved.getEventId(),
                saved.getCreatedAt(),
                saved.getType(),
                saved.getAmount(),
                saved.getCurrency()
        );
        return toResponse(saved, false);
    }

    /**
     * Resolves an already-stored event id as either an exact duplicate or a
     * conflicting duplicate.
     *
     * @param existing persisted transaction with the same event id
     * @param accountId incoming account id path variable
     * @param request incoming transaction request body
     * @return duplicate response when payload data matches the stored record
     */
    private TransactionResponse duplicateOrConflict(
            AccountTransactionRecord existing,
            String accountId,
            AccountTransactionRequest request
    ) {
        log.debug(
                "Evaluating existing transaction for idempotency accountId={} eventId={} existingAccountId={}",
                accountId,
                request.eventId(),
                existing.getAccountId()
        );
        if (!sameTransaction(existing, accountId, request)) {
            log.warn(
                    "Detected duplicate transaction conflict accountId={} eventId={} existingAccountId={} "
                            + "existingType={} requestedType={} existingAmount={} requestedAmount={} "
                            + "existingCurrency={} requestedCurrency={} existingTimestamp={} requestedTimestamp={}",
                    accountId,
                    request.eventId(),
                    existing.getAccountId(),
                    existing.getType(),
                    request.type(),
                    existing.getAmount(),
                    request.amount(),
                    existing.getCurrency(),
                    request.currency(),
                    existing.getEventTimestamp(),
                    request.eventTimestamp()
            );
            throw new DuplicateTransactionConflictException(request.eventId());
        }
        log.info(
                "Acknowledging exact duplicate account transaction accountId={} eventId={} originalAppliedAt={}",
                accountId,
                request.eventId(),
                existing.getCreatedAt()
        );
        return toResponse(existing, true);
    }

    /**
     * Compares the persisted transaction identity fields with an incoming
     * request to determine whether a repeated event id is a safe duplicate.
     *
     * @param existing persisted transaction
     * @param accountId incoming account id
     * @param request incoming transaction request
     * @return {@code true} when all idempotency-significant fields match
     */
    private boolean sameTransaction(
            AccountTransactionRecord existing,
            String accountId,
            AccountTransactionRequest request
    ) {
        boolean same = existing.getAccountId().equals(accountId)
                && existing.getType() == request.type()
                && existing.getAmount().compareTo(request.amount()) == 0
                && existing.getCurrency().equalsIgnoreCase(request.currency())
                && existing.getEventTimestamp().equals(request.eventTimestamp());
        log.debug(
                "Compared account transaction idempotency fields accountId={} eventId={} same={}",
                accountId,
                request.eventId(),
                same
        );
        return same;
    }

    /**
     * Loads all transaction records for an account in ascending business event
     * timestamp order.
     *
     * @param accountId account id to read
     * @return account transactions ordered from oldest to newest
     * @throws AccountNotFoundException when the skeleton ledger has no records for the account
     */
    private List<AccountTransactionRecord> accountRecords(String accountId) {
        log.debug("Loading account transaction records accountId={}", accountId);
        List<AccountTransactionRecord> records = repository.findByAccountId(accountId)
                .stream()
                .sorted(Comparator.comparing(AccountTransactionRecord::getEventTimestamp))
                .toList();
        if (records.isEmpty()) {
            log.warn("No account transaction records found accountId={}", accountId);
            throw new AccountNotFoundException(accountId);
        }
        log.debug("Loaded account transaction records accountId={} transactionCount={}", accountId, records.size());
        return records;
    }

    /**
     * Calculates the signed account balance from transaction records.
     *
     * @param records account transaction records
     * @return sum of credits minus debits
     */
    private BigDecimal balance(List<AccountTransactionRecord> records) {
        BigDecimal computedBalance = records.stream()
                .map(this::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.debug("Calculated account balance transactionCount={} balance={}", records.size(), computedBalance);
        return computedBalance;
    }

    /**
     * Converts a transaction amount to its balance impact.
     *
     * @param record persisted transaction record
     * @return positive amount for credits and negative amount for debits
     */
    private BigDecimal signedAmount(AccountTransactionRecord record) {
        BigDecimal signedAmount;
        if (record.getType() == EventType.DEBIT) {
            signedAmount = record.getAmount().negate();
        } else {
            signedAmount = record.getAmount();
        }
        log.debug(
                "Converted transaction amount to signed balance impact accountId={} eventId={} type={} amount={} signedAmount={}",
                record.getAccountId(),
                record.getEventId(),
                record.getType(),
                record.getAmount(),
                signedAmount
        );
        return signedAmount;
    }

    /**
     * Maps a persisted transaction record to the public transaction response
     * shape used by controller and acceptance-test responses.
     *
     * @param record persisted transaction record
     * @param duplicate whether the response represents an acknowledged duplicate request
     * @return transaction response DTO
     */
    private TransactionResponse toResponse(AccountTransactionRecord record, boolean duplicate) {
        log.debug(
                "Mapping transaction record to response accountId={} eventId={} duplicate={}",
                record.getAccountId(),
                record.getEventId(),
                duplicate
        );
        return new TransactionResponse(
                record.getEventId(),
                record.getAccountId(),
                record.getType(),
                record.getAmount(),
                record.getCurrency(),
                record.getEventTimestamp(),
                duplicate,
                record.getCreatedAt()
        );
    }
}
