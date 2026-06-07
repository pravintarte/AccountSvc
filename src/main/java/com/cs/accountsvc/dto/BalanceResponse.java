package com.cs.accountsvc.dto;

import java.math.BigDecimal;

/**
 * Account balance response.
 */
public record BalanceResponse(String accountId, BigDecimal balance, String currency) {
}
