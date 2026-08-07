-- Seed prior account↔payee history for simulator quiet paths.
-- Inserts bypass the app/rule engine so NEW_PAYEE does not fire on:
--   - SOFT_TENANCY_MIX / MVP_SEED fixed pairs
--   - continuous NORMAL ACC-QUIET-* traffic (pool sized for default velocity)

INSERT INTO transactions (
  source_type, source_id, source_name, account_id, payee_id, payee_name,
  amount, currency, type, `timestamp`, status, description
) VALUES
  ('BANK', 'HSBC-UK', 'HSBC United Kingdom', 'ACC-SCENARIO-TEN-BANK', 'PAYEE10001', 'Amazon',
   10.00, 'USD', 'TRANSFER', '2020-01-01 00:00:00', 'COMPLETED', 'Seed: soft tenancy history'),
  ('MERCHANT', 'ACME-POS', 'ACME Payments', 'ACC-SCENARIO-TEN-MERCH', 'PAYEE10002', 'Netflix',
   10.00, 'USD', 'TRANSFER', '2020-01-01 00:00:00', 'COMPLETED', 'Seed: soft tenancy history'),
  ('BANK', 'HSBC-UK', 'HSBC United Kingdom', 'ACC-1001', 'PAYEE10001', 'Amazon',
   10.00, 'INR', 'TRANSFER', '2020-01-01 00:00:00', 'COMPLETED', 'Seed: MVP history'),
  ('MERCHANT', 'ACME-POS', 'ACME Point of Sale', 'ACC-2002', 'PAYEE10002', 'Netflix',
   10.00, 'INR', 'TRANSFER', '2020-01-01 00:00:00', 'COMPLETED', 'Seed: MVP history');

INSERT INTO transactions (
  source_type, source_id, source_name, account_id, payee_id, payee_name,
  amount, currency, type, `timestamp`, status, description
)
SELECT
  'BANK',
  'HSBC-UK',
  'HSBC United Kingdom',
  CONCAT('ACC-QUIET-', LPAD(n, 5, '0')),
  CONCAT('PAYEE', 10001 + (n % 25)),
  'Seed Payee',
  10.00,
  'USD',
  'TRANSFER',
  '2020-01-01 00:00:00',
  'COMPLETED',
  'Seed: quiet traffic history'
FROM (
  SELECT ones.n + tens.n * 10 + hundreds.n * 100 + thousands.n * 1000 AS n
  FROM
    (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
     UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) ones
  CROSS JOIN
    (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
     UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) tens
  CROSS JOIN
    (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
     UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) hundreds
  CROSS JOIN
    (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
     UNION SELECT 5 UNION SELECT 6 UNION SELECT 7) thousands
) nums
WHERE n < 7200;
