CREATE TABLE rule_configs (
  rule_type                  VARCHAR(64)    NOT NULL PRIMARY KEY,
  name                       VARCHAR(128)   NOT NULL,
  description                VARCHAR(512)   NOT NULL,
  enabled                    BOOLEAN        NOT NULL DEFAULT TRUE,
  amount_threshold           DECIMAL(15,2)  NULL,
  velocity_max_transactions  INT            NULL,
  velocity_window_minutes    INT            NULL,
  daily_limit                DECIMAL(15,2)  NULL,
  updated_at                 DATETIME       NOT NULL
);

INSERT INTO rule_configs
  (rule_type, name, description, enabled, amount_threshold, velocity_max_transactions, velocity_window_minutes, daily_limit, updated_at)
VALUES
  ('AMOUNT_THRESHOLD',
   'Amount Threshold Rule',
   'Fires when a single transaction amount exceeds the configured threshold. Used to detect unusually large individual transfers.',
   TRUE, 10000.00, NULL, NULL, NULL, NOW()),

  ('VELOCITY',
   'Velocity Rule',
   'Fires when an account exceeds the maximum number of transactions within a rolling time window. Detects rapid bursts of activity.',
   TRUE, NULL, 5, 10, NULL, NOW()),

  ('NEW_PAYEE',
   'New Payee Rule',
   'Fires when an account sends money to a payee it has never transacted with before. Flags first-contact transfers for review.',
   TRUE, NULL, NULL, NULL, NULL, NOW()),

  ('DAILY_LIMIT',
   'Daily Limit Rule',
   'Fires when the cumulative DEBIT amount for an account on a calendar day exceeds the configured limit. Catches high-volume daily outflow.',
   TRUE, NULL, NULL, NULL, 50000.00, NOW());

