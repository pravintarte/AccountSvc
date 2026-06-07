package com.cs.accountsvc.controller;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.cs.accountsvc.dto.AccountTransactionRequest;
import com.cs.accountsvc.dto.ApiResponse;
import com.cs.accountsvc.dto.EventType;
import com.cs.accountsvc.dto.TransactionResponse;
import com.cs.accountsvc.service.AccountLedgerService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for account controller response-envelope selection.
 *
 * <p>The controller does not own persistence or idempotency decisions; those
 * belong to {@link AccountLedgerService}. These tests mock the service and
 * verify the HTTP status and stable response code selected for each major
 * transaction apply outcome.</p>
 */
@Slf4j
@DisplayName("AccountController transaction response-envelope mapping behavior")
class AccountControllerTest {

    private static final Instant NOW = Instant.parse("2026-06-06T21:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID EVENT_ID = UUID.fromString("9b63f0d4-0f49-4f85-9447-fd45a0c5b3c2");

    /**
     * Verifies that a newly applied transaction is exposed as HTTP 201 with the
     * stable {@code TRANSACTION_APPLIED} code.
     */
    @Test
    @DisplayName("applyTransaction returns HTTP 201 and TRANSACTION_APPLIED when the service stores a new transaction")
    void applyTransaction_whenServiceAppliesNewTransaction_returnsCreatedEnvelopeWithTransactionAppliedCode() {
        log.info("Testing controller response for a newly applied account transaction");
        AccountLedgerService service = mock(AccountLedgerService.class);
        AccountController controller = new AccountController(service, CLOCK);
        AccountTransactionRequest request = request();

        when(service.applyTransaction("acct-123", request, EVENT_ID.toString()))
                .thenReturn(response(false));

        ResponseEntity<ApiResponse<TransactionResponse>> response = controller
                .applyTransaction("acct-123", EVENT_ID.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("TRANSACTION_APPLIED");
        log.info("Verified controller returned HTTP 201 and TRANSACTION_APPLIED for new transaction");
    }

    /**
     * Verifies that a safe duplicate transaction is exposed as HTTP 200 with the
     * stable {@code TRANSACTION_DUPLICATE} code.
     */
    @Test
    @DisplayName("applyTransaction returns HTTP 200 and TRANSACTION_DUPLICATE when the service reports a duplicate")
    void applyTransaction_whenServiceReportsExactDuplicate_returnsOkEnvelopeWithTransactionDuplicateCode() {
        log.info("Testing controller response for an exact duplicate account transaction");
        AccountLedgerService service = mock(AccountLedgerService.class);
        AccountController controller = new AccountController(service, CLOCK);
        AccountTransactionRequest request = request();

        when(service.applyTransaction("acct-123", request, EVENT_ID.toString()))
                .thenReturn(response(true));

        ResponseEntity<ApiResponse<TransactionResponse>> response = controller
                .applyTransaction("acct-123", EVENT_ID.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("TRANSACTION_DUPLICATE");
        log.info("Verified controller returned HTTP 200 and TRANSACTION_DUPLICATE for duplicate transaction");
    }

    /**
     * Builds a valid request DTO reused by controller unit tests.
     *
     * @return valid credit transaction request
     */
    private AccountTransactionRequest request() {
        log.debug("Building controller test account transaction request eventId={}", EVENT_ID);
        return new AccountTransactionRequest(
                EVENT_ID,
                EventType.CREDIT,
                new BigDecimal("150.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                Map.of("source", "event-gateway")
        );
    }

    /**
     * Builds the mocked service response that the controller wraps.
     *
     * @param duplicate duplicate flag to include in the service response
     * @return transaction response returned by the mocked service
     */
    private TransactionResponse response(boolean duplicate) {
        log.debug("Building controller test transaction response eventId={} duplicate={}", EVENT_ID, duplicate);
        return new TransactionResponse(
                EVENT_ID,
                "acct-123",
                EventType.CREDIT,
                new BigDecimal("150.00"),
                "USD",
                Instant.parse("2026-05-15T14:02:11Z"),
                duplicate,
                NOW
        );
    }
}
