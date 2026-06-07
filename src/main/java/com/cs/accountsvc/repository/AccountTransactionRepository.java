package com.cs.accountsvc.repository;

import java.util.List;
import java.util.UUID;

import com.cs.accountsvc.entity.AccountTransactionRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for account transaction records.
 *
 * <p>This interface uses Spring Data JPA derived queries to keep the skeleton
 * repository explicit and dependency-free. The primary key is the upstream
 * event id, while account-level lookup methods support balance calculation and
 * account detail history projection.</p>
 */
public interface AccountTransactionRepository extends JpaRepository<AccountTransactionRecord, UUID> {

    /**
     * Finds all transactions currently stored for an account.
     *
     * <p>The service layer sorts these results by business event timestamp when
     * calculating balances or building read models, so this query intentionally
     * keeps the database method simple.</p>
     *
     * @param accountId account id to search
     * @return all persisted transaction records for the account
     */
    List<AccountTransactionRecord> findByAccountId(String accountId);

    /**
     * Finds the most recent transactions for an account in descending event time
     * order.
     *
     * @param accountId account id to search
     * @param pageable page request that bounds the number of returned rows
     * @return bounded recent transaction history ordered newest first
     */
    List<AccountTransactionRecord> findByAccountIdOrderByEventTimestampDesc(String accountId, Pageable pageable);
}
