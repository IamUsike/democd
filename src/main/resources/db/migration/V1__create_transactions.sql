CREATE TABLE transactions (
  transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_id     VARCHAR(64)  NOT NULL,
  payee_id       VARCHAR(64)  NOT NULL,
  amount         DECIMAL(15,2) NOT NULL,
  currency       VARCHAR(3)   NOT NULL,
  type           VARCHAR(30)  NOT NULL,
  `timestamp`    DATETIME     NOT NULL,
  description    VARCHAR(255) NULL,
  status         VARCHAR(20)  NOT NULL,
  CONSTRAINT chk_txn_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_txn_account_timestamp ON transactions (account_id, `timestamp`);
CREATE INDEX idx_txn_account_payee ON transactions (account_id, payee_id);
