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
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for account controller response-envelope selection.
 */
class AccountControllerTest {

    private static final Instant NOW = Instant.parse("2026-06-06T21:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID EVENT_ID = UUID.fromString("9b63f0d4-0f49-4f85-9447-fd45a0c5b3c2");

    @Test
    void applyTransaction_whenServiceAppliesNewTransaction_returnsCreatedEnvelope() {
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
    }

    @Test
    void applyTransaction_whenServiceReportsDuplicate_returnsOkEnvelope() {
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
    }

    private AccountTransactionRequest request() {
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
