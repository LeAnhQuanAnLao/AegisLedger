package com.aegisledger.fraud.engine;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe sliding window counter for velocity checks.
 * Uses bounded deques to track transaction timestamps per account.
 */
@Component
public class SlidingWindowCounter {

    private final Map<UUID, Deque<Instant>> accountWindows = new ConcurrentHashMap<>();

    public int recordAndCount(UUID accountId, Instant eventTime, Duration window) {
        Deque<Instant> windowDeque = accountWindows.computeIfAbsent(accountId, k -> new ArrayDeque<>());

        synchronized (windowDeque) {
            Instant cutoff = eventTime.minus(window);
            // Evict expired entries older than the sliding window cutoff
            while (!windowDeque.isEmpty() && windowDeque.peekFirst().isBefore(cutoff)) {
                windowDeque.pollFirst();
            }
            windowDeque.addLast(eventTime);
            return windowDeque.size();
        }
    }

    public int countInWindow(UUID accountId, Instant currentTime, Duration window) {
        Deque<Instant> windowDeque = accountWindows.get(accountId);
        if (windowDeque == null) {
            return 0;
        }

        synchronized (windowDeque) {
            Instant cutoff = currentTime.minus(window);
            while (!windowDeque.isEmpty() && windowDeque.peekFirst().isBefore(cutoff)) {
                windowDeque.pollFirst();
            }
            return windowDeque.size();
        }
    }

    public void clear(UUID accountId) {
        accountWindows.remove(accountId);
    }
}
