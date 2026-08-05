# Load test results — Transaction Monitoring

**Date:** YYYY-MM-DD  
**Tester:**  
**Client:** Windows VM (k6) → Linux VM API `http://10.9.69.3:8081`  
**App deploy:** Docker Compose on single box (JVM + MySQL co-located)

## Environment

| Item | Value |
|------|-------|
| Linux host | `ip-10-9-69-3` / `10.9.69.3` |
| OS | Amazon Linux 2023 |
| CPU | Intel Xeon Platinum 8259CL (note vCPU count from `nproc`) |
| Memory | 3.7 GiB (+ ~3.9 GiB swap) |
| Disk | 30G root (~59% used at start) |
| API | `http://10.9.69.3:8081` |
| UI | `http://10.9.69.3:8082` |
| Seed row count before tests | |
| k6 version | |

## Co-location caveat

App and MySQL share one VM (~2 vCPU / 3.7GB). Under load the JVM and
`mysqld` compete for the same cores. When latency climbs we cannot blame
“sync rule evaluation” alone unless process-level CPU split and MySQL
`PROCESSLIST` / InnoDB status support that claim.

CPU split during peak load (from `top` / `mpstat`):

- JVM: ___%
- mysqld: ___%
- Swap (`vmstat` si/so): ___

Interpretation: ___

What we’d change with a second VM (separate DB host): ___

## Index verification (EXPLAIN)

| Query | Index used (`key`) | Rows examined (`rows`) | Notes |
|-------|--------------------|------------------------|-------|
| `account_id` + `timestamp` (velocity-style) | | | |
| `account_id` + `payee_id` (new-payee-style) | | | |
| `source_type` + `source_id` + `timestamp` ORDER BY | | | |

Paste `EXPLAIN` output (or screenshots) under each if useful.

## Pass 1 — write-only ramp

Command:

```powershell
$env:BASE_URL="http://10.9.69.3:8081"
k6 run .\scripts\k6\post-only.js
```

| Stage / VU target | approx RPS | p95 (ms) | p99 (ms) | error rate | Notes |
|-------------------|------------|----------|----------|------------|-------|
| 10 | | | | | |
| 25 | | | | | |
| 50 | | | | | |
| 100 | | | | | |
| 200 | | | | | |

Where p95 started climbing hard: ___ VUs / ___ RPS  
Observed JVM vs MySQL at that point: ___

## Pass 2 — mixed 80/20 read/write

Command:

```powershell
$env:BASE_URL="http://10.9.69.3:8081"
k6 run .\scripts\k6\mixed.js
```

| Metric | Value |
|--------|-------|
| Peak VUs | |
| approx RPS | |
| p95 | |
| error rate | |
| Notes (GET vs POST) | |

## Pass 3 — soak (~70% of Pass 1 cliff)

Command:

```powershell
$env:BASE_URL="http://10.9.69.3:8081"
$env:VUS="<70pct_of_cliff>"
k6 run .\scripts\k6\soak.js
```

| Metric | Value |
|--------|-------|
| VUs | |
| Duration | 10m |
| p95 start → end | |
| error rate | |
| Swap activity | |
| Memory trend | |
| PROCESSLIST / locks | |

## rule.evaluate (Actuator)

Sample during / after peak (from `/actuator/metrics/rule.evaluate`):

| Measurement | Value |
|-------------|-------|
| count | |
| total time | |
| max | |
| mean (total/count) | |

Compare to k6 `http_req_duration` p95: ___

## Conclusions

1. Max sustainable write RPS (before p95 cliff): ___
2. Indexes confirmed via EXPLAIN: yes / no / partial
3. Dominant bottleneck under load: JVM rule path / MySQL / co-location contention / network / other: ___
4. Evidence: ___
5. Phase 3 implications (queue / extract rule workers / separate DB): ___

## Raw artifacts

- k6 summary JSON / screenshots:
- `mpstat` / `vmstat` / `iostat` snippets:
- `SHOW PROCESSLIST` / `SHOW ENGINE INNODB STATUS` snippets:
