package com.aegisledger.ledger.repository;

import com.aegisledger.ledger.domain.EntryType;
import com.aegisledger.ledger.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for appending and querying immutable ledger entries.
 */
@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.entryType = :entryType")
    BigDecimal sumTotalByEntryType(@Param("entryType") EntryType entryType);

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.accountId = :accountId AND l.entryType = :entryType")
    BigDecimal sumAmountByAccountIdAndEntryType(@Param("accountId") UUID accountId, @Param("entryType") EntryType entryType);
}
