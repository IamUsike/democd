# Load test runbook — Windows k6 → Linux VM API

Run load tests from the **Windows** machine (where you SSH from). Point k6 at the Linux VM IP. Do **not** run k6 on the app box — client→VM network path is more realistic.

| Role | Host | What runs there |
|------|------|-----------------|
| Client | Windows VM | k6 |
| App + DB | Linux `10.9.69.3` | Docker: Spring Boot `:8081`, MySQL, UI `:8082` |

**Correct API base (this repo):** `http://10.9.69.3:8081/api/v1/transactions`  
(Not `/transactions` — the controller is `@RequestMapping("/api/v1/transactions")`. Successful create returns **201**.)

**Actuator / rule timer:** `rule.evaluate` is recorded via Micrometer around
`RuleEngine.evaluate` in `TransactionService` (not `@Timed` on the final
`RuleEngine` class — that would not AOP-proxy reliably). After you **redeploy**
this branch:

- Health: `http://10.9.69.3:8081/actuator/health`
- Timer: `http://10.9.69.3:8081/actuator/metrics/rule.evaluate`
- Prometheus scrape: `http://10.9.69.3:8081/actuator/prometheus`

Still watch OS/MySQL during runs — Actuator alone does not show JVM vs mysqld contention.

Scripts in repo:

- [`scripts/k6/post-only.js`](../scripts/k6/post-only.js) — Pass 1
- [`scripts/k6/mixed.js`](../scripts/k6/mixed.js) — Pass 2
- [`scripts/k6/soak.js`](../scripts/k6/soak.js) — Pass 3
- Results template: [`docs/load-test-results.md`](./load-test-results.md)

Replace `10.9.69.3` everywhere if the IP changes.

---

## 0. One-time: install k6 on Windows

PowerShell (Admin if winget needs it):

```powershell
winget install k6.k6
# or: choco install k6
```

Close/reopen the terminal, then:

```powershell
k6 version
```

Clone or copy this repo onto Windows so you have `scripts\k6\*.js` (or copy those three files into a folder).

Smoke-check the API from Windows **before** any load:

```powershell
curl.exe -sS -o NUL -w "%{http_code}`n" http://10.9.69.3:8081/api/v1/transactions?accountId=ACC-1
```

Expect `200`. If it hangs or fails, fix security groups / Windows firewall / routing first — do not start k6.

Optional single POST smoke (must be **201**):

```powershell
curl.exe -sS -X POST http://10.9.69.3:8081/api/v1/transactions `
  -H "Content-Type: application/json" `
  -d "{\"sourceType\":\"BANK\",\"sourceId\":\"HSBC-UK\",\"sourceName\":\"HSBC UK\",\"accountId\":\"ACC-1\",\"payeeId\":\"PAYEE-1\",\"payeeName\":\"Smoke\",\"amount\":500,\"currency\":\"USD\",\"type\":\"DEBIT\",\"timestamp\":\"2026-08-05T12:00:00\",\"status\":\"COMPLETED\"}"
```

---

## 1. Linux SSH session A — seed ~300k rows

SSH into the Linux VM. App can stay up; this SQL bypasses the rule engine (no alert storm).

```bash
# confirm containers
docker ps

# open MySQL (password matches docker-compose in this repo)
docker exec -it mysql-container mysql -uroot -pn3u3da! txnmonitor
```

Inside `mysql`:

```sql
-- baseline
SELECT COUNT(*) AS before_cnt FROM transactions;

-- generate volume (may need 2–3 runs if one INSERT under-shoots)
INSERT INTO transactions (
  source_type, source_id, source_name, account_id, payee_id, payee_name,
  amount, currency, type, `timestamp`, status
)
SELECT
  IF(RAND() < 0.5, 'BANK', 'MERCHANT'),
  CONCAT('SRC-', FLOOR(RAND() * 10)),
  'Seed Source',
  CONCAT('ACC-', FLOOR(RAND() * 5000)),
  CONCAT('PAYEE-', FLOOR(RAND() * 2000)),
  'Seed Payee',
  ROUND(RAND() * 20000, 2),
  'USD',
  'DEBIT',
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
  'COMPLETED'
FROM information_schema.columns a
CROSS JOIN information_schema.columns b
LIMIT 300000;

SELECT COUNT(*) AS after_cnt FROM transactions;
SELECT COUNT(DISTINCT account_id) AS accounts FROM transactions;
```

If `after_cnt` is still under ~200000, run the same `INSERT … LIMIT 300000` again until you land in the **200k–500k** range. Cardinality on `account_id` should be in the thousands (seed uses ~5000 buckets).

Exit mysql: `exit`

---

## 2. Linux — EXPLAIN index check (5 minutes, do this before k6)

Still in MySQL (`docker exec -it mysql-container mysql -uroot -pn3u3da! txnmonitor`):

