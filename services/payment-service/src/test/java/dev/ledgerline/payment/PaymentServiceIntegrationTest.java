package dev.ledgerline.payment;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ledgerline.testsupport.actuator.ActuatorContractTest;
import dev.ledgerline.testsupport.containers.PostgresContainerConfiguration;
import dev.ledgerline.testsupport.docker.RequiresDocker;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Starts the service against a real Postgres in Docker, then checks Flyway and the endpoints. */
@RequiresDocker
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresContainerConfiguration.class)
class PaymentServiceIntegrationTest extends ActuatorContractTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    protected Set<String> readinessComponents() {
        return Set.of("readinessState", "db");
    }

    @Test
    void flywayAppliedTheBaselineMigration() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success", Integer.class);

        assertThat(applied).isEqualTo(1);
    }
}
