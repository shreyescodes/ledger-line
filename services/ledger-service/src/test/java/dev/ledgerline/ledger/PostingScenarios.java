package dev.ledgerline.ledger;

import dev.ledgerline.ledger.application.PostingStrategyName;
import dev.ledgerline.ledger.domain.AccountType;
import dev.ledgerline.ledger.domain.Side;
import dev.ledgerline.testsupport.property.PropertyCheck;
import dev.ledgerline.testsupport.property.Shrinkers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

/**
 * Random ledger scenarios for the property tests: a handful of accounts of random types in INR
 * and USD, and a sequence of balanced postings between them. About one posting in ten puts an
 * entry on an account of the wrong currency, and many postings overdraw a non-negative account,
 * so refusals are exercised as much as successes.
 */
final class PostingScenarios {

    private static final String[] CURRENCIES = {"INR", "USD"};

    private PostingScenarios() {}

    record AccountSpec(AccountType type, String currency) {}

    record Leg(int account, Side side, long amount, String currency) {}

    record Op(List<Leg> legs) {
        Op {
            legs = List.copyOf(legs);
        }
    }

    record Scenario(PostingStrategyName strategy, List<AccountSpec> accounts, List<Op> ops) {
        Scenario {
            accounts = List.copyOf(accounts);
            ops = List.copyOf(ops);
        }

        Scenario withOps(List<Op> fewer) {
            return new Scenario(strategy, accounts, fewer);
        }
    }

    enum Outcome {
        POSTED,
        INSUFFICIENT_FUNDS,
        CURRENCY_MISMATCH
    }

    static Scenario generate(SplittableRandom random) {
        PostingStrategyName strategy =
                PostingStrategyName.values()[random.nextInt(PostingStrategyName.values().length)];
        List<AccountSpec> accounts = new ArrayList<>();
        int accountCount = random.nextInt(3, 9);
        for (int i = 0; i < accountCount; i++) {
            AccountType type = AccountType.values()[random.nextInt(AccountType.values().length)];
            String currency = random.nextInt(5) == 0 ? CURRENCIES[1] : CURRENCIES[0];
            accounts.add(new AccountSpec(type, currency));
        }
        List<Op> ops = new ArrayList<>();
        int opCount = random.nextInt(1, 30);
        for (int i = 0; i < opCount; i++) {
            nextOp(random, accounts).ifPresent(ops::add);
        }
        return new Scenario(strategy, accounts, ops);
    }

    static PropertyCheck.Shrinker<Scenario> shrinker() {
        PropertyCheck.Shrinker<List<Op>> ops = Shrinkers.list();
        return scenario -> ops.candidates(scenario.ops()).map(scenario::withOps);
    }

    private static Optional<Op> nextOp(SplittableRandom random, List<AccountSpec> accounts) {
        String currency = CURRENCIES[random.nextInt(5) == 0 ? 1 : 0];
        List<Integer> sameCurrency = IntStream.range(0, accounts.size())
                .filter(i -> accounts.get(i).currency().equals(currency))
                .boxed()
                .toList();
        if (sameCurrency.size() < 2) {
            return Optional.empty();
        }
        int first = sameCurrency.get(random.nextInt(sameCurrency.size()));
        int second = pickOther(random, sameCurrency, first);
        Side side = random.nextBoolean() ? Side.DEBIT : Side.CREDIT;
        if (random.nextInt(10) == 0) {
            // Put the second leg on an account of the other currency: the ledger must refuse it.
            List<Integer> other = IntStream.range(0, accounts.size())
                    .filter(i -> !accounts.get(i).currency().equals(currency))
                    .boxed()
                    .toList();
            if (!other.isEmpty()) {
                second = other.get(random.nextInt(other.size()));
            }
        }
        long amount = random.nextLong(1, 5_001);
        if (sameCurrency.size() >= 3 && random.nextInt(3) == 0) {
            // A three-legged posting, like a payment split between merchant and fee revenue.
            int third = pickOther(random, sameCurrency, first, second);
            long part = random.nextLong(1, amount + 1);
            long extra = random.nextLong(1, 5_001);
            return Optional.of(new Op(List.of(
                    new Leg(first, side, amount + extra, currency),
                    new Leg(second, side.opposite(), amount, currency),
                    new Leg(third, side.opposite(), extra, currency))));
        }
        return Optional.of(new Op(List.of(
                new Leg(first, side, amount, currency), new Leg(second, side.opposite(), amount, currency))));
    }

    private static int pickOther(SplittableRandom random, List<Integer> candidates, int... taken) {
        List<Integer> free = new ArrayList<>(candidates);
        for (int t : taken) {
            free.remove(Integer.valueOf(t));
        }
        return free.isEmpty() ? candidates.get(0) : free.get(random.nextInt(free.size()));
    }

    /**
     * An independent oracle for the ledger rules, written without {@code PostingRules}: tracks raw
     * (debit-positive) balances and predicts each posting's outcome.
     */
    static final class Model {

        private final List<AccountSpec> accounts;
        final long[] raw;
        int postedLegs;

        Model(List<AccountSpec> accounts) {
            this.accounts = accounts;
            this.raw = new long[accounts.size()];
        }

        Outcome apply(Op op) {
            for (Leg leg : op.legs()) {
                if (!accounts.get(leg.account()).currency().equals(leg.currency())) {
                    return Outcome.CURRENCY_MISMATCH;
                }
            }
            long[] next = raw.clone();
            for (Leg leg : op.legs()) {
                next[leg.account()] += leg.side() == Side.DEBIT ? leg.amount() : -leg.amount();
            }
            for (int i = 0; i < next.length; i++) {
                AccountType type = accounts.get(i).type();
                boolean creditNormal = type.category().normalSide() == Side.CREDIT;
                long balance = creditNormal ? -next[i] : next[i];
                if (type.nonNegative() && balance < 0 && next[i] != raw[i]) {
                    return Outcome.INSUFFICIENT_FUNDS;
                }
            }
            System.arraycopy(next, 0, raw, 0, raw.length);
            postedLegs += op.legs().size();
            return Outcome.POSTED;
        }
    }
}
