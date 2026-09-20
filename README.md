#ledger-sync — Simplify Money 

This submission implements the ledger ingestion, normalization, deduplication, categorization, reporting, incident fix, and document-store design requested in the assignment.

#Verification

`./verify.sh` is the dependency-free smoke test. Current result:

- raw messages read: **522**
- logical transactions produced: **256**
- messages not parsed: **43**
- 4821: **145** logical transactions vs checkpoint **146**
- 9075: **91** logical transactions vs checkpoint **91**
- card x3310: **20** logical transactions

The checkpoint file says 257 total transactions, while its two bank-account counts plus the 20 unique card transactions account for 256. I have not invented a 257th transaction.

The 4821 balance also has a documented **₹7,500** reconciliation discrepancy. At 29 Jul 2026 17:06 the bank-stated balance falls by ₹7,575 even though the transaction message itself says ₹75.00. The ledger keeps the evidenced ₹75 transaction and records the unexplained ₹7,500 separately.

#Parsing and normalization

- HDFC single-line and multiline SMS formats are supported.
- HDFC card x3310 transactions are supported.
- ICICI original and `Dr/Cr ... ref no ...` formats are supported.
- HDFC and ICICI transaction-alert emails are parsed; email timestamps are normalized to IST so SMS/email copies deduplicate correctly.
- Whole-rupee amounts such as `Rs.5` are accepted and normalized to two decimals.
- Scheduled HDFC e-mandate notices are not treated as completed transactions.
- Non-transaction alerts/OTP/promotional/delivery messages remain skipped.

#Deduplication and categories

A logical transaction is grouped by account, bank transaction time, direction and amount. Merchant text is retained as evidence but is not used as identity, allowing SMS and email copies to merge.

- `MICRO`: debit with UPI in the bank-provided merchant/remarks and amount <= ₹100.
- `TRANSFER`: matched opposite-account IMPS/P2A/PARAG KAPOOR legs with equal amount and close transaction time.
- `SPEND`: other debits.
- `INCOME`: other credits.

Transfer totals are excluded from spend/income. Micro transactions are excluded from spend and reported through `micro_count` and `micro_total`.

## Document-store design

The target design is DynamoDB. The intended document layout is:

- transaction item: `PK=A#<account>#<yyyy-mm>`, `SK=T#<transaction-fingerprint>`
- account totals item: `PK=A#<account>`, `SK=TOTALS`
- message lookup item: `PK=MSG#<message-id>`, `SK=INDEX`

This maps directly to the three required access patterns: month-by-account, category totals, and message lookup. `InMemoryDocumentStore` is included for deterministic tests of those access patterns. A Docker/DynamoDB Local integration was not executed in this environment because Docker/Gradle were unavailable, so no runtime benchmark numbers are claimed as measured.

At 100,000 transactions, the intended indexed access characteristics are:

| Query              | Key access          | Expected engine examination                  |
|                    |                     |                                              |
| Q1 account + month | one partition/query | approximately the matching transaction items |
| Q2 account totals  | one totals item     |                                       1 item |
| Q3 message id      | one lookup item     |                                       1 item |

These are design expectations, not measured DynamoDB Local `ScannedCount`/`Count` results.

#Backfill and consistency

`Backfill` is idempotent using transaction/message evidence, so rerunning after partial progress does not intentionally create another copy. `ConsistencyChecker` compares transaction content rather than merely row counts and reports missing/unexpected transactions.

#Incident

The original amount parser required exactly two decimal places. A message such as `Rs.5 debited ... Avl Bal: Rs.92,213.10` could therefore select the later balance instead of the transaction amount. The parser now accepts 0–2 decimal places and normalizes the value. See `docs/incident-note.md`.

#AI disclosure

AI assistance was used for code exploration, regex ideas, debugging hypotheses, test suggestions and documentation drafting. Suggestions were checked against the corpus and assignment contract before inclusion. See `docs/ai-disclosure.md`.
