CREATE TABLE alerts (
  alert_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  rule_type VARCHAR(64) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  source_type VARCHAR(20) NOT NULL,
  source_id VARCHAR(64) NOT NULL,
  source_name VARCHAR(128) NOT NULL,
  status VARCHAR(30) NOT NULL,
  severity VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL,
  acknowledged_at DATETIME NULL,
  investigating_at DATETIME NULL,
  dismissed_at DATETIME NULL,
  closed_at DATETIME NULL,
  resolution_notes VARCHAR(1000) NULL,
  CONSTRAINT chk_alert_source_type CHECK (source_type IN ('BANK', 'MERCHANT')),
  CONSTRAINT chk_alert_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'CLOSED', 'DISMISSED')),
  CONSTRAINT chk_alert_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE INDEX idx_alert_status ON alerts (status);
CREATE INDEX idx_alert_account ON alerts (account_id);
CREATE INDEX idx_alert_source ON alerts (source_type, source_id);
CREATE INDEX idx_alert_created ON alerts (created_at);

CREATE TABLE alert_transactions (
  alert_id BIGINT NOT NULL,
  transaction_id BIGINT NOT NULL,
  PRIMARY KEY (alert_id, transaction_id),
  CONSTRAINT fk_alert_txn_alert FOREIGN KEY (alert_id) REFERENCES alerts (alert_id),
  CONSTRAINT fk_alert_txn_txn FOREIGN KEY (transaction_id) REFERENCES transactions (transaction_id)
);
