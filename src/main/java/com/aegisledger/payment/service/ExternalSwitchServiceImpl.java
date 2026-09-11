package com.aegisledger.payment.service;

import com.aegisledger.core.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mock implementation of ExternalSwitchService supporting simulated failures.
 */
@Service
public class ExternalSwitchServiceImpl implements ExternalSwitchService {

    private static final Logger log = LoggerFactory.getLogger(ExternalSwitchServiceImpl.class);
    private final AtomicBoolean simulateFailure = new AtomicBoolean(false);

    @Override
    public SwitchResponse dispatchTransfer(UUID txId, UUID sourceAcc, UUID destAcc, Money amount) {
        log.info("Dispatching transaction {} to external interbank switch: amount={}", txId, amount);

        if (simulateFailure.get()) {
            log.warn("External switch simulated FAILURE or TIMEOUT for tx {}", txId);
            return new SwitchResponse(false, null, "SWITCH_TIMEOUT: Partner network unreachable");
        }

        String ref = "EXT-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("External switch processed successfully for tx {}: ref={}", txId, ref);
        return new SwitchResponse(true, ref, null);
    }

    @Override
    public void setSimulateFailure(boolean simulate) {
        this.simulateFailure.set(simulate);
    }
}
