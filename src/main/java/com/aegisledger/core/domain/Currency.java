package com.aegisledger.core.domain;

/**
 * Standard supported currencies in AegisLedger.
 */
public enum Currency {
    USD("USD", 2),
    VND("VND", 0),
    EUR("EUR", 2);

    private final String code;
    private final int defaultFractionDigits;

    Currency(String code, int defaultFractionDigits) {
        this.code = code;
        this.defaultFractionDigits = defaultFractionDigits;
    }

    public String getCode() {
        return code;
    }

    public int getDefaultFractionDigits() {
        return defaultFractionDigits;
    }

    public static Currency fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Currency code cannot be null");
        }
        for (Currency currency : values()) {
            if (currency.code.equalsIgnoreCase(code.trim())) {
                return currency;
            }
        }
        throw new IllegalArgumentException("Unsupported currency code: " + code);
    }
}
