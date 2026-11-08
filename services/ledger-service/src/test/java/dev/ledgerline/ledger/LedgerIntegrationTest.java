package dev.ledgerline.ledger;

import dev.ledgerline.testsupport.containers.PostgresWithAppRoleConfiguration;
import dev.ledgerline.testsupport.docker.RequiresDocker;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * The whole service against a real Postgres, connected as the least-privilege app role. Every
 * class using this shares one Spring context and one container.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RequiresDocker
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresWithAppRoleConfiguration.class)
public @interface LedgerIntegrationTest {}
