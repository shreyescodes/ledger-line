package dev.ledgerline.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ledgerline.testsupport.actuator.ActuatorContractTest;
import dev.ledgerline.testsupport.containers.PostgresContainerConfiguration;
import dev.ledgerline.testsupport.docker.RequiresDocker;
import java.util.List;
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
class LedgerServiceIntegrationTest extends ActuatorContractTest {

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

    @Test
    void databaseDefaultsToUtc() {
        List<String> settings = jdbc.queryForList(
                """
                select unnest(setconfig) from pg_db_role_setting
                where setdatabase = (select oid from pg_database where datname = current_database())
                """,
                String.class);

        assertThat(settings).anySatisfy(setting -> assertThat(setting).isEqualToIgnoringCase("timezone=UTC"));
    }
}
