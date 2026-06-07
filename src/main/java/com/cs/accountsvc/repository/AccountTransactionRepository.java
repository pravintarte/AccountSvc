package com.cs.accountsvc.repository;

import java.util.List;
import java.util.UUID;

import com.cs.accountsvc.entity.AccountTransactionRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for account transaction records.
 */
public interface AccountTransactionRepository extends JpaRepository<AccountTransactionRecord, UUID> {

    List<AccountTransactionRecord> findByAccountId(String accountId);

    List<AccountTransactionRecord> findByAccountIdOrderByEventTimestampDesc(String accountId, Pageable pageable);
}
