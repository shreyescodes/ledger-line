package dev.ledgerline.testsupport.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * Health, readiness and metrics checks every service must pass. A service's test extends this
 * class and adds {@code @SpringBootTest(webEnvironment = RANDOM_PORT)}.
 */
public abstract class ActuatorContractTest {

    @Value("${local.server.port}")
    private int port;

    @Value("${spring.application.name}")
    private String applicationName;

    /** Health components that must be UP before the service takes traffic. */
    protected Set<String> readinessComponents() {
        return Set.of("readinessState");
    }

    @Test
    public void livenessIsUp() throws Exception {
        HttpResponse<String> response = get("/actuator/health/liveness");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    public void readinessIsUpWithItsComponents() throws Exception {
        HttpResponse<String> response = get("/actuator/health/readiness");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
        for (String component : readinessComponents()) {
            assertThat(response.body()).contains("\"" + component + "\":{\"status\":\"UP\"}");
        }
    }

    @Test
    public void prometheusMetricsAreTaggedWithTheApplicationName() throws Exception {
        HttpResponse<String> response = get("/actuator/prometheus");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("jvm_memory_used_bytes")
                .contains("application=\"" + applicationName + "\"");
    }

    protected HttpResponse<String> get(String path) throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
