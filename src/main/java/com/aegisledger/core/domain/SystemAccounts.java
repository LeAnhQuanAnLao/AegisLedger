package com.aegisledger.core.domain;

import java.util.UUID;

/**
 * Predefined System Accounts used for strict double-entry bookkeeping across banking engines.
 */
public final class SystemAccounts {

    private SystemAccounts() {
    }

    // 1. Bank Treasury Account (Used for loan disbursements, receiving principal repayments)
    public static final UUID TREASURY_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String TREASURY_ACCOUNT_NUMBER = "SYS-TREASURY";

    // 2. Interest Expense Account (Used for paying interest on savings accounts)
    public static final UUID INTEREST_EXPENSE_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final String INTEREST_EXPENSE_ACCOUNT_NUMBER = "SYS-INTEREST-EXP";

    // 3. Interest Income Account (Used for collecting interest on loans)
    public static final UUID INTEREST_INCOME_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final String INTEREST_INCOME_ACCOUNT_NUMBER = "SYS-INTEREST-INC";

    // 4. Fee Revenue Account (Used for collecting transaction and service fees)
    public static final UUID FEE_REVENUE_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    public static final String FEE_REVENUE_ACCOUNT_NUMBER = "SYS-FEE-REV";

    // 5. Savings Vault Liability Account (Holds customer deposits committed to savings)
    public static final UUID SAVINGS_VAULT_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    public static final String SAVINGS_VAULT_ACCOUNT_NUMBER = "SYS-SAVINGS-VAULT";
}
