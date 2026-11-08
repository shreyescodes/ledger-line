package dev.ledgerline.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/** The ledger's HTTP API, end to end: accounts, transfers, statements, refusals and tenant isolation. */
@LedgerIntegrationTest
class LedgerApiIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private RestClient http;
    private final UUID tenant = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {})
                .build();
    }

    @Test
    void openAccountReturnsItWithAZeroBalance() {
        ResponseEntity<JsonNode> response =
                post(tenant, "/v1/accounts", Map.of("type", "CUSTOMER_WALLET", "currency", "INR"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        JsonNode body = response.getBody();
        assertThat(body.get("type").asString()).isEqualTo("CUSTOMER_WALLET");
        assertThat(body.get("currency").asString()).isEqualTo("INR");
        assertThat(body.get("ledgerBalance").asLong()).isZero();
    }

    @Test
    void transferMovesMoneyAndShowsUpOnBothStatements() {
        String clearing = open("PSP_CLEARING");
        String alice = open("CUSTOMER_WALLET");
        String bob = open("CUSTOMER_WALLET");
        fund(clearing, alice, 100_000);

        ResponseEntity<JsonNode> transfer = post(
                tenant,
                "/v1/transfers",
                Map.of(
                        "fromAccountId",
                        alice,
                        "toAccountId",
                        bob,
                        "amount",
                        25_000,
                        "currency",
                        "INR",
                        "reference",
                        "p2p-1"));

        assertThat(transfer.getStatusCode().value()).isEqualTo(201);
        assertThat(balance(alice)).isEqualTo(75_000);
        assertThat(balance(bob)).isEqualTo(25_000);
        JsonNode bobStatement = get(tenant, "/v1/accounts/" + bob + "/entries").getBody();
        assertThat(bobStatement.get("items")).hasSize(1);
        assertThat(bobStatement.get("items").get(0).get("side").asString()).isEqualTo("CREDIT");
        assertThat(bobStatement.get("items").get(0).get("reference").asString()).isEqualTo("p2p-1");
    }

    @Test
    void statementsPageThroughEveryEntryExactlyOnce() {
        String clearing = open("PSP_CLEARING");
        String alice = open("CUSTOMER_WALLET");
        for (int i = 1; i <= 7; i++) {
            fund(clearing, alice, i * 100L);
        }

        List<Long> seen = new ArrayList<>();
        Long cursor = 0L;
        int pages = 0;
        while (cursor != null) {
            JsonNode page = get(tenant, "/v1/accounts/" + alice + "/entries?limit=3&after=" + cursor)
                    .getBody();
            page.get("items").forEach(item -> seen.add(item.get("accountSeq").asLong()));
            cursor = page.get("nextCursor").isNull()
                    ? null
                    : page.get("nextCursor").asLong();
            pages++;
        }

        assertThat(pages).isEqualTo(3);
        assertThat(seen).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L);
    }

    @Test
    void anOverdraftIsRefusedAndChangesNothing() {
        String clearing = open("PSP_CLEARING");
        String alice = open("CUSTOMER_WALLET");
        String bob = open("CUSTOMER_WALLET");
        fund(clearing, alice, 1_000);

        ResponseEntity<JsonNode> response = post(
                tenant,
                "/v1/transfers",
                Map.of("fromAccountId", alice, "toAccountId", bob, "amount", 1_001, "currency", "INR"));

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody().get("type").asString()).isEqualTo("urn:ledgerline:problem:insufficient-funds");
        assertThat(balance(alice)).isEqualTo(1_000);
        assertThat(balance(bob)).isZero();
    }

    @Test
    void anUnbalancedTransactionIsRefused() {
        String alice = open("CUSTOMER_WALLET");
        String bob = open("CUSTOMER_WALLET");

        ResponseEntity<JsonNode> response = post(
                tenant,
                "/v1/transactions",
                Map.of(
                        "type",
                        "ADJUSTMENT",
                        "entries",
                        List.of(
                                Map.of("accountId", alice, "side", "DEBIT", "amount", 100, "currency", "INR"),
                                Map.of("accountId", bob, "side", "CREDIT", "amount", 99, "currency", "INR"))));

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody().get("type").asString()).isEqualTo("urn:ledgerline:problem:invalid-transaction");
    }

    @Test
    void anotherTenantCanNotSeeOrUseTheseAccounts() {
        String clearing = open("PSP_CLEARING");
        String alice = open("CUSTOMER_WALLET");
        String bob = open("CUSTOMER_WALLET");
        fund(clearing, alice, 1_000);
        UUID intruder = UUID.randomUUID();

        assertThat(get(intruder, "/v1/accounts/" + alice).getStatusCode().value())
                .isEqualTo(404);
        ResponseEntity<JsonNode> transfer = post(
                intruder,
                "/v1/transfers",
                Map.of("fromAccountId", alice, "toAccountId", bob, "amount", 500, "currency", "INR"));
        assertThat(transfer.getStatusCode().value()).isEqualTo(404);
        assertThat(balance(alice)).isEqualTo(1_000);
    }

    @Test
    void badRequestsAreRejectedBeforeTheLedger() {
        assertThat(http.post()
                        .uri("/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("type", "CUSTOMER_WALLET", "currency", "INR"))
                        .retrieve()
                        .toEntity(JsonNode.class)
                        .getStatusCode()
                        .value())
                .as("missing tenant header")
                .isEqualTo(400);
        String alice = open("CUSTOMER_WALLET");
        String bob = open("CUSTOMER_WALLET");
        assertThat(post(
                                tenant,
                                "/v1/transfers",
                                Map.of("fromAccountId", alice, "toAccountId", bob, "amount", 0, "currency", "INR"))
                        .getStatusCode()
                        .value())
                .as("zero amount")
                .isEqualTo(400);
        assertThat(post(
                                tenant,
                                "/v1/accounts",
                                Map.of("type", "CUSTOMER_WALLET", "currency", "INR", "holderRef", "Alice Smith"))
                        .getStatusCode()
                        .value())
                .as("holder refs are opaque ids, not names")
                .isEqualTo(400);
    }

    @Test
    void theInvariantEndpointReportsHealthyBooks() {
        String clearing = open("PSP_CLEARING");
        String alice = open("CUSTOMER_WALLET");
        fund(clearing, alice, 5_000);

        JsonNode report = get(tenant, "/v1/invariants").getBody();

        assertThat(report.get("ok").asBoolean()).isTrue();
        JsonNode violations = report.get("violations");
        for (String check : List.of("books_sum_to_zero", "balances_match_entries", "no_overdrawn_accounts")) {
            assertThat(violations.has(check)).as("report includes %s", check).isTrue();
            assertThat(violations.get(check).asLong()).as(check).isZero();
        }
    }

    private String open(String type) {
        return post(tenant, "/v1/accounts", Map.of("type", type, "currency", "INR"))
                .getBody()
                .get("id")
                .asString();
    }

    private void fund(String clearing, String account, long amount) {
        ResponseEntity<JsonNode> response = post(
                tenant,
                "/v1/transactions",
                Map.of(
                        "type",
                        "TOP_UP",
                        "entries",
                        List.of(
                                Map.of("accountId", clearing, "side", "DEBIT", "amount", amount, "currency", "INR"),
                                Map.of("accountId", account, "side", "CREDIT", "amount", amount, "currency", "INR"))));
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }

    private long balance(String account) {
        return get(tenant, "/v1/accounts/" + account)
                .getBody()
                .get("ledgerBalance")
                .asLong();
    }

    private ResponseEntity<JsonNode> post(UUID tenantId, String path, Object body) {
        return http.post()
                .uri(path)
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(JsonNode.class);
    }

    private ResponseEntity<JsonNode> get(UUID tenantId, String path) {
        return http.get()
                .uri(path)
                .header("X-Tenant-Id", tenantId.toString())
                .retrieve()
                .toEntity(JsonNode.class);
    }
}
