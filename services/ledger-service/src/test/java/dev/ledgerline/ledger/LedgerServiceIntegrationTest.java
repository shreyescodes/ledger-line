package dev.ledgerline.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ledgerline.testsupport.actuator.ActuatorContractTest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Starts the service against a real Postgres in Docker, then checks Flyway and the endpoints. */
@LedgerIntegrationTest
class LedgerServiceIntegrationTest extends ActuatorContractTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    protected Set<String> readinessComponents() {
        return Set.of("readinessState", "db");
    }

    @Test
    void flywayAppliedEveryMigration() {
        List<String> applied = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

        assertThat(applied).containsExactly("1", "2", "3", "4");
    }

    @Test
    void databaseDefaultsToUtc() {
        List<String> settings = jdbc.queryForList("""
                select unnest(setconfig) from pg_db_role_setting
                where setdatabase = (select oid from pg_database where datname = current_database())
                """, String.class);

        assertThat(settings).anySatisfy(setting -> assertThat(setting).isEqualToIgnoringCase("timezone=UTC"));
    }

    @Test
    void theServiceConnectsAsTheLeastPrivilegeRole() {
        assertThat(jdbc.queryForObject("SELECT current_user", String.class)).isEqualTo("app_user");
    }

    @Test
    void invariantMetricsAreExported() throws Exception {
        HttpResponse<String> response = get("/actuator/prometheus");

        assertThat(response.body())
                .contains("ledger_invariant_violations")
                .contains("ledger_invariant_last_verified_seconds");
    }
}
