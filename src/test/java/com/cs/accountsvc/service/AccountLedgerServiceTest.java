package com.cs.accountsvc.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.cs.accountsvc.dto.AccountDetailsResponse;
import com.cs.accountsvc.dto.AccountTransactionRequest;
import com.cs.accountsvc.dto.BalanceResponse;
import com.cs.accountsvc.dto.EventType;
import com.cs.accountsvc.dto.TransactionResponse;
import com.cs.accountsvc.exception.DuplicateTransactionConflictException;
import com.cs.accountsvc.repository.AccountTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration-style unit tests for {@link AccountLedgerService}.
 *
 * <p>These tests validate the service contract that Event Gateway depends on:
 * idempotent transaction application, duplicate-conflict detection, signed
 * balance calculation, and account detail projection from persisted H2 ledger
 * records.</p>
 */
@Slf4j
@DisplayName("AccountLedgerService persistence, idempotency, balance, and read-model behavior")
@SpringBootTest(properties = "eureka.client.enabled=false")
class AccountLedgerServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("9b63f0d4-0f49-4f85-9447-fd45a0c5b3c2");

    @Autowired
    private AccountTransactionRepository repository;

    @Autowired
    private AccountLedgerService service;

    /**
     * Clears the in-memory account transaction table before every scenario so
     * each test proves behavior from an isolated ledger state.
     */
    @BeforeEach
    @DisplayName("Reset the in-memory account transaction repository before each AccountLedgerService scenario")
    void setUp() {
        log.info("Resetting account transaction repository before AccountLedgerService test scenario");
        repository.deleteAll();
    }

    /**
     * Verifies that a new credit transaction is stored exactly once and returned
     * as a non-duplicate response.
     */
    @Test
    @DisplayName("applyTransaction persists a new CREDIT event and returns a non-duplicate applied response")
    void applyTransaction_whenCreditEventIdHasNotBeenSeen_persistsOneTransactionAndReturnsNonDuplicateResponse() {
        log.info("Testing new CREDIT transaction persistence and non-duplicate response");
        TransactionResponse response = service.applyTransaction(
                "acct-123",
                request(EVENT_ID, EventType.CREDIT, "150.00"),
                EVENT_ID.toString()
        );

        assertThat(response.duplicate()).isFalse();
        assertThat(response.accountId()).isEqualTo("acct-123");
        assertThat(repository.count()).isEqualTo(1);
        log.info("Verified new CREDIT transaction persistence and non-duplicate response");
    }

    /**
     * Verifies idempotency behavior for an exact repeated Event Gateway apply
     * request.
     */
    @Test
    @DisplayName("applyTransaction acknowledges an exact duplicate event without inserting a second transaction")
    void applyTransaction_whenSameEventIdAndSamePayloadAreSubmittedAgain_returnsDuplicateAndKeepsSingleRecord() {
        log.info("Testing exact duplicate transaction acknowledgement without a second insert");
        AccountTransactionRequest request = request(EVENT_ID, EventType.CREDIT, "150.00");
        service.applyTransaction("acct-123", request, EVENT_ID.toString());

        TransactionResponse duplicate = service.applyTransaction("acct-123", request, EVENT_ID.toString());

        assertThat(duplicate.duplicate()).isTrue();
        assertThat(repository.count()).isEqualTo(1);
        log.info("Verified exact duplicate transaction acknowledgement without a second insert");
    }

    /**
     * Verifies that event id reuse with different transaction data is treated as
     * a conflict rather than a safe duplicate.
     */
    @Test
    @DisplayName("applyTransaction rejects a reused event id when the incoming payload differs from the stored transaction")
    void applyTransaction_whenExistingEventIdIsReusedWithDifferentTransactionType_throwsDuplicateTransactionConflict() {
        log.info("Testing duplicate transaction conflict when event id is reused with different payload");
        service.applyTransaction("acct-123", request(EVENT_ID, EventType.CREDIT, "150.00"), EVENT_ID.toString());

        assertThatThrownBy(() -> service.applyTransaction(
                "acct-123",
                request(EVENT_ID, EventType.DEBIT, "150.00"),
                EVENT_ID.toString()
        )).isInstanceOf(DuplicateTransactionConflictException.class);
        log.info("Verified duplicate transaction conflict for reused event id with different payload");
    }

    /**
     * Verifies balance calculation across both transaction directions.
     */
    @Test
    @DisplayName("getBalance returns credits minus debits for all persisted transactions on the account")
    void getBalance_whenAccountContainsCreditAndDebitTransactions_returnsSignedNetBalanceAndCurrency() {
        log.info("Testing signed balance calculation from CREDIT and DEBIT transactions");
        service.applyTransaction("acct-123", request(EVENT_ID, EventType.CREDIT, "150.00"), EVENT_ID.toString());
        service.applyTransaction(
                "acct-123",
                request(UUID.fromString("5ecf9f54-2f32-41b0-9b48-e879842f3fb5"), EventType.DEBIT, "25.00"),
                "debit-key"
        );

        BalanceResponse balance = service.getBalance("acct-123");

        assertThat(balance.balance()).isEqualByComparingTo("125.00");
        assertThat(balance.currency()).isEqualTo("USD");
        log.info("Verified signed balance calculation from CREDIT and DEBIT transactions");
    }

    /**
     * Verifies account detail read projection after at least one transaction has
     * been applied.
     */
    @Test
    @DisplayName("getAccount returns account details with computed balance and recent transaction history")
    void getAccount_whenAccountHasStoredTransactions_returnsDetailsWithRecentTransactions() {
        log.info("Testing account detail projection with recent transaction history");
        service.applyTransaction("acct-123", request(EVENT_ID, EventType.CREDIT, "150.00"), EVENT_ID.toString());

        AccountDetailsResponse account = service.getAccount("acct-123");

        assertThat(account.accountId()).isEqualTo("acct-123");
        assertThat(account.recentTransactions()).hasSize(1);
        log.info("Verified account detail projection with recent transaction history");
    }

    /**
     * Builds a valid Event Gateway transaction request for service tests.
     *
     * @param eventId upstream event id
     * @param type transaction direction
     * @param amount positive decimal transaction amount
     * @return valid account transaction request
     */
    private AccountTransactionRequest request(UUID eventId, EventType type, String amount) {
        log.debug("Building AccountLedgerService test transaction request eventId={} type={} amount={}",
                eventId, type, amount);
        return new AccountTransactionRequest(
                eventId,
                type,
                new BigDecimal(amount),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                Map.of("source", "event-gateway")
        );
    }

}
