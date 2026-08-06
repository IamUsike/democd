// Pass 1 — write-only baseline (ingest + sync rule eval + occasional alert writes)
// Usage (PowerShell):
//   $env:BASE_URL="http://10.9.69.3:8081"
//   k6 run .\scripts\k6\post-only.js

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '60s', target: 25 },
    { duration: '60s', target: 50 },
    { duration: '60s', target: 100 },
    { duration: '60s', target: 200 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://10.9.69.3:8081';

function isoTimestamp() {
  // LocalDateTime on the API — no trailing Z
  return new Date().toISOString().replace(/\.\d{3}Z$/, '').replace('Z', '');
}

export default function () {
  const overThreshold = Math.random() < 0.1; // 10% also exercise alert inserts
  const payload = JSON.stringify({
    sourceType: Math.random() < 0.5 ? 'BANK' : 'MERCHANT',
    sourceId: Math.random() < 0.5 ? 'HSBC-UK' : 'ACME-POS',
    sourceName: 'Load Test Source',
    accountId: `ACC-${Math.floor(Math.random() * 5000)}`,
    payeeId: `PAYEE-${Math.floor(Math.random() * 2000)}`,
    payeeName: 'Load Test Payee',
    amount: overThreshold ? 15000 : 500,
    currency: 'USD',
    type: 'DEBIT',
    timestamp: isoTimestamp(),
    status: 'COMPLETED',
  });

  const res = http.post(`${BASE_URL}/api/v1/transactions`, payload, {
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
  });

  check(res, {
    'status 201': (r) => r.status === 201,
  });

  sleep(0.05);
}
