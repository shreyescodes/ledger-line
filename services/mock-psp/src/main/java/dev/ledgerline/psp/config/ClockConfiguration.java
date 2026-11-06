package dev.ledgerline.psp.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    /** The only place that reads the system clock (invariant I10). Everything else injects this. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
