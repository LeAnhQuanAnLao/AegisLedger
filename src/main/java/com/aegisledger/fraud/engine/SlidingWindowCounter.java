package com.aegisledger.fraud.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe sliding window counter for velocity checks with automatic memory eviction.
 * Uses bounded deques to track transaction timestamps per account.
 */
@Component
public class SlidingWindowCounter {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowCounter.class);
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
            if (windowDeque.isEmpty()) {
                accountWindows.remove(accountId, windowDeque);
            }
            return windowDeque.size();
        }
    }

    public int evictExpiredWindows(Instant now, Duration maxWindow) {
        Instant cutoff = now.minus(maxWindow);
        int evictedAccounts = 0;

        for (Map.Entry<UUID, Deque<Instant>> entry : accountWindows.entrySet()) {
            Deque<Instant> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                    deque.pollFirst();
                }
                if (deque.isEmpty()) {
                    if (accountWindows.remove(entry.getKey(), deque)) {
                        evictedAccounts++;
                    }
                }
            }
        }

        if (evictedAccounts > 0) {
            log.debug("Evicted {} inactive account windows from memory", evictedAccounts);
        }
        return evictedAccounts;
    }

    @Scheduled(fixedDelay = 60000)
    public void scheduledCleanup() {
        evictExpiredWindows(Instant.now(), Duration.ofMinutes(5));
    }

    public int getTrackedAccountCount() {
        return accountWindows.size();
    }

    public void clear(UUID accountId) {
        accountWindows.remove(accountId);
    }
}
