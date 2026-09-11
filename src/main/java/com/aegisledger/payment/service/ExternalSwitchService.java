package com.aegisledger.payment.service;

import com.aegisledger.core.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interface and mock provider for external interbank payment switch.
 */
public interface ExternalSwitchService {

    record SwitchResponse(boolean success, String referenceNumber, String errorMessage) {}

    SwitchResponse dispatchTransfer(UUID txId, UUID sourceAcc, UUID destAcc, Money amount);

    void setSimulateFailure(boolean simulate);
}
