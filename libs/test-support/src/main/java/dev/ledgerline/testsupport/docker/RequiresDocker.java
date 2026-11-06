package dev.ledgerline.testsupport.docker;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks a test class that needs Docker (Testcontainers).
 *
 * <p>On a machine without Docker the test is skipped with a clear reason. In CI it always runs, so
 * a broken Docker setup fails the build instead of silently skipping the integration tests.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DockerAvailableCondition.class)
public @interface RequiresDocker {}
