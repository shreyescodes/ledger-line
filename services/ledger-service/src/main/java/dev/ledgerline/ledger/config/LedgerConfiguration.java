package dev.ledgerline.ledger.config;

import dev.ledgerline.common.id.IdGenerator;
import dev.ledgerline.common.id.TimeOrderedIdGenerator;
import dev.ledgerline.ledger.application.AccountStore;
import dev.ledgerline.ledger.application.InvariantQueries;
import dev.ledgerline.ledger.application.InvariantVerifier;
import dev.ledgerline.ledger.application.LedgerService;
import dev.ledgerline.ledger.application.PostingOptions;
import dev.ledgerline.ledger.application.PostingStrategy;
import dev.ledgerline.ledger.application.StatementQuery;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Wires the plain-Java application layer to its adapters. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(LedgerProperties.class)
class LedgerConfiguration {

    @Bean
    IdGenerator idGenerator(Clock clock) {
        return new TimeOrderedIdGenerator(clock);
    }

    @Bean
    PostingOptions postingOptions(LedgerProperties properties) {
        return new PostingOptions(properties.posting().maxAttempts());
    }

    /** All three strategies are beans; this picks the one postings use. Tests may call any of them. */
    @Bean
    LedgerService ledgerService(
            AccountStore accounts,
            List<PostingStrategy> strategies,
            StatementQuery statements,
            IdGenerator ids,
            Clock clock,
            LedgerProperties properties) {
        PostingStrategy chosen = strategies.stream()
                .filter(strategy -> strategy.name() == properties.posting().strategy())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "no posting strategy named " + properties.posting().strategy()));
        return new LedgerService(accounts, chosen, statements, ids, clock);
    }

    @Bean
    InvariantVerifier invariantVerifier(InvariantQueries queries, Clock clock) {
        return new InvariantVerifier(queries, clock);
    }
}
