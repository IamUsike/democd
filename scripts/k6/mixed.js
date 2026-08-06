// Pass 2 — mixed read/write (~80% POST, ~20% filtered GET)
// Usage (PowerShell):
//   $env:BASE_URL="http://10.9.69.3:8081"
//   k6 run .\scripts\k6\mixed.js

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '60s', target: 25 },
    { duration: '60s', target: 50 },
    { duration: '60s', target: 100 },
    { duration: '60s', target: 150 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<3000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://10.9.69.3:8081';

function isoTimestamp() {
  return new Date().toISOString().replace(/\.\d{3}Z$/, '').replace('Z', '');
}

function postTxn() {
  const overThreshold = Math.random() < 0.1;
  const accountId = `ACC-${Math.floor(Math.random() * 5000)}`;
  const payload = JSON.stringify({
    sourceType: Math.random() < 0.5 ? 'BANK' : 'MERCHANT',
    sourceId: Math.random() < 0.5 ? 'HSBC-UK' : 'ACME-POS',
    sourceName: 'Load Test Source',
    accountId,
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

  check(res, { 'POST status 201': (r) => r.status === 201 });
}

function getTxn() {
  // ALWAYS filter — unfiltered GET after a 300k seed can OOM the JVM
  const accountId = `ACC-${Math.floor(Math.random() * 5000)}`;
  const res = http.get(
    `${BASE_URL}/api/v1/transactions?accountId=${accountId}`,
    { headers: { Accept: 'application/json' } }
  );
  check(res, { 'GET status 200': (r) => r.status === 200 });
}

export default function () {
  if (Math.random() < 0.8) {
    postTxn();
  } else {
    getTxn();
  }
  sleep(0.05);
}
