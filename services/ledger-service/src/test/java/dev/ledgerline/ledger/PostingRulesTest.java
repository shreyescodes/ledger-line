package dev.ledgerline.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ledgerline.common.money.CurrencyCode;
import dev.ledgerline.common.money.Money;
import dev.ledgerline.ledger.domain.AccountBalance;
import dev.ledgerline.ledger.domain.AccountCurrencyMismatchException;
import dev.ledgerline.ledger.domain.AccountId;
import dev.ledgerline.ledger.domain.AccountType;
import dev.ledgerline.ledger.domain.EntryDraft;
import dev.ledgerline.ledger.domain.InsufficientFundsException;
import dev.ledgerline.ledger.domain.InvalidTransactionException;
import dev.ledgerline.ledger.domain.PostingPlan;
import dev.ledgerline.ledger.domain.PostingRules;
import dev.ledgerline.ledger.domain.TenantId;
import dev.ledgerline.ledger.domain.TransactionDraft;
import dev.ledgerline.ledger.domain.TransactionType;
import dev.ledgerline.ledger.domain.UnknownAccountException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The posting rules on the numbers from DOMAIN.md example E3 (merchant payment with fee). */
class PostingRulesTest {

    private static final CurrencyCode INR = CurrencyCode.of("INR");
    private final TenantId acme = new TenantId(UUID.randomUUID());
    private final AccountId alice = id();
    private final AccountId chai = id();
    private final AccountId fees = id();

    @Test
    void aMerchantPaymentWithFeeSplitsTheMoneyAndBumpsVersions() {
        Map<AccountId, AccountBalance> balances = Map.of(
                alice, balance(alice, AccountType.CUSTOMER_WALLET, -75_000, 3), // Acme owes Alice ₹750.00
                chai, balance(chai, AccountType.MERCHANT, 0, 0),
                fees, balance(fees, AccountType.FEE_REVENUE, 0, 0));

        PostingPlan plan = PostingRules.plan(
                draft(EntryDraft.debit(alice, inr(40_000)), EntryDraft.credit(chai, inr(39_200)), EntryDraft.credit(fees, inr(800))),
                balances);

        assertThat(plan.newBalances().get(alice).ledger()).isEqualTo(inr(35_000));
        assertThat(plan.newBalances().get(chai).ledger()).isEqualTo(inr(39_200));
        assertThat(plan.newBalances().get(fees).ledger()).isEqualTo(inr(800));
        assertThat(plan.entries()).extracting(PostingPlan.PlannedEntry::accountSeq).containsExactly(4L, 1L, 1L);
    }

    @Test
    void aWalletCanNotGoBelowZero() {
        Map<AccountId, AccountBalance> balances = Map.of(
                alice, balance(alice, AccountType.CUSTOMER_WALLET, -100, 1),
                chai, balance(chai, AccountType.MERCHANT, 0, 0));

        assertThatThrownBy(() -> PostingRules.plan(
                        draft(EntryDraft.debit(alice, inr(101)), EntryDraft.credit(chai, inr(101))), balances))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void aMerchantMayGoNegativeThroughAForcedDebit() {
        Map<AccountId, AccountBalance> balances = Map.of(
                chai, balance(chai, AccountType.MERCHANT, -97_199, 5),
                fees, balance(fees, AccountType.PSP_CLEARING, 0, 0));

        PostingPlan plan = PostingRules.plan(
                draft(EntryDraft.debit(chai, inr(99_999)), EntryDraft.credit(fees, inr(99_999))), balances);

        assertThat(plan.newBalances().get(chai).ledger()).isEqualTo(inr(-2_800)); // DOMAIN.md E5.1
    }

    @Test
    void entriesMustBeInTheAccountsCurrencyAndInThisTenantsBooks() {
        Map<AccountId, AccountBalance> balances = Map.of(
                alice, balance(alice, AccountType.CUSTOMER_WALLET, -100, 1),
                chai, balance(chai, AccountType.MERCHANT, 0, 0));
        Money usd = Money.of(10, "USD");

        assertThatThrownBy(() -> PostingRules.plan(draft(EntryDraft.debit(alice, usd), EntryDraft.credit(chai, usd)), balances))
                .isInstanceOf(AccountCurrencyMismatchException.class);
        AccountId stranger = id();
        assertThatThrownBy(() -> PostingRules.plan(
                        draft(EntryDraft.debit(alice, inr(10)), EntryDraft.credit(stranger, inr(10))), balances))
                .isInstanceOf(UnknownAccountException.class);
        TenantId other = new TenantId(UUID.randomUUID());
        assertThatThrownBy(() -> PostingRules.plan(
                        new TransactionDraft(other, TransactionType.TRANSFER, null, List.of(
                                EntryDraft.debit(alice, inr(10)), EntryDraft.credit(chai, inr(10)))),
                        balances))
                .as("another tenant's accounts look like they don't exist")
                .isInstanceOf(UnknownAccountException.class);
    }

    @Test
    void draftsMustHaveTwoBalancedEntriesWithPositiveAmounts() {
        assertThatThrownBy(() -> draft(EntryDraft.debit(alice, inr(10))))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("at least two");
        assertThatThrownBy(() -> draft(EntryDraft.debit(alice, inr(10)), EntryDraft.credit(chai, inr(9))))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("differ by 1");
        assertThatThrownBy(() -> EntryDraft.debit(alice, inr(0))).isInstanceOf(InvalidTransactionException.class);
        assertThat(draft(
                        EntryDraft.debit(alice, inr(10)),
                        EntryDraft.credit(chai, inr(10)),
                        EntryDraft.debit(alice, Money.of(5, "USD")),
                        EntryDraft.credit(chai, Money.of(5, "USD")))
                .entries())
                .as("each currency balances on its own")
                .hasSize(4);
    }

    private TransactionDraft draft(EntryDraft... entries) {
        return new TransactionDraft(acme, TransactionType.PAYMENT, null, List.of(entries));
    }

    private AccountBalance balance(AccountId id, AccountType type, long raw, long version) {
        return new AccountBalance(id, acme, type, INR, raw, version);
    }

    private static Money inr(long amount) {
        return Money.of(amount, INR);
    }

    private static AccountId id() {
        return new AccountId(UUID.randomUUID());
    }
}
