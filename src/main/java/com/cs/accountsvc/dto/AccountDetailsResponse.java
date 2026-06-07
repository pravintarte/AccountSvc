package com.cs.accountsvc.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Account detail response including a computed balance and recent transactions.
 *
 * <p>This payload backs {@code GET /accounts/{accountId}}. It gives callers a
 * compact account read model: the account identity, current signed balance,
 * ledger currency, and a bounded recent transaction history controlled by
 * {@code account-service.transactions.recent-limit}.</p>
 *
 * @param accountId account id requested by the caller
 * @param balance current signed balance computed from all persisted transactions
 * @param currency account transaction currency returned from the ledger
 * @param recentTransactions bounded newest-first transaction history
 */
public record AccountDetailsResponse(
        String accountId,
        BigDecimal balance,
        String currency,
        List<TransactionResponse> recentTransactions
) {
}
