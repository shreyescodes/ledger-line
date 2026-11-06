package dev.ledgerline.psp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Simulated bank/PSP. The payment API, failure modes and settlement files arrive in Stage 5. */
@SpringBootApplication
public class MockPspApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockPspApplication.class, args);
    }
}
