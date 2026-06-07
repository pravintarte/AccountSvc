package com.cs.accountsvc.controller;

import java.time.Clock;
import java.time.Instant;

import com.cs.accountsvc.dto.AccountDetailsResponse;
import com.cs.accountsvc.dto.AccountTransactionRequest;
import com.cs.accountsvc.dto.ApiCodes;
import com.cs.accountsvc.dto.ApiResponse;
import com.cs.accountsvc.dto.BalanceResponse;
import com.cs.accountsvc.dto.TransactionResponse;
import com.cs.accountsvc.service.AccountLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
    private final Clock clock;

    /**
     * Applies an account transaction idempotently.
     *
     * @param accountId account id path variable
     * @param idempotencyKey optional idempotency key header
     * @param request transaction request body
     * @return applied or duplicate transaction response
     */
    @Operation(
            summary = "Apply account transaction",
            description = "Applies a CREDIT or DEBIT transaction sent by Event Gateway."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Transaction was applied.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Duplicate transaction was acknowledged.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Event id was reused with different transaction data."
            )
    })
    @PostMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<ApiResponse<TransactionResponse>> applyTransaction(
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
        TransactionResponse serviceResponse = accountLedgerService.applyTransaction(accountId, request, idempotencyKey);
        HttpStatus status = serviceResponse.duplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        String code = serviceResponse.duplicate() ? ApiCodes.TRANSACTION_DUPLICATE : ApiCodes.TRANSACTION_APPLIED;
        String description = serviceResponse.duplicate()
                ? "Duplicate transaction was acknowledged and not re-applied."
                : "Transaction was applied.";
        return ResponseEntity.status(status).body(new ApiResponse<>(
                Instant.now(clock),
                status.value(),
                code,
                description,
                serviceResponse
        ));
    }

    /**
     * Gets the current account balance.
     *
     * @param accountId account id path variable
     * @return balance response
     */
    @Operation(summary = "Get account balance")
    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @PathVariable @NotBlank String accountId
    ) {
        log.info("Received account balance request accountId={}", accountId);
        BalanceResponse balance = accountLedgerService.getBalance(accountId);
        return ResponseEntity.ok(new ApiResponse<>(
                Instant.now(clock),
                HttpStatus.OK.value(),
                ApiCodes.BALANCE_RETRIEVED,
                "Account balance was retrieved.",
                balance
        ));
    }

    /**
     * Gets account details with recent transactions.
     *
     * @param accountId account id path variable
     * @return account detail response
     */
    @Operation(summary = "Get account details")
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<ApiResponse<AccountDetailsResponse>> getAccount(
            @PathVariable @NotBlank String accountId
    ) {
        log.info("Received account detail request accountId={}", accountId);
        AccountDetailsResponse account = accountLedgerService.getAccount(accountId);
        return ResponseEntity.ok(new ApiResponse<>(
                Instant.now(clock),
                HttpStatus.OK.value(),
                ApiCodes.ACCOUNT_RETRIEVED,
                "Account details were retrieved.",
                account
        ));
    }
}
