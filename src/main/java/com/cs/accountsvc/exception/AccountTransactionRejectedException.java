package com.cs.accountsvc.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * Raised when Account Service rejects a valid gateway transaction request for a
 * business reason.
 *
 * <p>The Event Gateway treats this as a non-retryable downstream rejection and
 * records the event as rejected rather than failed.</p>
 */
@Slf4j
public class AccountTransactionRejectedException extends RuntimeException {

    /**
     * Creates a business rejection exception.
     *
     * @param message stable rejection description returned to the gateway
     */
    public AccountTransactionRejectedException(String message) {
        super(message);
        log.warn("Creating account transaction rejection exception message={}", message);
    }
}
