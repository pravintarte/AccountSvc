package com.cs.accountsvc.dto;

/**
 * Stable application-level API codes returned in response envelopes.
 *
 * <p>These codes are intentionally separate from HTTP status codes. Clients can
 * use them for deterministic branching, monitoring, and acceptance assertions
 * even when multiple business outcomes share the same HTTP status family.</p>
 */
public final class ApiCodes {

    public static final String TRANSACTION_APPLIED = "TRANSACTION_APPLIED";
    public static final String TRANSACTION_DUPLICATE = "TRANSACTION_DUPLICATE";
    public static final String ACCOUNT_RETRIEVED = "ACCOUNT_RETRIEVED";
    public static final String BALANCE_RETRIEVED = "BALANCE_RETRIEVED";
    public static final String HEALTH_OK = "HEALTH_OK";

    public static final String ACCOUNT_TRANSACTION_REJECTED = "ACCOUNT_TRANSACTION_REJECTED";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    public static final String DUPLICATE_TRANSACTION_CONFLICT = "DUPLICATE_TRANSACTION_CONFLICT";
    public static final String INTERNAL_ACCESS_DENIED = "INTERNAL_ACCESS_DENIED";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    /**
     * Prevents construction because this type is a constants namespace.
     */
    private ApiCodes() {
    }
}
