package com.cs.accountsvc.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.cs.accountsvc.dto.EventType;
import com.cs.accountsvc.entity.AccountTransactionRecord;
import com.cs.accountsvc.repository.AccountTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Pact provider verification for the Event Gateway to Account Service contract.
 *
 * <p>The sibling Event Gateway project generates the consumer pact. This test
 * verifies Account Service as the provider against the same interaction file so
 * consumer request expectations and provider HTTP behavior are checked from
 * both sides.</p>
 */
@Slf4j
@Provider("account-service")
@PactFolder("src/test/resources/pacts")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "management.tracing.enabled=false"
        }
)
class AccountServicePactProviderTest {

    private static final UUID BALANCE_EVENT_ID = UUID.fromString("5ecf9f54-2f32-41b0-9b48-e879842f3fb5");
    private static final UUID REJECTION_BALANCE_EVENT_ID = UUID.fromString("0bd17867-d631-44f1-ae8f-a6a726761785");

    @LocalServerPort
    private int port;

    @Autowired
    private AccountTransactionRepository repository;

    /**
     * Connects Pact's verifier to the random HTTP port allocated by Spring Boot
     * and clears provider state before each interaction.
     *
     * @param context Pact verification context for the current interaction
     */
    @BeforeEach
    void beforeEach(PactVerificationContext context) {
        log.info("Preparing Pact provider verification target port={}", port);
        repository.deleteAll();
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    /**
     * Executes Pact verification for each interaction loaded from the local pact
     * fixture.
     *
     * @param context Pact verification context for the current interaction
     */
    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    /**
     * Provider state for a credit apply request that should be accepted.
     */
    @State("account acct-123 accepts credit transactions")
    void accountAcceptsCreditTransactions() {
        log.info("Setting Pact provider state: account acct-123 accepts credit transactions");
        repository.deleteAll();
    }

    /**
     * Provider state for a balance read with a deterministic response body.
     */
    @State("account acct-123 has a balance")
    void accountHasBalance() {
        log.info("Setting Pact provider state: account acct-123 has a balance");
        repository.deleteAll();
        repository.save(record(
                BALANCE_EVENT_ID,
                "acct-123",
                EventType.CREDIT,
                "275.50",
                "balance-key",
                "2026-05-15T14:02:11Z"
        ));
    }

    /**
     * Provider state for a debit request that should be rejected as
     * non-retryable because the account cannot cover the debit.
     */
    @State("account acct-123 rejects the transaction")
    void accountRejectsTransaction() {
        log.info("Setting Pact provider state: account acct-123 rejects the transaction");
        repository.deleteAll();
        repository.save(record(
                REJECTION_BALANCE_EVENT_ID,
                "acct-123",
                EventType.CREDIT,
                "100.00",
                "rejection-balance-key",
                "2026-05-15T13:02:11Z"
        ));
    }

    /**
     * Provider state for a missing-account read.
     */
    @State("account acct-404 does not exist")
    void accountDoesNotExist() {
        log.info("Setting Pact provider state: account acct-404 does not exist");
        repository.deleteAll();
    }

    private AccountTransactionRecord record(
            UUID eventId,
            String accountId,
            EventType type,
            String amount,
            String idempotencyKey,
            String eventTimestamp
    ) {
        AccountTransactionRecord record = new AccountTransactionRecord();
        record.setEventId(eventId);
        record.setAccountId(accountId);
        record.setType(type);
        record.setAmount(new BigDecimal(amount));
        record.setCurrency("USD");
        record.setEventTimestamp(Instant.parse(eventTimestamp));
        record.setIdempotencyKey(idempotencyKey);
        record.setCreatedAt(Instant.parse(eventTimestamp));
        return record;
    }
}
