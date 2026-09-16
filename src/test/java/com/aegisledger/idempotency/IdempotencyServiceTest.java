package com.aegisledger.idempotency;

import com.aegisledger.core.exception.DuplicateRequestException;
import com.aegisledger.idempotency.domain.IdempotencyRecord;
import com.aegisledger.idempotency.domain.IdempotencyStatus;
import com.aegisledger.idempotency.repository.IdempotencyRepository;
import com.aegisledger.idempotency.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for IdempotencyService (Tier 3)")
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRepository repository;

    @InjectMocks
    private IdempotencyService service;

    private String key;

    @BeforeEach
    void setUp() {
        key = "IDEM-TEST-123";
    }

    @Test
    @DisplayName("Should successfully acquire lock on new idempotency key")
    void testAcquireNewKey() {
        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.empty());

        Optional<IdempotencyRecord> result = service.tryAcquire(key, "HASH-1");

        assertTrue(result.isEmpty());
        verify(repository).saveAndFlush(any(IdempotencyRecord.class));
    }

    @Test
    @DisplayName("Should throw DuplicateRequestException when key is IN_PROGRESS")
    void testAcquireInProgressThrowsException() {
        IdempotencyRecord inProgressRecord = new IdempotencyRecord(UUID.randomUUID(), key, "HASH-1");
        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.of(inProgressRecord));

        assertThrows(DuplicateRequestException.class, () -> service.tryAcquire(key, "HASH-1"));
    }

    @Test
    @DisplayName("Should return cached record when key is already COMPLETED")
    void testAcquireCompletedReturnsRecord() {
        IdempotencyRecord completedRecord = new IdempotencyRecord(UUID.randomUUID(), key, "HASH-1");
        completedRecord.markCompleted(200, "{\"success\":true}");
        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.of(completedRecord));

        Optional<IdempotencyRecord> result = service.tryAcquire(key, "HASH-1");

        assertTrue(result.isPresent());
        assertEquals(IdempotencyStatus.COMPLETED, result.get().getStatus());
        assertEquals("{\"success\":true}", result.get().getResponseBody());
    }

    @Test
    @DisplayName("Should throw RequestPayloadMismatchException when key exists but payload hash is different")
    void testAcquireDifferentPayloadThrowsException() {
        IdempotencyRecord existingRecord = new IdempotencyRecord(UUID.randomUUID(), key, "HASH-ORIGINAL");
        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingRecord));

        assertThrows(
            com.aegisledger.core.exception.RequestPayloadMismatchException.class,
            () -> service.tryAcquire(key, "HASH-TAMPERED")
        );
    }

    @Test
    @DisplayName("Should delete in-progress idempotency key when fail() is invoked")
    void testFailReleasesKey() {
        IdempotencyRecord existingRecord = new IdempotencyRecord(UUID.randomUUID(), key, "HASH-1");
        when(repository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingRecord));

        service.fail(key);

        verify(repository).delete(existingRecord);
    }
}
