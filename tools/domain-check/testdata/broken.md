# Planted mistakes: DomainCheck must catch every one

This file tests the checker itself. Each marked step contains one deliberate
mistake. The run passes only if the checker reports exactly the number of
problems declared below, which proves it isn't silently accepting bad books.

    java tools/domain-check/DomainCheck.java tools/domain-check/testdata/broken.md

<!-- expect-problems: 11 -->

Mistake 1: a LIABILITY type can't be debit-normal.

| Type | Category | Normal side | Non-negative | Holder | Purpose |
|---|---|---|---|---|---|
| CUSTOMER_WALLET | LIABILITY | CREDIT | yes | customer | stored value |
| PSP_CLEARING | ASSET | DEBIT | no | system | money in flight |
| SUSPENSE | LIABILITY | DEBIT | no | system | wrong on purpose |

| Account | Type | Currency |
|---|---|---|
| psp_clearing | PSP_CLEARING | INR |
| wallet:alice | CUSTOMER_WALLET | INR |
| wallet:bob | CUSTOMER_WALLET | INR |

**E1.1** A correct top-up, so the later steps have money to work with.

| Account | Currency | Debit | Credit |
|---|---|---:|---:|
| psp_clearing | INR | 10000 | |
| wallet:alice | INR | | 10000 |

**E1.2** Mistake 2: debits and credits differ by one paisa.

| Account | Currency | Debit | Credit |
|---|---|---:|---:|
| psp_clearing | INR | 5000 | |
| wallet:bob | INR | | 4999 |

**E1.3** Mistake 3: the entry's currency differs from the account's currency.

| Account | Currency | Debit | Credit |
|---|---|---:|---:|
| psp_clearing | INR | 100 | |
| wallet:bob | USD | | 100 |

**E1.4** Mistake 4: overdraws Alice's non-negative wallet (she has 10000).

| Account | Currency | Debit | Credit |
|---|---|---:|---:|
| wallet:alice | INR | 20000 | |
| wallet:bob | INR | | 20000 |

**E1.5** Mistake 5: holds more than Alice has available.

| Hold | Account | Action | Amount |
|---|---|---|---:|
| H1 | wallet:alice | AUTHORIZE | 10001 |

**E1.6** A correct hold.

| Hold | Account | Action | Amount |
|---|---|---|---:|
| H2 | wallet:alice | AUTHORIZE | 6000 |

**E1.7** Mistake 6: spends held money without capturing the hold (available would be -1000).

| Account | Currency | Debit | Credit |
|---|---|---:|---:|
| wallet:alice | INR | 5000 | |
| wallet:bob | INR | | 5000 |

**E1.8** A correct capture: captures H2.

| Account | Currency | Debit | Credit |
|---|---|---:|---:|
| wallet:alice | INR | 6000 | |
| wallet:bob | INR | | 6000 |

**E1.9** Mistake 7: captures H2 a second time.

| Account | Currency | Debit | Credit |
|---|---|---:|---:|
| wallet:alice | INR | 100 | |
| wallet:bob | INR | | 100 |

**E1.10** Mistake 8: posts to an account that doesn't exist.

| Account | Currency | Debit | Credit |
|---|---|---:|---:|
| wallet:carol | INR | 100 | |
| wallet:bob | INR | | 100 |

Mistake 9: the stated balance is wrong (Alice has 4000 left).

| Account | Ledger | Held | Available |
|---|---:|---:|---:|
| wallet:alice | 5000 | — | — |

**E1.11** Mistake 10: claims a hold is rejected, but Bob has 6000 available.

| Hold | Account | Action | Amount | Result |
|---|---|---|---:|---|
| H3 | wallet:bob | AUTHORIZE | 100 | rejected |

Mistake 11: a variant that starts from a step that doesn't exist.

<!-- branch X from E9.9 -->
<!-- end branch -->
