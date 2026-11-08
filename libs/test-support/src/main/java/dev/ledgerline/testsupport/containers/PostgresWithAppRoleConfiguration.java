package dev.ledgerline.testsupport.containers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A real Postgres for a service that separates its database roles, as production does: Flyway
 * migrates as the container's superuser (the owner), and the application connects as
 * {@value #APP_ROLE}, which only has the privileges the migrations grant it.
 *
 * <p>Import it next to {@code @SpringBootTest}. The service's Flyway placeholder {@code app_role}
 * is pointed at the test role.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresWithAppRoleConfiguration {

    public static final String APP_ROLE = "app_user";
    public static final String APP_PASSWORD = "app_password";

    @Bean
    public PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(ContainerImages.POSTGRES).withInitScript("testcontainers/create-app-role.sql");
    }

    @Bean
    public DynamicPropertyRegistrar postgresProperties(PostgreSQLContainer postgres) {
        return registry -> {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", () -> APP_ROLE);
            registry.add("spring.datasource.password", () -> APP_PASSWORD);
            registry.add("spring.flyway.url", postgres::getJdbcUrl);
            registry.add("spring.flyway.user", postgres::getUsername);
            registry.add("spring.flyway.password", postgres::getPassword);
            registry.add("spring.flyway.placeholders.app_role", () -> APP_ROLE);
        };
    }
}
