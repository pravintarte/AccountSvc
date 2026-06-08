package com.cs.accountsvc.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.cs.accountsvc.dto.AccountTransactionRequest;
import com.cs.accountsvc.dto.BalanceResponse;
import com.cs.accountsvc.dto.EventType;
import com.cs.accountsvc.dto.TransactionResponse;
import com.cs.accountsvc.exception.AccountTransactionRejectedException;
import com.cs.accountsvc.service.AccountLedgerService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Account Service HTTP contract response selection.
 */
@Slf4j
@DisplayName("AccountController Event Gateway contract behavior")
class AccountControllerTest {

    private static final Instant NOW = Instant.parse("2026-06-06T21:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("9b63f0d4-0f49-4f85-9447-fd45a0c5b3c2");

    @Test
    @DisplayName("applyTransaction returns HTTP 204 when the service accepts the transaction")
    void applyTransaction_whenServiceAcceptsTransaction_returnsNoContent() {
        log.info("Testing controller response for an accepted account transaction");
        AccountLedgerService service = mock(AccountLedgerService.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AccountController controller = new AccountController(service, meterRegistry);
        AccountTransactionRequest request = request();

        when(service.applyTransaction("acct-123", request, EVENT_ID.toString()))
                .thenReturn(response(false));

        ResponseEntity<Void> response = controller
                .applyTransaction("acct-123", EVENT_ID.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        assertThat(meterRegistry.counter(
                "account_service.transactions",
                "type",
                "CREDIT",
                "result",
                "success"
        ).count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter(
                "account_service.transactions.applied",
                "type",
                "CREDIT",
                "result",
                "accepted"
        ).count()).isEqualTo(1.0);
        verify(service).applyTransaction("acct-123", request, EVENT_ID.toString());
        log.info("Verified controller returned HTTP 204 for accepted transaction");
    }

    @Test
    @DisplayName("applyTransaction records a failure metric when the service rejects the transaction")
    void applyTransaction_whenServiceRejectsTransaction_recordsFailureMetricAndRethrows() {
        log.info("Testing controller metric recording for a rejected account transaction");
        AccountLedgerService service = mock(AccountLedgerService.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AccountController controller = new AccountController(service, meterRegistry);
        AccountTransactionRequest request = request();

        doThrow(new AccountTransactionRejectedException("insufficient funds"))
                .when(service)
                .applyTransaction("acct-123", request, EVENT_ID.toString());

        assertThatThrownBy(() -> controller.applyTransaction("acct-123", EVENT_ID.toString(), request))
                .isInstanceOf(AccountTransactionRejectedException.class)
                .hasMessage("insufficient funds");

        assertThat(meterRegistry.counter(
                "account_service.transactions",
                "type",
                "CREDIT",
                "result",
                "failure"
        ).count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("account_service.transactions")
                .tag("type", "CREDIT")
                .tag("result", "success")
                .counter()).isNull();
        verify(service).applyTransaction("acct-123", request, EVENT_ID.toString());
        verifyNoMoreInteractions(service);
        log.info("Verified controller recorded failure metric for rejected transaction");
    }

    @Test
    @DisplayName("getBalance returns the raw Account Service balance DTO")
    void getBalance_whenServiceReturnsBalance_returnsRawBalancePayload() {
        log.info("Testing controller response for account balance read");
        AccountLedgerService service = mock(AccountLedgerService.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AccountController controller = new AccountController(service, meterRegistry);
        BalanceResponse balance = new BalanceResponse("acct-123", new BigDecimal("275.50"), "USD");

        when(service.getBalance("acct-123")).thenReturn(balance);

        ResponseEntity<BalanceResponse> response = controller.getBalance("acct-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(balance);
        assertThat(meterRegistry.counter("account_service.requests", "endpoint", "balance").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.counter("account_service.balance.enquiries").count())
                .isEqualTo(1.0);
        log.info("Verified controller returned raw balance response");
    }

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
