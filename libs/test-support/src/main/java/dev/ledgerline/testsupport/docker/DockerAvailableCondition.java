package dev.ledgerline.testsupport.docker;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/** Decides whether a {@link RequiresDocker} test runs. */
public final class DockerAvailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            return ConditionEvaluationResult.enabled("Docker is available");
        }
        if (System.getenv("CI") != null) {
            // GitHub Actions sets CI=true. Never skip there: run and fail loudly.
            return ConditionEvaluationResult.enabled("CI must run Docker tests, so this one runs and will fail");
        }
        return ConditionEvaluationResult.disabled("Docker isn't available on this machine; this test runs in CI");
    }
}
