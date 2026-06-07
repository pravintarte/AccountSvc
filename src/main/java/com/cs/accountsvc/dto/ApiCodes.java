package com.cs.accountsvc.dto;

/**
 * Stable application-level API codes returned in response envelopes.
 */
public final class ApiCodes {

    public static final String TRANSACTION_APPLIED = "TRANSACTION_APPLIED";
    public static final String TRANSACTION_DUPLICATE = "TRANSACTION_DUPLICATE";
    public static final String ACCOUNT_RETRIEVED = "ACCOUNT_RETRIEVED";
    public static final String BALANCE_RETRIEVED = "BALANCE_RETRIEVED";
    public static final String HEALTH_OK = "HEALTH_OK";

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    public static final String DUPLICATE_TRANSACTION_CONFLICT = "DUPLICATE_TRANSACTION_CONFLICT";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    private ApiCodes() {
    }
}
