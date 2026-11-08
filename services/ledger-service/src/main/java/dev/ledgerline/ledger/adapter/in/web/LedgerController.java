package dev.ledgerline.ledger.adapter.in.web;

import dev.ledgerline.common.money.CurrencyCode;
import dev.ledgerline.common.money.Money;
import dev.ledgerline.ledger.adapter.in.web.ApiTypes.AccountResponse;
import dev.ledgerline.ledger.adapter.in.web.ApiTypes.InvariantReportResponse;
import dev.ledgerline.ledger.adapter.in.web.ApiTypes.OpenAccountRequest;
import dev.ledgerline.ledger.adapter.in.web.ApiTypes.PostTransactionRequest;
import dev.ledgerline.ledger.adapter.in.web.ApiTypes.StatementResponse;
import dev.ledgerline.ledger.adapter.in.web.ApiTypes.TransactionResponse;
import dev.ledgerline.ledger.adapter.in.web.ApiTypes.TransferRequest;
import dev.ledgerline.ledger.application.InvariantVerifier;
import dev.ledgerline.ledger.application.LedgerService;
import dev.ledgerline.ledger.domain.Account;
import dev.ledgerline.ledger.domain.AccountId;
import dev.ledgerline.ledger.domain.EntryDraft;
import dev.ledgerline.ledger.domain.TenantId;
import dev.ledgerline.ledger.domain.TransactionDraft;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The ledger's internal HTTP API. Every call names its tenant in the {@code X-Tenant-Id} header;
 * Stage 7 replaces that with the tenant from a verified JWT.
 */
@RestController
@RequestMapping("/v1")
class LedgerController {

    static final String TENANT_HEADER = "X-Tenant-Id";

    private final LedgerService ledger;
    private final InvariantVerifier verifier;

    LedgerController(LedgerService ledger, InvariantVerifier verifier) {
        this.ledger = ledger;
        this.verifier = verifier;
    }

    @PostMapping("/accounts")
    ResponseEntity<AccountResponse> openAccount(
            @RequestHeader(TENANT_HEADER) UUID tenant, @Valid @RequestBody OpenAccountRequest request) {
        TenantId tenantId = new TenantId(tenant);
        Account account =
                ledger.openAccount(tenantId, request.type(), CurrencyCode.of(request.currency()), request.holderRef());
        AccountResponse body = AccountResponse.of(ledger.account(tenantId, account.id()));
        return ResponseEntity.created(URI.create("/v1/accounts/" + account.id())).body(body);
    }

    @GetMapping("/accounts/{accountId}")
    AccountResponse account(@RequestHeader(TENANT_HEADER) UUID tenant, @PathVariable UUID accountId) {
        return AccountResponse.of(ledger.account(new TenantId(tenant), new AccountId(accountId)));
    }

    @GetMapping("/accounts/{accountId}/entries")
    StatementResponse statement(
            @RequestHeader(TENANT_HEADER) UUID tenant,
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") long after,
            @RequestParam(defaultValue = "50") int limit) {
        return StatementResponse.of(ledger.statement(new TenantId(tenant), new AccountId(accountId), after, limit));
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse post(
            @RequestHeader(TENANT_HEADER) UUID tenant, @Valid @RequestBody PostTransactionRequest request) {
        TransactionDraft draft = new TransactionDraft(
                new TenantId(tenant),
                request.type(),
                request.reference(),
                request.entries().stream()
                        .map(entry -> new EntryDraft(
                                new AccountId(entry.accountId()),
                                entry.side(),
                                Money.of(entry.amount(), entry.currency())))
                        .toList());
        return TransactionResponse.of(ledger.post(draft));
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse transfer(@RequestHeader(TENANT_HEADER) UUID tenant, @Valid @RequestBody TransferRequest request) {
        return TransactionResponse.of(ledger.transfer(
                new TenantId(tenant),
                new AccountId(request.fromAccountId()),
                new AccountId(request.toAccountId()),
                Money.of(request.amount(), request.currency()),
                request.reference()));
    }

    /** Runs the invariant checks now, for this tenant's books. */
    @GetMapping("/invariants")
    InvariantReportResponse invariants(@RequestHeader(TENANT_HEADER) UUID tenant) {
        return InvariantReportResponse.of(verifier.verify(new TenantId(tenant)));
    }
}
