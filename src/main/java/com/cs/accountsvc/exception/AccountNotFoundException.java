package com.cs.accountsvc.exception;

/**
 * Raised when an account has no transactions in the skeleton ledger.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountId) {
        super("Account was not found: " + accountId);
    }
}
