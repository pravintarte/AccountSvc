package com.cs.accountsvc.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * Raised when an account has no transactions in the skeleton ledger.
 *
 * <p>The current skeleton treats an account as existing once at least one
 * transaction has been applied. Read requests for accounts with no ledger rows
 * are converted by the global exception handler into a stable
 * {@code ACCOUNT_NOT_FOUND} API error.</p>
 */
@Slf4j
public class AccountNotFoundException extends RuntimeException {

    /**
     * Creates an account-not-found exception for a requested account id.
     *
     * @param accountId account id that had no persisted transactions
     */
    public AccountNotFoundException(String accountId) {
        super(message(accountId));
    }

    private static String message(String accountId) {
        log.warn("Creating account not found exception accountId={}", accountId);
        return "Account was not found: " + accountId;
    }
}
