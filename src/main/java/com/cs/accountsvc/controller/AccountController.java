package com.cs.accountsvc.controller;

import com.cs.accountsvc.dto.AccountDetailsResponse;
import com.cs.accountsvc.dto.AccountTransactionRequest;
import com.cs.accountsvc.dto.BalanceResponse;
import com.cs.accountsvc.service.AccountLedgerService;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account transaction and read endpoints consumed by Event Gateway.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account transaction and balance endpoints")
public class AccountController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final AccountLedgerService accountLedgerService;
    private final MeterRegistry meterRegistry;

    /**
     * Applies an account transaction idempotently.
     *
     * @param accountId account id path variable
     * @param idempotencyKey optional idempotency key header
     * @param request transaction request body
     * @return no-content response after the transaction is accepted
     */
    @Operation(
            summary = "Apply account transaction",
            description = "Applies a CREDIT or DEBIT transaction sent by Event Gateway."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Transaction was accepted or an exact duplicate was acknowledged."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Transaction was rejected or event id was reused with different transaction data."
            )
    })
    @PostMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<Void> applyTransaction(
            @Parameter(
                    name = "accountId",
                    description = "Account id receiving the transaction.",
                    example = "acct-123",
                    required = true,
                    in = ParameterIn.PATH
            )
            @PathVariable @NotBlank String accountId,

            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,

            @Valid @RequestBody AccountTransactionRequest request
    ) {
        log.info("Received account transaction request accountId={} eventId={}", accountId, request.eventId());
        accountLedgerService.applyTransaction(accountId, request, idempotencyKey);
        recordTransactionMetric(request.type().name(), "accepted");
        log.info(
                "Completed account transaction request accountId={} eventId={} httpStatus={}",
                accountId,
                request.eventId(),
                HttpStatus.NO_CONTENT.value()
        );
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets the current account balance.
     *
     * @param accountId account id path variable
     * @return balance response
     */
    @Operation(summary = "Get account balance")
    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable @NotBlank String accountId
    ) {
        log.info("Received account balance request accountId={}", accountId);
        BalanceResponse balance = accountLedgerService.getBalance(accountId);
        recordReadMetric("balance");
        ResponseEntity<BalanceResponse> response = ResponseEntity.ok(balance);
        log.info(
                "Completed account balance request accountId={} httpStatus={} balance={} currency={}",
                accountId,
                response.getStatusCode().value(),
                balance.balance(),
                balance.currency()
        );
        return response;
    }

    /**
     * Gets account details with recent transactions.
     *
     * @param accountId account id path variable
     * @return account detail response
     */
    @Operation(summary = "Get account details")
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountDetailsResponse> getAccount(
            @PathVariable @NotBlank String accountId
    ) {
        log.info("Received account detail request accountId={}", accountId);
        AccountDetailsResponse account = accountLedgerService.getAccount(accountId);
        recordReadMetric("account");
        ResponseEntity<AccountDetailsResponse> response = ResponseEntity.ok(account);
        log.info(
                "Completed account detail request accountId={} httpStatus={} recentTransactionCount={}",
                accountId,
                response.getStatusCode().value(),
                account.recentTransactions().size()
        );
        return response;
    }

    private void recordTransactionMetric(String type, String result) {
        meterRegistry.counter(
                "account_service.transactions.applied",
                "type",
                type,
                "result",
                result
        ).increment();
    }

    private void recordReadMetric(String endpoint) {
        meterRegistry.counter(
                "account_service.requests",
                "endpoint",
                endpoint
        ).increment();
    }
}
