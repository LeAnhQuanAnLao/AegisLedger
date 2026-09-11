package com.aegisledger.outbox.service;

/**
 * Service interface for publishing domain events to the local transactional outbox.
 */
public interface OutboxPublisherService {

    void publishEvent(String aggregateType, String aggregateId, String eventType, Object payload);
}
