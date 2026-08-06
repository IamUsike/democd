// Pass 3 — sustained soak at ~70% of the RPS where Pass 1 p95 started climbing.
// Set VUS to that number before running. Default 70 is a starting guess — replace
// after Pass 1 (e.g. if p95 broke at ~100 VUs, run soak at 70).
//
// Usage (PowerShell):
//   $env:BASE_URL="http://10.9.69.3:8081"
//   $env:VUS="70"
//   k6 run .\scripts\k6\soak.js

import http from 'k6/http';
import { check, sleep } from 'k6';

const VUS = Number(__ENV.VUS || 70);

export const options = {
  vus: VUS,
  duration: '10m',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<3000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://10.9.69.3:8081';

function isoTimestamp() {
  return new Date().toISOString().replace(/\.\d{3}Z$/, '').replace('Z', '');
}

export default function () {
  const overThreshold = Math.random() < 0.1;
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

  check(res, { 'status 201': (r) => r.status === 201 });
  sleep(0.05);
}
