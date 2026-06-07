package com.cs.accountsvc.dto;

import java.math.BigDecimal;

/**
 * Account balance response.
 *
 * <p>This payload backs {@code GET /accounts/{accountId}/balance}. The balance
 * is calculated as credits minus debits from all persisted ledger rows for the
 * account.</p>
 *
 * @param accountId account id requested by the caller
 * @param balance signed account balance
 * @param currency currency associated with the account ledger rows
 */
public record BalanceResponse(String accountId, BigDecimal balance, String currency) {
}
