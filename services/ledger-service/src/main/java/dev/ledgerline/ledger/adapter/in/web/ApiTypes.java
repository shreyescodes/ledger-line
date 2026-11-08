package dev.ledgerline.ledger.adapter.in.web;

import dev.ledgerline.ledger.application.AccountView;
import dev.ledgerline.ledger.application.InvariantReport;
import dev.ledgerline.ledger.application.StatementLine;
import dev.ledgerline.ledger.application.StatementPage;
import dev.ledgerline.ledger.domain.AccountType;
import dev.ledgerline.ledger.domain.PostedEntry;
import dev.ledgerline.ledger.domain.PostedTransaction;
import dev.ledgerline.ledger.domain.Side;
import dev.ledgerline.ledger.domain.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request and response bodies of the ledger API. Amounts are integers in minor units, capped well
 * below 2^53 so JavaScript clients read them exactly (ADR-0002).
 */
final class ApiTypes {

    static final long MAX_AMOUNT = 1_000_000_000_000L;
    static final String CURRENCY = "[A-Z]{3}";
    /** Opaque ids only: letters, digits, '-' and '_', so names and phone numbers can't slip in (I11). */
    static final String OPAQUE_REF = "[A-Za-z0-9_-]{1,64}";

    private ApiTypes() {}

    record OpenAccountRequest(
            @NotNull AccountType type,
            @NotNull @Pattern(regexp = CURRENCY) String currency,
            @Pattern(regexp = OPAQUE_REF) String holderRef) {}

    record AccountResponse(
            UUID id,
            AccountType type,
            String currency,
            String holderRef,
            long ledgerBalance,
            long availableBalance,
            long version,
            Instant createdAt) {

        static AccountResponse of(AccountView view) {
            long ledger = view.balance().ledger().amount();
            return new AccountResponse(
                    view.account().id().value(),
                    view.account().type(),
                    view.account().currency().code(),
                    view.account().holderRef(),
                    ledger,
                    ledger,
                    view.balance().version(),
                    view.account().createdAt());
        }
    }

    record EntryRequest(
            @NotNull UUID accountId,
            @NotNull Side side,
            @Positive @Max(MAX_AMOUNT) long amount,
            @NotNull @Pattern(regexp = CURRENCY) String currency) {}

    record PostTransactionRequest(
            @NotNull TransactionType type,
            @Size(max = 128) String reference,
            @NotNull @Size(min = 2, max = 50) List<@Valid @NotNull EntryRequest> entries) {}

    record TransferRequest(
            @NotNull UUID fromAccountId,
            @NotNull UUID toAccountId,
            @Positive @Max(MAX_AMOUNT) long amount,
            @NotNull @Pattern(regexp = CURRENCY) String currency,
            @Size(max = 128) String reference) {}

    record EntryResponse(UUID id, UUID accountId, Side side, long amount, String currency, long accountSeq) {}

    record TransactionResponse(
            UUID id, TransactionType type, String reference, Instant recordedAt, List<EntryResponse> entries) {

        static TransactionResponse of(PostedTransaction posted) {
            List<EntryResponse> entries =
                    posted.entries().stream().map(TransactionResponse::entry).toList();
            return new TransactionResponse(
                    posted.id().value(), posted.type(), posted.reference(), posted.recordedAt(), entries);
        }

        private static EntryResponse entry(PostedEntry entry) {
            return new EntryResponse(
                    entry.id().value(),
                    entry.account().value(),
                    entry.side(),
                    entry.amount().amount(),
                    entry.amount().currency().code(),
                    entry.accountSeq());
        }
    }

    record StatementLineResponse(
            UUID entryId,
            UUID transactionId,
            TransactionType transactionType,
            String reference,
            Instant recordedAt,
            Side side,
            long amount,
            String currency,
            long accountSeq) {

        static StatementLineResponse of(StatementLine line) {
            return new StatementLineResponse(
                    line.entryId().value(),
                    line.transactionId().value(),
                    line.transactionType(),
                    line.reference(),
                    line.recordedAt(),
                    line.side(),
                    line.amount().amount(),
                    line.amount().currency().code(),
                    line.accountSeq());
        }
    }

    /** {@code nextCursor} is null on the last page; otherwise pass it back as {@code after}. */
    record StatementResponse(List<StatementLineResponse> items, Long nextCursor) {

        static StatementResponse of(StatementPage page) {
            Long next = page.nextCursor().isPresent() ? page.nextCursor().getAsLong() : null;
            return new StatementResponse(
                    page.lines().stream().map(StatementLineResponse::of).toList(), next);
        }
    }

    record InvariantReportResponse(Instant checkedAt, boolean ok, Map<String, Long> violations) {

        static InvariantReportResponse of(InvariantReport report) {
            return new InvariantReportResponse(report.checkedAt(), report.ok(), report.violations());
        }
    }
}
