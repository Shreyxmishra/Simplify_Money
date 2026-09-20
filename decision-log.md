# Decision log

1. Kept the frozen `NormalizedTxn` contract unchanged.
2. Used only JDK APIs for the core pipeline so `verify.sh` remains dependency-free.
3. Parsed bank-stated transaction time rather than message receipt time.
4. Grouped evidence by account, transaction time, direction and amount so SMS/email copies become one logical transaction; merchant text is retained as evidence but is not part of identity.
5. Classified UPI debits <= ₹100 as `MICRO`; the ₹0.50 mandate-verification debit remains a micro transaction because it satisfies the stated rule.
6. Classified matched opposite-account IMPS/P2A/PARAG KAPOOR legs as `TRANSFER`; unmatched external payments remain spending/income.
7. Treated the ₹7,500 balance discrepancy in account 4821 as reconciliation evidence rather than inventing a transaction to make the checkpoint pass.
8. Chose a DynamoDB-style document key design for the migration: account/month transaction partitioning, message lookup index, and account totals document. A production run would use DynamoDB Local via Docker.
9. Backfill is idempotent by transaction/message evidence; consistency checks compare transaction content rather than row counts.
10. No product usage/referral was fabricated for the real-world Task 0/1 items.
