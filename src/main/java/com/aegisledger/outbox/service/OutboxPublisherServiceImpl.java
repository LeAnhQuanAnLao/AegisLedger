package com.aegisledger.outbox.service;

import com.aegisledger.outbox.domain.OutboxEvent;
import com.aegisledger.outbox.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementation of OutboxPublisherService recording events in the local database.
 */
@Service
public class OutboxPublisherServiceImpl implements OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherServiceImpl.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxPublisherServiceImpl(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventType,
                jsonPayload
            );
            outboxRepository.save(event);
            log.info("Persisted transactional outbox event [type={}, aggregate={}]", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload for aggregate {}", aggregateId, e);
            throw new RuntimeException("Could not serialize outbox payload", e);
        }
    }
}
