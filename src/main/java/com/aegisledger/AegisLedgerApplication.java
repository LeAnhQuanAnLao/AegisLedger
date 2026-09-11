package com.aegisledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entrypoint for AegisLedger Core Banking & Payment Orchestration Engine.
 */
@SpringBootApplication
@EnableScheduling
public class AegisLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AegisLedgerApplication.class, args);
    }
}
