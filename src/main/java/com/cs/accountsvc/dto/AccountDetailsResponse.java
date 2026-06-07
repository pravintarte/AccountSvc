package com.cs.accountsvc.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Account detail response including a computed balance and recent transactions.
 */
public record AccountDetailsResponse(
        String accountId,
        BigDecimal balance,
        String currency,
        List<TransactionResponse> recentTransactions
) {
}
