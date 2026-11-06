package dev.ledgerline.reconciliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Matches PSP settlement files to the ledger. The Spring Batch job arrives in Stage 6. */
@SpringBootApplication
public class ReconciliationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconciliationServiceApplication.class, args);
    }
}
