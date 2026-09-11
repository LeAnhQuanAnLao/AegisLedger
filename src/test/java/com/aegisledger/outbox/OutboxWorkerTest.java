package com.aegisledger.outbox;

import com.aegisledger.outbox.domain.OutboxEvent;
import com.aegisledger.outbox.domain.OutboxStatus;
import com.aegisledger.outbox.repository.OutboxRepository;
import com.aegisledger.outbox.worker.OutboxWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for OutboxWorker (Tier 3)")
class OutboxWorkerTest {

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private OutboxWorker outboxWorker;

    @Test
    @DisplayName("Should fetch pending outbox events and mark them PROCESSED")
    void testProcessPendingEventsSuccess() {
        OutboxEvent event = new OutboxEvent(
            UUID.randomUUID(),
            "TRANSACTION",
            "TX-100",
            "PAYMENT_COMPLETED",
            "{\"amount\":100}"
        );
        when(outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
            .thenReturn(List.of(event));

        outboxWorker.processPendingEvents();

        assertEquals(OutboxStatus.PROCESSED, event.getStatus());
        assertNotNull(event.getProcessedAt());
        verify(outboxRepository).save(event);
    }
}
