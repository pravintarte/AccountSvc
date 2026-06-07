package com.cs.accountsvc.dto;

/**
 * Supported account transaction directions.
 *
 * <p>{@link #CREDIT} increases an account balance and {@link #DEBIT} decreases
 * it. The enum values intentionally match Event Gateway's transaction event
 * type values so the gateway request body can be forwarded without translation.</p>
 */
public enum EventType {
    /**
     * Increases the signed account balance by the transaction amount.
     */
    CREDIT,

    /**
     * Decreases the signed account balance by the transaction amount.
     */
    DEBIT
}
