package com.aegisledger.savings.domain;

/**
 * Maturity handling options for savings accounts.
 */
public enum RolloverOption {
    AUTO_SETTLE,                        // Settle principal + interest back to checking account
    ROLLOVER_PRINCIPAL_AND_INTEREST,    // Add interest to principal and renew for another term
    ROLLOVER_PRINCIPAL                  // Settle interest to checking and renew principal
}
