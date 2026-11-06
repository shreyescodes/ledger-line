package dev.ledgerline.testsupport.containers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A real Postgres for integration tests. Import it with {@code @Import} next to
 * {@code @SpringBootTest}; {@code @ServiceConnection} points the app's DataSource at the container.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfiguration {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(ContainerImages.POSTGRES);
    }
}