```sql
EXPLAIN SELECT * FROM transactions
WHERE account_id = 'ACC-123'
  AND `timestamp` > NOW() - INTERVAL 10 MINUTE;

EXPLAIN SELECT COUNT(*) FROM transactions
WHERE account_id = 'ACC-123' AND payee_id = 'PAYEE-456';

EXPLAIN SELECT * FROM transactions
WHERE source_type = 'BANK' AND source_id = 'HSBC-UK'
ORDER BY `timestamp` DESC
LIMIT 50;
```

Copy the `key` and `rows` columns into [`docs/load-test-results.md`](./load-test-results.md).

Expect something like:

| Query | Expected `key` |
|-------|----------------|
| account + timestamp | `idx_txn_account_timestamp` |
| account + payee | `idx_txn_account_payee` |
| source + timestamp | `idx_txn_source_timestamp` |

If `key` is `NULL` and `type` is `ALL`, say so honestly in the results doc (planner may still full-scan on tiny subsets — try an `account_id` that exists: `SELECT account_id FROM transactions LIMIT 1`).

---

## 3. Linux SSH session B — watchers during every k6 pass

Open a **second** SSH session (or `tmux` panes). Install tools once if missing:

```bash
sudo dnf install -y sysstat   # mpstat, iostat on Amazon Linux 2023
```

Run watchers (separate panes / terminals):

```bash
# per-core CPU
mpstat -P ALL 1

# swap: watch si/so go non-zero
vmstat 1

# disk (MySQL on same disk)
iostat -x 1

# JVM vs MySQL CPU share
top -b -d 1 -n 100 | grep -E "java|mysqld"
```

Mid-run MySQL (third pane is ideal):

```bash
docker exec -it mysql-container mysql -uroot -pn3u3da! txnmonitor
```

```sql
SHOW FULL PROCESSLIST;
SHOW ENGINE INNODB STATUS\G
```

If `PROCESSLIST` piles up in `Sending data` / `Locked` while k6 p95 climbs → bottleneck is DB / co-location, not “sync rule eval” by itself. Write that in the results doc.

---

## 4. Windows — Pass 1 (write-only ramp)

In PowerShell, from the repo root (or the folder that contains `scripts\k6`):

```powershell
$env:BASE_URL="http://10.9.69.3:8081"
k6 run .\scripts\k6\post-only.js
```

What it does:

- Ramps 10 → 25 → 50 → 100 → 200 VUs
- POST `/api/v1/transactions` only
- ~10% over Amount Threshold (`15000`) so alert + `alert_transactions` writes are exercised

Record from the k6 summary: RPS, `http_req_duration` p95/p99, `http_req_failed`. Note the VU level where p95 **starts climbing hard** — call that the “cliff”.

---

## 5. Windows — Pass 2 (mixed 80/20)

Keep Linux watchers running.

```powershell
$env:BASE_URL="http://10.9.69.3:8081"
k6 run .\scripts\k6\mixed.js
```

~80% POST, ~20% `GET /api/v1/transactions?accountId=ACC-…`  
**Never** hit unfiltered `GET /api/v1/transactions` after a 300k seed — that can OOM the JVM.

---

## 6. Windows — Pass 3 (10 min soak at ~70% of cliff)

If Pass 1 p95 broke around **100** VUs, soak at **70**:

```powershell
$env:BASE_URL="http://10.9.69.3:8081"
$env:VUS="70"
k6 run .\scripts\k6\soak.js
```

Watch especially: `vmstat` si/so (swap), JVM vs mysqld CPU, memory growth, MySQL `PROCESSLIST`.

---

## 7. Fill in results

Copy numbers into [`docs/load-test-results.md`](./load-test-results.md):

1. Index EXPLAIN table  
2. Co-location caveat + JVM/mysqld %  
3. Pass 1 / 2 / 3 tables  
4. Conclusion: max sustainable RPS, dominant bottleneck, Phase 3 implication  

---

## Quick reference — wrong vs right

| Item | Wrong (old sketch) | Right (this app) |
|------|--------------------|------------------|
| POST URL | `http://IP:8081/transactions` | `http://IP:8081/api/v1/transactions` |
| Success status | 200 | **201** |
| Timestamp | `toISOString()` with `Z` | strip `Z` / millis (Jackson `LocalDateTime`) |
| GET under load | bare `/transactions` | always `?accountId=…` |
| Metrics | guess at rule cost | `/actuator/metrics/rule.evaluate` after redeploy + OS/MySQL |
| Seed | empty table | **200k–500k** rows first |

---

## After redeploy — read rule.evaluate mid/post run

From Windows (PowerShell), during or after a pass:

```powershell
curl.exe -sS http://10.9.69.3:8081/actuator/metrics/rule.evaluate
```

Useful fields in the JSON: look under `measurements` for `COUNT`, `TOTAL_TIME`, `MAX`.
Compare mean rule time (`TOTAL_TIME / COUNT`) to k6 `http_req_duration` p95: if
request p95 is high but rule mean stays low, the time is elsewhere (DB write,
pool wait, GC, co-location).
