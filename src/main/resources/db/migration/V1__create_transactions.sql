CREATE TABLE transactions (
  transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_type    VARCHAR(20)  NOT NULL,
  source_id      VARCHAR(64)  NOT NULL,
  source_name    VARCHAR(128) NOT NULL,
  account_id     VARCHAR(64)  NOT NULL,
  payee_id       VARCHAR(64)  NOT NULL,
  payee_name     VARCHAR(128) NULL,
  amount         DECIMAL(15,2) NOT NULL,
  currency       VARCHAR(3)   NOT NULL,
  type           VARCHAR(30)  NOT NULL,
  `timestamp`    DATETIME     NOT NULL,
  location       VARCHAR(255) NULL,
  latitude       DECIMAL(10,7) NULL,
  longitude      DECIMAL(10,7) NULL,
  description    VARCHAR(255) NULL,
  status         VARCHAR(20)  NOT NULL,
  CONSTRAINT chk_txn_amount_positive CHECK (amount > 0),
  CONSTRAINT chk_txn_source_type CHECK (source_type IN ('BANK', 'MERCHANT'))
);

CREATE INDEX idx_txn_account_timestamp ON transactions (account_id, `timestamp`);
CREATE INDEX idx_txn_account_payee ON transactions (account_id, payee_id);
CREATE INDEX idx_txn_source_timestamp ON transactions (source_type, source_id, `timestamp`);
