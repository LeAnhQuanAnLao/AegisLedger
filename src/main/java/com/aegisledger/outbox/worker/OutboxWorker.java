package com.aegisledger.outbox.worker;

import com.aegisledger.outbox.domain.OutboxEvent;
import com.aegisledger.outbox.domain.OutboxStatus;
import com.aegisledger.outbox.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Background worker polling pending outbox events and dispatching to Message Brokers.
 */
@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    private static final int MAX_RETRIES = 3;

    private final OutboxRepository outboxRepository;

    public OutboxWorker(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Scheduled(fixedDelayString = "${aegis.outbox.worker-fixed-rate-ms:1000}")
    @Transactional
    public void processPendingEvents() {
        List<OutboxEvent> pendingEvents;
        try {
            pendingEvents = outboxRepository.findPendingEventsForProcessing(OutboxStatus.PENDING);
            if (pendingEvents == null) {
                pendingEvents = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
            }
        } catch (Exception ex) {
            log.warn("Falling back to standard find query: {}", ex.getMessage());
            pendingEvents = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        }

        if (pendingEvents == null || pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events for dispatch", pendingEvents.size());
        for (OutboxEvent event : pendingEvents) {
            dispatchSingleEvent(event);
        }
    }

    public void dispatchSingleEvent(OutboxEvent event) {
        try {
            // Dispatch logic: Simulate delivery to Apache Kafka broker
            log.info("Dispatched event {} [{}] to Kafka broker topic 'aegis-ledger-events'",
                event.getId(), event.getEventType());
            event.markProcessed();
        } catch (Exception e) {
            log.error("Failed to publish outbox event {}", event.getId(), e);
            event.incrementRetry();
            if (event.getRetryCount() >= MAX_RETRIES) {
                event.markFailed();
            }
        }
        outboxRepository.save(event);
    }
}
