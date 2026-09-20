CREATE TABLE IF NOT EXISTS ledger_evidence (
  account_last4 VARCHAR(4) NOT NULL,
  occurred_at VARCHAR(40) NOT NULL,
  direction VARCHAR(10) NOT NULL,
  amount DECIMAL(20,2) NOT NULL,
  message_id VARCHAR(100) NOT NULL,
  stated_balance DECIMAL(20,2),
  PRIMARY KEY(account_last4, occurred_at, direction, amount, message_id)
);
