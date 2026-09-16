package com.aegisledger.idempotency.service;

import com.aegisledger.core.exception.DuplicateRequestException;
import com.aegisledger.core.exception.RequestPayloadMismatchException;
import com.aegisledger.idempotency.domain.IdempotencyRecord;
import com.aegisledger.idempotency.domain.IdempotencyStatus;
import com.aegisledger.idempotency.repository.IdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service managing idempotency locking and cached response retrieval.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private final IdempotencyRepository repository;

    public IdempotencyService(IdempotencyRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyRecord> tryAcquire(String key, String requestHash) {
        Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (requestHash != null && !requestHash.equals(record.getRequestHash())) {
                log.warn("Payload hash mismatch for idempotency key {}: existing={}, incoming={}",
                    key, record.getRequestHash(), requestHash);
                throw new RequestPayloadMismatchException(key);
            }
            if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                log.warn("Concurrent duplicate request for key {}", key);
                throw new DuplicateRequestException(key);
            }
            log.info("Returning cached response for idempotency key {}", key);
            return Optional.of(record);
        }

        try {
            IdempotencyRecord newRecord = new IdempotencyRecord(UUID.randomUUID(), key, requestHash);
            repository.saveAndFlush(newRecord);
            return Optional.empty();
        } catch (DataIntegrityViolationException ex) {
            log.warn("Race condition during idempotency acquire for key {}", key);
            throw new DuplicateRequestException(key);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String key, int statusCode, String responseBody) {
        repository.findByIdempotencyKey(key).ifPresent(record -> {
            record.markCompleted(statusCode, responseBody);
            repository.save(record);
            log.debug("Marked idempotency key {} as COMPLETED", key);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(String key) {
        repository.findByIdempotencyKey(key).ifPresent(record -> {
            repository.delete(record);
            log.debug("Released idempotency key {} on failure for retry", key);
        });
    }
}
