#!/usr/bin/env bash
# Seed BANK + MERCHANT sample traffic for the MVP demo.
# Usage:
#   ./scripts/seed-demo.sh
#   API_BASE=http://localhost:8081 ./scripts/seed-demo.sh

set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8081}"
TS="$(date -u +%Y-%m-%dT%H:%M:%S)"

post_txn() {
  local payload="$1"
  local label="$2"
  echo "→ ${label}"
  curl -sS -X POST "${API_BASE}/api/v1/transactions" \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -d "${payload}"
  echo
}

echo "Seeding demo data against ${API_BASE}"

post_txn "{
  \"sourceType\": \"BANK\",
  \"sourceId\": \"HSBC-UK\",
  \"sourceName\": \"HSBC United Kingdom\",
  \"accountId\": \"ACC-1001\",
  \"payeeId\": \"PAYEE10001\",
  \"payeeName\": \"Amazon\",
  \"amount\": 2500.00,
  \"currency\": \"INR\",
  \"type\": \"TRANSFER\",
  \"timestamp\": \"${TS}\",
  \"location\": \"London, UK\",
  \"description\": \"Normal vendor payment\",
  \"status\": \"COMPLETED\"
}" "BANK normal amount (no alert when V6 history seed present)"

post_txn "{
  \"sourceType\": \"MERCHANT\",
  \"sourceId\": \"ACME-POS\",
  \"sourceName\": \"ACME Point of Sale\",
  \"accountId\": \"ACC-2002\",
  \"payeeId\": \"PAYEE10002\",
  \"payeeName\": \"Netflix\",
  \"amount\": 149.99,
  \"currency\": \"INR\",
  \"type\": \"TRANSFER\",
  \"timestamp\": \"${TS}\",
  \"location\": \"Mumbai, IN\",
  \"description\": \"POS purchase\",
  \"status\": \"COMPLETED\"
}" "MERCHANT normal amount (no alert when V6 history seed present)"

post_txn "{
  \"sourceType\": \"BANK\",
  \"sourceId\": \"HSBC-UK\",
  \"sourceName\": \"HSBC United Kingdom\",
  \"accountId\": \"ACC-1001\",
  \"payeeId\": \"PAYEE10001\",
  \"payeeName\": \"Amazon\",
  \"amount\": 25000.00,
  \"currency\": \"INR\",
  \"type\": \"TRANSFER\",
  \"timestamp\": \"${TS}\",
  \"location\": \"London, UK\",
  \"description\": \"Over-threshold spike for demo\",
  \"status\": \"COMPLETED\"
}" "BANK over-threshold (creates OPEN AMOUNT_THRESHOLD alert)"

echo
echo "Done. Check:"
echo "  ${API_BASE}/api/v1/transactions"
echo "  ${API_BASE}/api/v1/alerts"
echo "  ${API_BASE}/api/v1/dashboard"
