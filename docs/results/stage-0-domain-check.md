# Stage 0 acceptance run: DomainCheck

Evidence for the Stage 0 criterion "each example balances on paper". This is
a correctness check, not a performance measurement.

## Environment

- Date (UTC): 2026-11-06T14:08:48Z
- Commit checked: `c42f0d8` (docs(domain): add DOMAIN.md with accounts, glossary, invariants and examples)
- OS: Windows 11 Home 10.0.26300, Git Bash (MINGW64_NT-10.0-26300)
- Java: java version "21.0.3" 2024-04-16 LTS

## Run 1: worked examples in DOMAIN.md

```text
$ java tools/domain-check/DomainCheck.java DOMAIN.md
DomainCheck: DOMAIN.md
Replayed 12 postings, 7 hold events and 41 balance rows, plus 4 variants.

Trial balance in INR (minor units) after E7.4:
  account          type                    debit       credit
  settlement_bank  SETTLEMENT_BANK        115000             
  psp_clearing     PSP_CLEARING                0             
  fee_revenue      FEE_REVENUE                           3400
  suspense         SUSPENSE                                 0
  wallet:alice     CUSTOMER_WALLET                      45000
  wallet:bob       CUSTOMER_WALLET                      55000
  merchant:chai    MERCHANT                             11600
  total                                   115000       115000

Open holds: none

RESULT: OK - every transaction balances and every stated balance matches its entries.
exit code: 0
```

## Run 2: planted mistakes (the checker's own test)

```text
$ java tools/domain-check/DomainCheck.java tools/domain-check/testdata/broken.md
DomainCheck: tools/domain-check/testdata/broken.md
Replayed 8 postings, 3 hold events and 1 balance row, plus 1 variant.

Trial balance in INR (minor units) after E1.11:
  account          type                    debit       credit
  psp_clearing     PSP_CLEARING            10000             
  wallet:alice     CUSTOMER_WALLET                       4000
  wallet:bob       CUSTOMER_WALLET                       6000
  total                                    10000        10000

Open holds: none

  x line 17: SUSPENSE: a LIABILITY account is CREDIT-normal, not DEBIT-normal
  x line 34: E1.2: INR debits 5000 != credits 4999
  x line 44: E1.3: wallet:bob holds INR, but the entry is in USD
  x line 48: E1.4: wallet:alice is non-negative, but its available balance would be -10000
  x line 57: E1.5: cannot authorize H1 for 10001: wallet:alice has only 10000 available
  x line 67: E1.7: wallet:alice is non-negative, but its available balance would be -1000
  x line 82: E1.9: captures H2, which is not active (already captured by E1.8)
  x line 91: E1.10: unknown account 'wallet:carol'
  x line 98: wallet:alice: ledger after E1.10 is 4000 by replay, but the document says 5000
  x line 104: E1.11: H3 should be rejected, but wallet:bob has 6000 available
  x line 108: variant 'X' starts from unknown step E9.9
RESULT: OK - found exactly the 11 problems this file plants.
exit code: 0
```
