/*
 * DomainCheck replays the worked examples in DOMAIN.md and fails if the money doesn't add up.
 *
 * Run it with Java 21+. A single source file runs directly, with no build step:
 *
 *     java tools/domain-check/DomainCheck.java DOMAIN.md
 *
 * It recognises Markdown tables by their header row:
 *
 *     | Type | Category | Normal side | Non-negative | ... |   account types
 *     | Account | Type | Currency |                           accounts used by the examples
 *     | Account | Currency | Debit | Credit |                 a posting (one ledger transaction)
 *     | Hold | Account | Action | Amount | [Result] |        hold events: AUTHORIZE, VOID, EXPIRE
 *     | Account | Ledger | Held | Available |                balance checks against the replay
 *
 * Postings and hold events are "steps". Each needs a caption line just above it that
 * starts with its id, e.g. "**E2.2** ... captures H1". A caption that says
 * "captures H<n>" turns the posting into the capture of that hold.
 *
 * A variant is wrapped in
 *     <!-- branch NAME from STEP --> ... <!-- end branch -->
 * It starts from the state right after STEP and doesn't affect anything after it.
 *
 * For a file of deliberate mistakes (a test of this checker), add
 *     <!-- expect-problems: N -->
 * and the run passes only if exactly N problems are found.
 *
 * Checks:
 *   - a posting has at least two entries, positive whole-number amounts, entry currency equal
 *     to the account's currency, and total debits equal to total credits for each currency;
 *   - AUTHORIZE needs available >= amount; a capture debits the held account by at most the
 *     held amount; every hold is resolved exactly once;
 *   - non-negative accounts never have available < 0;
 *   - raw (debit-positive) balances sum to zero for each currency after every step;
 *   - each balance table matches the replay.
 * Each step is applied to a copy of the books and kept only if all checks pass, the same way a
 * database transaction commits or rolls back.
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DomainCheck {

    // ----- Domain vocabulary (DOMAIN.md sections 2 to 6) -----

    enum Side { DEBIT, CREDIT }

    /** A row of the account types table. */
    record AccountType(String name, String category, Side normalSide, boolean nonNegative) {}

    record Account(String code, AccountType type, String currency) {
        /** Turns a raw (debit-positive) balance into the account's normal direction. */
        long normal(long raw) {
            return type.normalSide() == Side.DEBIT ? raw : Math.negateExact(raw);
        }
    }

    record Entry(String account, Side side, long amount, String currency) {
        long signed() {
            return side == Side.DEBIT ? amount : -amount;
        }
    }

    record Hold(String id, String account, long amount) {}

    record Row(int line, List<String> cells) {
        String cell(int i) {
            return i < cells.size() ? clean(cells.get(i)) : "";
        }
    }

    record Table(int line, List<String> header, List<Row> rows) {}

    /** The replayed books. Copied for snapshots, so a variant can start from any earlier step. */
    static final class State {
        final Map<String, Long> raw = new HashMap<>();             // account code -> debit-positive balance
        final Map<String, Hold> activeHolds = new LinkedHashMap<>();
        final Map<String, String> resolvedHolds = new HashMap<>();  // hold id -> how it was resolved

        State copy() {
            State c = new State();
            c.raw.putAll(raw);
            c.activeHolds.putAll(activeHolds);
            c.resolvedHolds.putAll(resolvedHolds);
            return c;
        }

        long raw(String account) {
            return raw.getOrDefault(account, 0L);
        }

        long held(String account) {
            long sum = 0;
            for (Hold h : activeHolds.values()) {
                if (h.account().equals(account)) {
                    sum = Math.addExact(sum, h.amount());
                }
            }
            return sum;
        }
    }

    private static final Pattern CAPTION = Pattern.compile("^\\*\\*(E\\d+(?:\\.[A-Za-z0-9]+)+)\\*\\*(.*)$");
    private static final Pattern CAPTURES = Pattern.compile("\\bcaptures\\s+(H\\d+)\\b");
    private static final Pattern BRANCH_START = Pattern.compile("^<!--\\s*branch\\s+(\\S+)\\s+from\\s+(\\S+)\\s*-->$");
    private static final Pattern BRANCH_END = Pattern.compile("^<!--\\s*end branch\\s*-->$");
    private static final Pattern EXPECT = Pattern.compile("^<!--\\s*expect-problems:\\s*(\\d+)\\s*-->$");
    private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");
    private static final String EM_DASH = "—"; // tables use it for "not shown"

    private final Map<String, AccountType> types = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final Map<String, State> snapshots = new HashMap<>();
    private final Set<String> stepIds = new HashSet<>();
    private final List<String> problems = new ArrayList<>();

    private State state = new State();
    private State savedMainLine;
    private String savedLastStep;
    private String branch;
    private String lastStep = "the start";
    private Integer expectedProblems;
    private int postings;
    private int holdEvents;
    private int balanceRows;
    private int variants;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("usage: java tools/domain-check/DomainCheck.java <markdown file>");
            System.exit(2);
        }
        Path file = Path.of(args[0]);
        DomainCheck check = new DomainCheck();
        check.run(Files.readAllLines(file, StandardCharsets.UTF_8));
        boolean passed = check.report(file.toString().replace('\\', '/'));
        System.exit(passed ? 0 : 1);
    }

    // ----- Reading the document -----

    void run(List<String> lines) {
        String captionId = null;
        String captionText = "";
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i).trim();
            int lineNo = i + 1;
            Matcher m;
            if ((m = CAPTION.matcher(line)).matches()) {
                if (captionId != null) {
                    problem(lineNo, "caption " + captionId + " has no posting or hold table under it");
                }
                captionId = m.group(1);
                captionText = m.group(2);
                i++;
            } else if ((m = BRANCH_START.matcher(line)).matches()) {
                startBranch(m.group(1), m.group(2), lineNo);
                i++;
            } else if (BRANCH_END.matcher(line).matches()) {
                endBranch(lineNo);
                i++;
            } else if ((m = EXPECT.matcher(line)).matches()) {
                expectedProblems = Integer.parseInt(m.group(1));
                i++;
            } else if (line.startsWith("|")) {
                List<String> block = new ArrayList<>();
                List<Integer> lineNumbers = new ArrayList<>();
                while (i < lines.size() && lines.get(i).trim().startsWith("|")) {
                    block.add(lines.get(i));
                    lineNumbers.add(i + 1);
                    i++;
                }
                Table table = parseTable(block, lineNumbers);
                String kind = kindOf(table.header());
                switch (kind) {
                    case "types" -> readTypes(table);
                    case "accounts" -> readAccounts(table);
                    case "balances" -> checkBalances(table);
                    case "posting", "holds" -> {
                        if (captionId == null) {
                            problem(lineNo, "this " + kind + " table needs a **E<n>.<m>** caption line above it");
                        } else if (kind.equals("posting")) {
                            posting(captionId, captionText, table);
                        } else {
                            holdEvents(captionId, table);
                        }
                        captionId = null;
                    }
                    default -> { } // a prose table, such as the glossary
                }
            } else {
                i++;
            }
        }
        if (branch != null) {
            problem(lines.size(), "variant '" + branch + "' is never closed with <!-- end branch -->");
        }
    }

    static Table parseTable(List<String> block, List<Integer> lineNumbers) {
        List<String> header = cells(block.get(0));
        List<Row> rows = new ArrayList<>();
        for (int k = 1; k < block.size(); k++) {
            List<String> cells = cells(block.get(k));
            boolean separator = cells.stream().allMatch(c -> c.matches(":?-+:?"));
            if (!separator) {
                rows.add(new Row(lineNumbers.get(k), cells));
            }
        }
        return new Table(lineNumbers.get(0), header, rows);
    }

    static List<String> cells(String line) {
        String t = line.trim();
        if (t.startsWith("|")) {
            t = t.substring(1);
        }
        if (t.endsWith("|")) {
            t = t.substring(0, t.length() - 1);
        }
        List<String> out = new ArrayList<>();
        for (String c : t.split("\\|", -1)) {
            out.add(c.trim());
        }
        return out;
    }

    /** Removes Markdown emphasis and code marks, so `wallet:alice` and **100** read as plain values. */
    static String clean(String cell) {
        return cell.replace("`", "").replace("*", "").trim();
    }

    static String kindOf(List<String> header) {
        List<String> h = header.stream().map(c -> clean(c).toLowerCase(Locale.ROOT)).toList();
        if (h.size() >= 4 && h.subList(0, 4).equals(List.of("type", "category", "normal side", "non-negative"))) {
            return "types";
        }
        if (h.equals(List.of("account", "type", "currency"))) {
            return "accounts";
        }
        if (h.equals(List.of("account", "currency", "debit", "credit"))) {
            return "posting";
        }
        if (h.equals(List.of("hold", "account", "action", "amount"))
                || h.equals(List.of("hold", "account", "action", "amount", "result"))) {
            return "holds";
        }
        if (h.equals(List.of("account", "ledger", "held", "available"))) {
            return "balances";
        }
        return "other";
    }

    // ----- Chart of accounts -----

    void readTypes(Table t) {
        for (Row r : t.rows()) {
            String name = r.cell(0);
            String category = r.cell(1).toUpperCase(Locale.ROOT);
            Side expected = switch (category) {
                case "ASSET", "EXPENSE" -> Side.DEBIT;
                case "LIABILITY", "REVENUE", "EQUITY" -> Side.CREDIT;
                default -> null;
            };
            if (expected == null) {
                problem(r.line(), name + ": unknown category '" + category + "'");
                continue;
            }
            Side normal;
            try {
                normal = Side.valueOf(r.cell(2).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                problem(r.line(), name + ": normal side must be DEBIT or CREDIT");
                continue;
            }
            if (normal != expected) {
                problem(r.line(), name + ": a " + category + " account is " + expected + "-normal, not " + normal + "-normal");
                continue;
            }
            Boolean nonNegative = switch (r.cell(3).toLowerCase(Locale.ROOT)) {
                case "yes" -> true;
                case "no" -> false;
                default -> null;
            };
            if (nonNegative == null) {
                problem(r.line(), name + ": non-negative must be yes or no");
                continue;
            }
            if (types.put(name, new AccountType(name, category, normal, nonNegative)) != null) {
                problem(r.line(), "account type " + name + " is defined twice");
            }
        }
    }

    void readAccounts(Table t) {
        for (Row r : t.rows()) {
            String code = r.cell(0);
            AccountType type = types.get(r.cell(1));
            String currency = r.cell(2);
            if (type == null) {
                problem(r.line(), code + ": unknown account type '" + r.cell(1) + "' (define it in the account types table first)");
            } else if (!CURRENCY.matcher(currency).matches()) {
                problem(r.line(), code + ": currency must be an ISO 4217 code such as INR");
            } else if (accounts.put(code, new Account(code, type, currency)) != null) {
                problem(r.line(), "account " + code + " is defined twice");
            }
        }
    }

    // ----- Steps -----

    void posting(String id, String caption, Table t) {
        if (!newStep(id, t.line())) {
            return;
        }
        postings++;
        boolean valid = true;
        List<Entry> entries = new ArrayList<>();
        for (Row r : t.rows()) {
            String code = r.cell(0);
            String currency = r.cell(1);
            String debit = r.cell(2);
            String credit = r.cell(3);
            Account account = accounts.get(code);
            if (account == null) {
                valid = fail(r.line(), id + ": unknown account '" + code + "'");
                continue;
            }
            if (!account.currency().equals(currency)) {
                valid = fail(r.line(), id + ": " + code + " holds " + account.currency() + ", but the entry is in " + currency);
                continue;
            }
            if (debit.isEmpty() == credit.isEmpty()) {
                valid = fail(r.line(), id + ": " + code + " needs exactly one of debit or credit");
                continue;
            }
            Long amount = amount(debit.isEmpty() ? credit : debit);
            if (amount == null || amount <= 0) {
                valid = fail(r.line(), id + ": " + code + " amount must be a positive whole number of minor units");
                continue;
            }
            entries.add(new Entry(code, debit.isEmpty() ? Side.CREDIT : Side.DEBIT, amount, currency));
        }
        if (t.rows().size() < 2) {
            valid = fail(t.line(), id + ": a transaction needs at least two entries");
        }
        if (valid) {
            Map<String, long[]> totals = new TreeMap<>(); // currency -> {debits, credits}
            for (Entry e : entries) {
                long[] dc = totals.computeIfAbsent(e.currency(), c -> new long[2]);
                int column = e.side() == Side.DEBIT ? 0 : 1;
                dc[column] = Math.addExact(dc[column], e.amount());
            }
            for (Map.Entry<String, long[]> total : totals.entrySet()) {
                long[] dc = total.getValue();
                if (dc[0] != dc[1]) {
                    valid = fail(t.line(), id + ": " + total.getKey() + " debits " + dc[0] + " != credits " + dc[1]);
                }
            }
        }

        Hold captured = null;
        Matcher m = CAPTURES.matcher(caption);
        if (m.find()) {
            String holdId = m.group(1);
            captured = state.activeHolds.get(holdId);
            if (captured == null) {
                valid = fail(t.line(), id + ": captures " + holdId + ", which is not active" + howResolved(state, holdId));
            } else {
                long debited = 0;
                for (Entry e : entries) {
                    if (e.account().equals(captured.account()) && e.side() == Side.DEBIT) {
                        debited = Math.addExact(debited, e.amount());
                    }
                }
                if (debited == 0) {
                    valid = fail(t.line(), id + ": capturing " + holdId + " must debit " + captured.account());
                } else if (debited > captured.amount()) {
                    valid = fail(t.line(), id + ": captures " + debited + ", but " + holdId + " holds only " + captured.amount());
                }
            }
        }

        if (valid) {
            State next = state.copy();
            if (captured != null) {
                next.activeHolds.remove(captured.id());
                next.resolvedHolds.put(captured.id(), "captured by " + id);
            }
            for (Entry e : entries) {
                next.raw.merge(e.account(), e.signed(), Math::addExact);
            }
            if (invariantsHold(next, id, t.line())) {
                state = next; // commit; otherwise `next` is dropped, like a rollback
            }
        }
        snapshots.put(id, state.copy());
    }

    void holdEvents(String id, Table t) {
        if (!newStep(id, t.line())) {
            return;
        }
        State next = state.copy();
        boolean valid = true;
        for (Row r : t.rows()) {
            holdEvents++;
            String holdId = r.cell(0);
            String code = r.cell(1);
            String action = r.cell(2).toUpperCase(Locale.ROOT);
            Long amount = amount(r.cell(3));
            boolean expectRejected = r.cell(4).toLowerCase(Locale.ROOT).startsWith("rejected");
            Account account = accounts.get(code);
            if (account == null) {
                valid = fail(r.line(), id + ": unknown account '" + code + "'");
                continue;
            }
            if (amount == null || amount <= 0) {
                valid = fail(r.line(), id + ": hold amount must be a positive whole number of minor units");
                continue;
            }
            valid &= switch (action) {
                case "AUTHORIZE" -> authorize(next, id, r.line(), holdId, account, amount, expectRejected);
                case "VOID", "EXPIRE" -> release(next, id, r.line(), holdId, account, amount, action);
                case "CAPTURE" -> fail(r.line(), id + ": write a capture as a posting whose caption says 'captures " + holdId + "'");
                default -> fail(r.line(), id + ": unknown hold action '" + action + "' (use AUTHORIZE, VOID or EXPIRE)");
            };
        }
        if (valid && invariantsHold(next, id, t.line())) {
            state = next;
        }
        snapshots.put(id, state.copy());
    }

    boolean authorize(State s, String step, int line, String holdId, Account account, long amount, boolean expectRejected) {
        if (s.activeHolds.containsKey(holdId) || s.resolvedHolds.containsKey(holdId)) {
            return fail(line, step + ": hold id " + holdId + " is already used");
        }
        long available = available(s, account);
        boolean fits = available >= amount;
        if (expectRejected) {
            // A rejected authorization changes nothing. We only check that it really would be rejected.
            return fits ? fail(line, step + ": " + holdId + " should be rejected, but " + account.code() + " has " + available + " available") : true;
        }
        if (!fits) {
            return fail(line, step + ": cannot authorize " + holdId + " for " + amount + ": " + account.code() + " has only " + available + " available");
        }
        s.activeHolds.put(holdId, new Hold(holdId, account.code(), amount));
        return true;
    }

    boolean release(State s, String step, int line, String holdId, Account account, long amount, String action) {
        Hold hold = s.activeHolds.get(holdId);
        if (hold == null) {
            return fail(line, step + ": cannot " + action + " " + holdId + ", which is not active" + howResolved(s, holdId));
        }
        if (!hold.account().equals(account.code()) || hold.amount() != amount) {
            return fail(line, step + ": " + holdId + " is " + hold.amount() + " on " + hold.account() + ", not " + amount + " on " + account.code());
        }
        s.activeHolds.remove(holdId);
        s.resolvedHolds.put(holdId, (action.equals("VOID") ? "voided" : "expired") + " by " + step);
        return true;
    }

    boolean invariantsHold(State s, String step, int line) {
        boolean ok = true;
        Map<String, Long> sumPerCurrency = new TreeMap<>();
        for (Account a : accounts.values()) {
            sumPerCurrency.merge(a.currency(), s.raw(a.code()), Math::addExact);
            long available = available(s, a);
            if (a.type().nonNegative() && available < 0) {
                ok = fail(line, step + ": " + a.code() + " is non-negative, but its available balance would be " + available);
            }
        }
        for (Map.Entry<String, Long> sum : sumPerCurrency.entrySet()) {
            if (sum.getValue() != 0) {
                ok = fail(line, step + ": " + sum.getKey() + " raw balances would sum to " + sum.getValue() + " instead of 0");
            }
        }
        return ok;
    }

    void checkBalances(Table t) {
        for (Row r : t.rows()) {
            balanceRows++;
            String code = r.cell(0);
            Account a = accounts.get(code);
            if (a == null) {
                problem(r.line(), "balance table: unknown account '" + code + "'");
                continue;
            }
            long ledger = a.normal(state.raw(code));
            long held = state.held(code);
            compare(r, code, "ledger", r.cell(1), ledger);
            compare(r, code, "held", r.cell(2), held);
            compare(r, code, "available", r.cell(3), Math.subtractExact(ledger, held));
        }
    }

    void compare(Row r, String code, String what, String cell, long actual) {
        if (cell.isEmpty() || cell.equals(EM_DASH) || cell.equals("-")) {
            return; // not stated in the document
        }
        Long stated = amount(cell);
        if (stated == null) {
            problem(r.line(), code + ": cannot read " + what + " '" + cell + "'");
        } else if (stated != actual) {
            problem(r.line(), code + ": " + what + " after " + lastStep + " is " + actual + " by replay, but the document says " + stated);
        }
    }

    // ----- Variants -----

    void startBranch(String name, String from, int line) {
        if (branch != null) {
            problem(line, "variant '" + name + "' starts inside variant '" + branch + "'; variants can't nest");
            return;
        }
        variants++;
        savedMainLine = state;
        savedLastStep = lastStep;
        State start = snapshots.get(from);
        if (start == null) {
            problem(line, "variant '" + name + "' starts from unknown step " + from);
            start = state;
        }
        state = start.copy();
        branch = name;
        lastStep = from;
    }

    void endBranch(int line) {
        if (branch == null) {
            problem(line, "<!-- end branch --> without a matching <!-- branch ... -->");
            return;
        }
        state = savedMainLine;
        lastStep = savedLastStep;
        branch = null;
    }

    // ----- Helpers -----

    boolean newStep(String id, int line) {
        if (!stepIds.add(id)) {
            problem(line, "step id " + id + " is used twice");
            return false;
        }
        lastStep = id;
        return true;
    }

    long available(State s, Account a) {
        return Math.subtractExact(a.normal(s.raw(a.code())), s.held(a.code()));
    }

    static String howResolved(State s, String holdId) {
        String how = s.resolvedHolds.get(holdId);
        return how == null ? " (never authorized)" : " (already " + how + ")";
    }

    /** Parses a whole number of minor units. Accepts thousands separators and a Unicode minus sign. */
    static Long amount(String text) {
        String s = text.replace('−', '-').replace(",", "").replace("_", "").replace(" ", "");
        if (!s.matches("-?\\d{1,18}")) {
            return null;
        }
        return Long.parseLong(s);
    }

    void problem(int line, String message) {
        problems.add("line " + line + ": " + ascii(message));
    }

    boolean fail(int line, String message) {
        problem(line, message);
        return false;
    }

    /** Keeps console output readable on terminals that are not UTF-8 (e.g. older Windows consoles). */
    static String ascii(String s) {
        return s.replace(EM_DASH, "-").replaceAll("[^\\x20-\\x7E]", "?");
    }

    // ----- Report -----

    boolean report(String file) {
        System.out.println("DomainCheck: " + file);
        System.out.println("Replayed " + count(postings, "posting") + ", " + count(holdEvents, "hold event") + " and "
                + count(balanceRows, "balance row") + ", plus " + count(variants, "variant") + ".");
        printTrialBalance();
        if (state.activeHolds.isEmpty()) {
            System.out.println("Open holds: none");
        } else {
            for (Hold h : state.activeHolds.values()) {
                System.out.println("Open hold: " + h.id() + " for " + h.amount() + " on " + h.account());
            }
        }
        System.out.println();
        for (String p : problems) {
            System.out.println("  x " + p);
        }
        if (expectedProblems != null) {
            boolean passed = problems.size() == expectedProblems;
            System.out.println(passed
                    ? "RESULT: OK - found exactly the " + expectedProblems + " problems this file plants."
                    : "RESULT: FAILED - this file plants " + expectedProblems + " problems, but " + problems.size() + " were found.");
            return passed;
        }
        System.out.println(problems.isEmpty()
                ? "RESULT: OK - every transaction balances and every stated balance matches its entries."
                : "RESULT: FAILED - " + problems.size() + " problem(s).");
        return problems.isEmpty();
    }

    static String count(int n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }

    void printTrialBalance() {
        Map<String, List<Account>> byCurrency = new TreeMap<>();
        for (Account a : accounts.values()) {
            byCurrency.computeIfAbsent(a.currency(), c -> new ArrayList<>()).add(a);
        }
        for (Map.Entry<String, List<Account>> group : byCurrency.entrySet()) {
            System.out.println();
            System.out.println("Trial balance in " + group.getKey() + " (minor units) after " + lastStep + ":");
            System.out.printf("  %-16s %-16s %12s %12s%n", "account", "type", "debit", "credit");
            long debits = 0;
            long credits = 0;
            for (Account a : group.getValue()) {
                long raw = state.raw(a.code());
                boolean debitColumn = raw > 0 || (raw == 0 && a.type().normalSide() == Side.DEBIT);
                if (raw > 0) {
                    debits = Math.addExact(debits, raw);
                } else {
                    credits = Math.addExact(credits, -raw);
                }
                String shown = Long.toString(Math.abs(raw));
                System.out.printf("  %-16s %-16s %12s %12s%n", a.code(), a.type().name(),
                        debitColumn ? shown : "", debitColumn ? "" : shown);
            }
            System.out.printf("  %-16s %-16s %12d %12d%n", "total", "", debits, credits);
        }
        System.out.println();
    }
}
