package com.aegisledger.simulation.service;

import com.aegisledger.account.domain.Account;
import java.util.List;

/**
 * Service for bulk seeding simulated user accounts, initial limits, and double-entry deposits.
 */
public interface UserSeedService {

    List<Account> seedUsers(int count, boolean seedInitialDeposit);
}
