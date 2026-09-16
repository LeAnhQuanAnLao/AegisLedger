package com.aegisledger.outbox.repository;

import com.aegisledger.outbox.domain.OutboxEvent;
import com.aegisledger.outbox.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing OutboxEvent records with skip locked concurrency support.
 */
@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Query(value = "SELECT * FROM outbox_events WHERE status = :#{#status.name()} ORDER BY created_at ASC LIMIT 50 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findPendingEventsForProcessing(@Param("status") OutboxStatus status);
}
