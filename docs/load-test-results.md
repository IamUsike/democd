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

--- 
## Outputs 
mysql> EXPLAIN SELECT * FROM transactions
    -> WHERE account_id = 'ACC-123'
    ->   AND `timestamp` > NOW() - INTERVAL 10 MINUTE;
+----+-------------+--------------+------------+-------+-------------------------------------------------+---------------------------+---------+------+------+----------+-----------------------+
| id | select_type | table        | partitions | type  | possible_keys                                   | key                       | key_len | ref  | rows | filtered | Extra                 |
+----+-------------+--------------+------------+-------+-------------------------------------------------+---------------------------+---------+------+------+----------+-----------------------+
|  1 | SIMPLE      | transactions | NULL       | range | idx_txn_account_timestamp,idx_txn_account_payee | idx_txn_account_timestamp | 263     | NULL |    1 |   100.00 | Using index condition |
+----+-------------+--------------+------------+-------+-------------------------------------------------+---------------------------+---------+------+------+----------+-----------------------+
1 row in set, 1 warning (0.01 sec)

mysql> EXPLAIN SELECT COUNT(*) FROM transactions
    -> WHERE account_id = 'ACC-123' AND payee_id = 'PAYEE-456';
+----+-------------+--------------+------------+------+-------------------------------------------------+-----------------------+---------+-------------+------+----------+-------------+
| id | select_type | table        | partitions | type | possible_keys                                   | key                   | key_len | ref         | rows | filtered | Extra       |
+----+-------------+--------------+------------+------+-------------------------------------------------+-----------------------+---------+-------------+------+----------+-------------+
|  1 | SIMPLE      | transactions | NULL       | ref  | idx_txn_account_timestamp,idx_txn_account_payee | idx_txn_account_payee | 516     | const,const |    1 |   100.00 | Using index |
+----+-------------+--------------+------------+------+-------------------------------------------------+-----------------------+---------+-------------+------+----------+-------------+
1 row in set, 1 warning (0.00 sec)

mysql> EXPLAIN SELECT * FROM transactions
    -> WHERE source_type = 'BANK' AND source_id = 'HSBC-UK'
    -> ORDER BY `timestamp` DESC
    -> LIMIT 50;
+----+-------------+--------------+------------+------+--------------------------+--------------------------+---------+-------------+------+----------+---------------------+
| id | select_type | table        | partitions | type | possible_keys            | key                      | key_len | ref         | rows | filtered | Extra               |
+----+-------------+--------------+------------+------+--------------------------+--------------------------+---------+-------------+------+----------+---------------------+
|  1 | SIMPLE      | transactions | NULL       | ref  | idx_txn_source_timestamp | idx_txn_source_timestamp | 340     | const,const |    1 |   100.00 | Backward index scan |
+----+-------------+--------------+------------+------+--------------------------+--------------------------+---------+-------------+------+----------+---------------------+
1 row in set, 1 warning (0.00 sec)

### p1 test results 
   execution: local
        script: .\scripts\k6\post-only.js
        output: -

     scenarios: (100.00%) 1 scenario, 200 max VUs, 5m0s max duration (incl. graceful stop):
              * default: Up to 200 looping VUs for 4m30s over 5 stages (gracefulRampDown: 30s, gracefulStop: 30s)



  █ THRESHOLDS

    http_req_duration
    ✓ 'p(95)<2000' p(95)=763.47ms

    http_req_failed
    ✓ 'rate<0.05' rate=0.00%


  █ TOTAL RESULTS

    checks_total.......: 63376   234.185573/s
    checks_succeeded...: 100.00% 63376 out of 63376
    checks_failed......: 0.00%   0 out of 63376

    ✓ status 201

    HTTP
    http_req_duration..............: avg=215.91ms min=5.7ms   med=111.42ms max=3.39s p(90)=562.26ms p(95)=763.47ms
      { expected_response:true }...: avg=215.91ms min=5.7ms   med=111.42ms max=3.39s p(90)=562.26ms p(95)=763.47ms
    http_req_failed................: 0.00% 0 out of 63376
    http_reqs......................: 63376 234.185573/s

    EXECUTION
    iteration_duration.............: avg=266.58ms min=56.68ms med=162.22ms max=3.44s p(90)=613.09ms p(95)=814.07ms
    iterations.....................: 63376 234.185573/s
    vus............................: 199   min=1          max=199
    vus_max........................: 200   min=200        max=200

    NETWORK
    data_received..................: 41 MB 150 kB/s
    data_sent......................: 27 MB 99 kB/s

##### actuator
Administrator@EC2AMAZ-64EBKCO MINGW64 ~/tr_notes (main)
$ curl.exe -sS http://10.9.69.3:8081/actuator/metrics/rule.evaluate
{"name":"rule.evaluate","description":"Rule engine evaluation time","baseUnit":"seconds","measurements":[{"statistic":"COUNT","value":67283.0},{"statistic":"TOTAL_TIME","value":3850.502325967},{"statistic":"MAX","value":2.497075951}],"availableTags":[]}
Administrator@EC2AMAZ-64EBKCO MINGW64 ~/tr_notes (main)

##### mysql in thw middle
sql> SHOW FULL PROCESSLIST;
+----+-----------------+------------------+------------+---------+------+----------------------------+-----------------------+
| Id | User            | Host             | db         | Command | Time | State                      | Info                  |
+----+-----------------+------------------+------------+---------+------+----------------------------+-----------------------+
|  5 | event_scheduler | localhost        | NULL       | Daemon  | 3342 | Waiting on empty queue     | NULL                  |
| 19 | root            | 172.27.0.3:45120 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 20 | root            | 172.27.0.3:45128 | txnmonitor | Sleep   | 1413 |                            | NULL                  |
| 21 | root            | 172.27.0.3:45162 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 22 | root            | 172.27.0.3:45170 | txnmonitor | Sleep   | 1350 |                            | NULL                  |
| 23 | root            | 172.27.0.3:45176 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 24 | root            | 172.27.0.3:34224 | txnmonitor | Sleep   | 1293 |                            | NULL                  |
| 25 | root            | 172.27.0.3:34238 | txnmonitor | Query   |    0 | waiting for handler commit | COMMIT                |
| 26 | root            | 172.27.0.3:35234 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 27 | root            | 172.27.0.3:35248 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 28 | root            | 172.27.0.3:56024 | txnmonitor | Query   |    0 | waiting for handler commit | COMMIT                |
| 29 | root            | localhost        | txnmonitor | Query   |    0 | init                       | SHOW FULL PROCESSLIST |
+----+-----------------+------------------+------------+---------+------+----------------------------+-----------------------+
12 rows in set, 1 warning (0.00 sec)

mysql> SHOW ENGINE INNODB STATUS\G
*************************** 1. row ***************************
  Type: InnoDB
  Name:
Status:
=====================================
2026-08-05 19:01:23 139917084546624 INNODB MONITOR OUTPUT
=====================================
Per second averages calculated from the last 30 seconds
-----------------
BACKGROUND THREAD
-----------------
srv_master_thread loops: 106 srv_active, 0 srv_shutdown, 3238 srv_idle
srv_master_thread log flush and writes: 0
----------
SEMAPHORES
----------
OS WAIT ARRAY INFO: reservation count 1210
OS WAIT ARRAY INFO: signal count 1040
RW-shared spins 0, rounds 0, OS waits 0
RW-excl spins 0, rounds 0, OS waits 0
RW-sx spins 0, rounds 0, OS waits 0
Spin rounds per wait: 0.00 RW-shared, 0.00 RW-excl, 0.00 RW-sx
------------
TRANSACTIONS
------------
Trx id counter 26831
Purge done for trx's n:o < 20968 undo n:o < 0 state: running but idle
History list length 0
LIST OF TRANSACTIONS FOR EACH SESSION:
---TRANSACTION 421393222486336, not started estimating records in index range
mysql tables in use 1, locked 0
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421393222479872, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421393222480680, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421393222484720, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421393222482296, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421393222478256, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421393222477448, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 26830, ACTIVE (PREPARED) 0 sec
5 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 2
MySQL thread id 25, OS thread handle 139917621417536, query id 171684 172.27.0.3 root waiting for handler commit
COMMIT
Trx read view will not see trx with id >= 26831, sees < 26824
---TRANSACTION 26829, ACTIVE (PREPARED) 0 sec
5 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 2
MySQL thread id 28, OS thread handle 139917489313344, query id 171682 172.27.0.3 root waiting for handler commit
COMMIT
Trx read view will not see trx with id >= 26830, sees < 26824
---TRANSACTION 26828, ACTIVE (PREPARED) 0 sec
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 23, OS thread handle 139917487199808, query id 171671 172.27.0.3 root waiting for handler commit
COMMIT
---TRANSACTION 26826, ACTIVE (PREPARED) 0 sec
5 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 2
MySQL thread id 19, OS thread handle 139917488256576, query id 171677 172.27.0.3 root waiting for handler commit
COMMIT
Trx read view will not see trx with id >= 26829, sees < 26824
---TRANSACTION 26825, ACTIVE (PREPARED) 0 sec
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 21, OS thread handle 139917085603392, query id 171665 172.27.0.3 root waiting for handler commit
COMMIT
---TRANSACTION 26824, ACTIVE (PREPARED) 0 sec
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 26, OS thread handle 139917623531072, query id 171667 172.27.0.3 root waiting for handler commit
COMMIT
--------
FILE I/O
--------
I/O thread 0 state: waiting for completed aio requests (insert buffer thread)
I/O thread 1 state: waiting for completed aio requests (read thread)
I/O thread 2 state: waiting for completed aio requests (read thread)
I/O thread 3 state: waiting for completed aio requests (read thread)
I/O thread 4 state: waiting for completed aio requests (read thread)
I/O thread 5 state: waiting for completed aio requests (write thread)
I/O thread 6 state: waiting for completed aio requests (write thread)
I/O thread 7 state: waiting for completed aio requests (write thread)
I/O thread 8 state: waiting for completed aio requests (write thread)
Pending normal aio reads: [0, 0, 0, 0] , aio writes: [0, 0, 0, 0] ,
 ibuf aio reads:
Pending flushes (fsync) log: 0; buffer pool: 0
1096 OS file reads, 138081 OS file writes, 52970 OS fsyncs
0.10 reads/s, 16384 avg bytes/read, 1795.37 writes/s, 784.81 fsyncs/s
-------------------------------------
INSERT BUFFER AND ADAPTIVE HASH INDEX
-------------------------------------
Ibuf: size 1, free list len 0, seg size 2, 0 merges
merged operations:
 insert 0, delete mark 0, delete 0
discarded operations:
 insert 0, delete mark 0, delete 0
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
0.00 hash searches/s, 10240.56 non-hash searches/s
---
LOG
---
Log capacity                 104857600
Log capacity used            104857600
Log sequence number          140035044
Log buffer assigned up to    140035044
Log buffer completed up to   140035044
Log written up to            140035044
Log flushed up to            140035044
Added dirty pages up to      140035044
Pages flushed up to          136982641
Last checkpoint at           136284551
Log minimum file id is       39
Log maximum file id is       42
95783 log i/o's done, 1242.65 log i/o's/second
----------------------
BUFFER POOL AND MEMORY
----------------------
Total large memory allocated 0
Dictionary memory allocated 593816
Buffer pool size   8192
Free buffers       1016
Database pages     7176
Old database pages 2628
Modified db pages  920
Pending reads      0
Pending writes: LRU 0, flush list 0, single page 0
Pages made young 505, not young 7
15.71 youngs/s, 0.23 non-youngs/s
Pages read 1071, created 6113, written 41452
0.10 reads/s, 10.35 creates/s, 530.87 writes/s
Buffer pool hit rate 1000 / 1000, young-making rate 0 / 1000 not 0 / 1000
Pages read ahead 0.00/s, evicted without access 0.00/s, Random read ahead 0.00/s
LRU len: 7176, unzip_LRU len: 0
I/O sum[634]:cur[0], unzip sum[0]:cur[0]
--------------
ROW OPERATIONS
--------------
0 queries inside InnoDB, 0 queries in queue
3 read views open inside InnoDB
Process ID=1, Main thread ID=139917520782912 , state=sleeping
Number of rows inserted 342611, updated 0, deleted 0, read 12086701
826.17 inserts/s, 0.00 updates/s, 0.00 deletes/s, 1871.07 reads/s
Number of system rows inserted 506, updated 554, deleted 341, read 514317
0.00 inserts/s, 0.00 updates/s, 0.00 deletes/s, 0.00 reads/s
----------------------------
END OF INNODB MONITOR OUTPUT
============================

1 row in set (0.00 sec)


--- 
### pass 2 
PS C:\Users\Administrator\tr_notes\ci-cd\democd> $env:BASE_URL="http://10.9.69.3:8081"
PS C:\Users\Administrator\tr_notes\ci-cd\democd> k6 run .\scripts\k6\mixed.js

         /\      Grafana   /‾‾/
    /\  /  \     |\  __   /  /
   /  \/    \    | |/ /  /   ‾‾\
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/


     execution: local
        script: .\scripts\k6\mixed.js
        output: -

     scenarios: (100.00%) 1 scenario, 150 max VUs, 5m0s max duration (incl. graceful stop):
              * default: Up to 150 looping VUs for 4m30s over 5 stages (gracefulRampDown: 30s, gracefulStop: 30s)



  █ THRESHOLDS

    http_req_duration
    ✓ 'p(95)<3000' p(95)=622.56ms

    http_req_failed
    ✓ 'rate<0.05' rate=0.00%


  █ TOTAL RESULTS

    checks_total.......: 65552   242.385054/s
    checks_succeeded...: 100.00% 65552 out of 65552
    checks_failed......: 0.00%   0 out of 65552

    ✓ POST status 201
    ✓ GET status 200

    HTTP
    http_req_duration..............: avg=183.8ms  min=1.15ms  med=99.11ms  max=3.19s p(90)=469.96ms p(95)=622.56ms
      { expected_response:true }...: avg=183.8ms  min=1.15ms  med=99.11ms  max=3.19s p(90)=469.96ms p(95)=622.56ms
    http_req_failed................: 0.00%  0 out of 65552
    http_reqs......................: 65552  242.385054/s

    EXECUTION
    iteration_duration.............: avg=234.38ms min=51.54ms med=149.67ms max=3.24s p(90)=520.42ms p(95)=672.92ms
    iterations.....................: 65552  242.385054/s
    vus............................: 149    min=1          max=149
    vus_max........................: 150    min=150        max=150

    NETWORK
    data_received..................: 393 MB 1.5 MB/s
    data_sent......................: 24 MB  88 kB/s




running (4m30.4s), 000/150 VUs, 65552 complete and 0 interrupted iterations
default ✓ [=====================================] 000/150 VUs  4m30s

##### Actuator
inistrator@EC2AMAZ-64EBKCO MINGW64 ~/tr_notes (main)
$ curl.exe -sS http://10.9.69.3:8081/actuator/metrics/rule.evaluate
{"name":"rule.evaluate","description":"Rule engine evaluation time","baseUnit":"seconds","measurements":[{"statistic":"COUNT","value":126224.0},{"statistic":"TOTAL_TIME","value":6673.526805849},{"statistic":"MAX","value":1.778028576}],"availableTags":[]}


mysql> SHOW FULL PROCESSLIST;
+----+-----------------+------------------+------------+---------+------+----------------------------+-----------------------+
| Id | User            | Host             | db         | Command | Time | State                      | Info                  |
+----+-----------------+------------------+------------+---------+------+----------------------------+-----------------------+
|  5 | event_scheduler | localhost        | NULL       | Daemon  | 4042 | Waiting on empty queue     | NULL                  |
| 20 | root            | 172.27.0.3:45128 | txnmonitor | Sleep   | 2113 |                            | NULL                  |
| 22 | root            | 172.27.0.3:45170 | txnmonitor | Sleep   | 2050 |                            | NULL                  |
| 24 | root            | 172.27.0.3:34224 | txnmonitor | Sleep   | 1993 |                            | NULL                  |
| 29 | root            | localhost        | txnmonitor | Query   |    0 | init                       | SHOW FULL PROCESSLIST |
| 30 | root            | 172.27.0.3:35898 | txnmonitor | Query   |    0 | waiting for handler commit | COMMIT                |
| 31 | root            | 172.27.0.3:51180 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 32 | root            | 172.27.0.3:45528 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 33 | root            | 172.27.0.3:41286 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 34 | root            | 172.27.0.3:60244 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 35 | root            | 172.27.0.3:60248 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 36 | root            | 172.27.0.3:58522 | txnmonitor | Sleep   |    0 |                            | NULL                  |
+----+-----------------+------------------+------------+---------+------+----------------------------+-----------------------+
12 rows in set, 1 warning (0.00 sec)

mysql> SHOW ENGINE INNODB STATUS\G
*************************** 1. row ***************************
  Type: InnoDB
  Name:
Status:
=====================================
2026-08-05 19:13:04 139917084546624 INNODB MONITOR OUTPUT
=====================================
Per second averages calculated from the last 12 seconds
-----------------
BACKGROUND THREAD
-----------------
srv_master_thread loops: 426 srv_active, 0 srv_shutdown, 3620 srv_idle
srv_master_thread log flush and writes: 0
----------
SEMAPHORES
----------
OS WAIT ARRAY INFO: reservation count 9092
--Thread 139917487199808 has waited at row0ins.cc line 2950 for 0 seconds the semaphore:
X-lock on RW-latch at 0x7f412bfa1568 created in file buf0buf.cc line 857
a writer (thread id 139917863798336) has reserved it in mode SX
number of readers 0, waiters flag 1, lock_word: 10000000
Last time read locked in file dict0stats.cc line 1307
Last time write locked in file ../../../mysql-8.4.11/storage/innobase/buf/buf0flu.cc line 1360
OS WAIT ARRAY INFO: signal count 7560
RW-shared spins 0, rounds 0, OS waits 0
RW-excl spins 0, rounds 0, OS waits 0
RW-sx spins 0, rounds 0, OS waits 0
Spin rounds per wait: 0.00 RW-shared, 0.00 RW-excl, 0.00 RW-sx
------------
TRANSACTIONS
------------
Trx id counter 171020
Purge done for trx's n:o < 165480 undo n:o < 0 state: running but idle
History list length 0
LIST OF TRANSACTIONS FOR EACH SESSION:
---TRANSACTION 421393222479872, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421393222480680, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421393222484720, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421393222482296, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421393222478256, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421393222477448, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 171019, ACTIVE (PREPARED) 0 sec
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 35, OS thread handle 139917623531072, query id 1376742 172.27.0.3 root waiting for handler commit
COMMIT
---TRANSACTION 171018, ACTIVE 0 sec inserting
mysql tables in use 1, locked 1
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 32, OS thread handle 139917488256576, query id 1376740 172.27.0.3 root update
insert into transactions (account_id,amount,currency,description,latitude,location,longitude,payee_id,payee_name,source_id,source_name,source_type,status,timestamp,type) values ('ACC-2351',500,'USD',null,null,null,null,'PAYEE-1513','Load Test Payee','HSBC-UK','Load Test Source','BANK','COMPLETED','2026-08-05 19:13:04','DEBIT')
---TRANSACTION 171017, ACTIVE (PREPARED) 0 sec
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 30, OS thread handle 139917085603392, query id 1376743 172.27.0.3 root waiting for handler commit
COMMIT
---TRANSACTION 171016, ACTIVE 0 sec inserting
mysql tables in use 1, locked 1
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 33, OS thread handle 139917621417536, query id 1376735 172.27.0.3 root update
insert into transactions (account_id,amount,currency,description,latitude,location,longitude,payee_id,payee_name,source_id,source_name,source_type,status,timestamp,type) values ('ACC-4739',500,'USD',null,null,null,null,'PAYEE-125','Load Test Payee','ACME-POS','Load Test Source','BANK','COMPLETED','2026-08-05 19:13:04','DEBIT')
---TRANSACTION 171014, ACTIVE (PREPARED) 0 sec
5 lock struct(s), heap size 1128, 3 row lock(s), undo log entries 4
MySQL thread id 36, OS thread handle 139917489313344, query id 1376731 172.27.0.3 root waiting for handler commit
COMMIT
Trx read view will not see trx with id >= 171015, sees < 171007
---TRANSACTION 171012, ACTIVE 0 sec inserting
mysql tables in use 1, locked 1
5 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 3
MySQL thread id 31, OS thread handle 139917487199808, query id 1376717 172.27.0.3 root update
insert into alerts (account_id,acknowledged_at,closed_at,created_at,dismissed_at,investigating_at,resolution_notes,rule_type,severity,source_id,source_name,source_type,status) values ('ACC-4559',null,null,'2026-08-05 19:13:04.53647',null,null,null,'NEW_PAYEE','MEDIUM','ACME-POS','Load Test Source','BANK','OPEN')
Trx read view will not see trx with id >= 171014, sees < 171007
---TRANSACTION 171011, ACTIVE (PREPARED) 0 sec
5 lock struct(s), heap size 1128, 4 row lock(s), undo log entries 6
MySQL thread id 34, OS thread handle 139917754705472, query id 1376724 172.27.0.3 root waiting for handler commit
COMMIT
Trx read view will not see trx with id >= 171012, sees < 171007
--------
FILE I/O
--------
I/O thread 0 state: waiting for completed aio requests (insert buffer thread)
I/O thread 1 state: waiting for completed aio requests (read thread)
I/O thread 2 state: waiting for completed aio requests (read thread)
I/O thread 3 state: waiting for completed aio requests (read thread)
I/O thread 4 state: waiting for completed aio requests (read thread)
I/O thread 5 state: waiting for completed aio requests (write thread)
I/O thread 6 state: waiting for completed aio requests (write thread)
I/O thread 7 state: waiting for completed aio requests (write thread)
I/O thread 8 state: waiting for completed aio requests (write thread)
Pending normal aio reads: [0, 0, 0, 0] , aio writes: [0, 0, 0, 0] ,
 ibuf aio reads:
Pending flushes (fsync) log: 1; buffer pool: 0
1722 OS file reads, 810801 OS file writes, 318321 OS fsyncs
0 pending preads, 1 pending pwrites
0.66 reads/s, 16384 avg bytes/read, 2247.56 writes/s, 884.09 fsyncs/s
-------------------------------------
INSERT BUFFER AND ADAPTIVE HASH INDEX
-------------------------------------
Ibuf: size 1, free list len 0, seg size 2, 0 merges
merged operations:
 insert 0, delete mark 0, delete 0
discarded operations:
 insert 0, delete mark 0, delete 0
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
0.00 hash searches/s, 21089.58 non-hash searches/s
---
LOG
---
Log capacity                 104857600
Log capacity used            104857600
Log sequence number          261804375
Log buffer assigned up to    261804375
Log buffer completed up to   261804375
Log written up to            261804375
Log flushed up to            261802908
Added dirty pages up to      261804375
Pages flushed up to          259114614
Last checkpoint at           256898667
Log minimum file id is       69
Log maximum file id is       79
543720 log i/o's done, 1482.92 log i/o's/second
----------------------
BUFFER POOL AND MEMORY
----------------------
Total large memory allocated 0
Dictionary memory allocated 593816
Buffer pool size   8192
Free buffers       1023
Database pages     7169
Old database pages 2626
Modified db pages  813
Pending reads      0
Pending writes: LRU 0, flush list 130, single page 0
Pages made young 164956, not young 1893
1065.95 youngs/s, 6.39 non-youngs/s
Pages read 1697, created 10104, written 264055
0.66 reads/s, 13.11 creates/s, 763.95 writes/s
Buffer pool hit rate 1000 / 1000, young-making rate 13 / 1000 not 0 / 1000
Pages read ahead 0.00/s, evicted without access 0.00/s, Random read ahead 0.00/s
LRU len: 7169, unzip_LRU len: 0
I/O sum[37882]:cur[385], unzip sum[0]:cur[0]
--------------
ROW OPERATIONS
--------------
0 queries inside InnoDB, 0 queries in queue
3 read views open inside InnoDB
Process ID=1, Main thread ID=139917520782912 , state=sleeping
Number of rows inserted 732964, updated 0, deleted 0, read 14076146
1315.47 inserts/s, 0.00 updates/s, 0.00 deletes/s, 10674.94 reads/s
Number of system rows inserted 506, updated 554, deleted 341, read 514317
0.00 inserts/s, 0.00 updates/s, 0.00 deletes/s, 0.00 reads/s
----------------------------
END OF INNODB MONITOR OUTPUT
============================

1 row in set (0.01 sec)

---
## Pass 3
 C:\Users\Administrator\tr_notes\ci-cd\democd> k6 run .\scripts\k6\soak.js

         /\      Grafana   /‾‾/
    /\  /  \     |\  __   /  /
   /  \/    \    | |/ /  /   ‾‾\
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/


     execution: local
        script: .\scripts\k6\soak.js
        output: -

     scenarios: (100.00%) 1 scenario, 140 max VUs, 10m30s max duration (incl. graceful stop):
              * default: 140 looping VUs for 10m0s (gracefulStop: 30s)

WARN[0532] Request Failed                                error="Post \"http://10.9.69.3:8081/api/v1/transactions\": request timeout"


  █ THRESHOLDS

    http_req_duration
    ✓ 'p(95)<3000' p(95)=1.13s

    http_req_failed
    ✓ 'rate<0.05' rate=0.09%


  █ TOTAL RESULTS

    checks_total.......: 128470 213.894354/s
    checks_succeeded...: 99.90% 128343 out of 128470
    checks_failed......: 0.09%  127 out of 128470

    ✗ status 201
      ↳  99% — ✓ 128343 / ✗ 127

    HTTP
    http_req_duration..............: avg=603.62ms min=20.65ms med=486.49ms max=1m0s  p(90)=940ms    p(95)=1.13s
      { expected_response:true }...: avg=553.56ms min=20.65ms med=486.23ms max=56.7s p(90)=937.65ms p(95)=1.12s
    http_req_failed................: 0.09%  127 out of 128470
    http_reqs......................: 128470 213.894354/s

    EXECUTION
    iteration_duration.............: avg=654.19ms min=71.01ms med=537.04ms max=1m0s  p(90)=990.68ms p(95)=1.18s
    iterations.....................: 128470 213.894354/s
    vus............................: 140    min=140           max=14
    vus_max........................: 140    min=140           max=14

    NETWORK
    data_received..................: 82 MB  137 kB/s
    data_sent......................: 54 MB  90 kB/s




running (10m00.6s), 000/140 VUs, 128470 complete and 0 interrupted iterations

### Actuator 
- in the middle and at the end 
Administrator@EC2AMAZ-64EBKCO MINGW64 ~/tr_notes (main)
$ curl.exe -sS http://10.9.69.3:8081/actuator/metrics/rule.evaluate
{"name":"rule.evaluate","description":"Rule engine evaluation time","baseUnit":"seconds","measurements":[{"statistic":"COUNT","value":198126.0},{"statistic":"TOTAL_TIME","value":18043.325302339},{"statistic":"MAX","value":1.940228051}],"availableTags":[]}
Administrator@EC2AMAZ-64EBKCO MINGW64 ~/tr_notes (main)
$ curl.exe -sS http://10.9.69.3:8081/actuator/metrics/rule.evaluate
{"name":"rule.evaluate","description":"Rule engine evaluation time","baseUnit":"seconds","measurements":[{"statistic":"COUNT","value":254626.0},{"statistic":"TOTAL_TIME","value":32071.927185666},{"statistic":"MAX","value":60.177009401}],"availableTags":[]}

mysql> SHOW FULL PROCESSLIST;
+----+-----------------+------------------+------------+---------+------+----------------------------+-----------------------+
| Id | User            | Host             | db         | Command | Time | State                      | Info                  |
+----+-----------------+------------------+------------+---------+------+----------------------------+-----------------------+
|  5 | event_scheduler | localhost        | NULL       | Daemon  | 1320 | Waiting on empty queue     | NULL                  |
|  8 | root            | 172.28.0.3:38724 | txnmonitor | Sleep   |    0 |                            | NULL                  |
|  9 | root            | 172.28.0.3:38730 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 10 | root            | 172.28.0.3:38736 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 11 | root            | 172.28.0.3:38748 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 12 | root            | 172.28.0.3:38760 | txnmonitor | Query   |    0 | waiting for handler commit | COMMIT                |
| 13 | root            | 172.28.0.3:38766 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 14 | root            | 172.28.0.3:38768 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 15 | root            | 172.28.0.3:38772 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 16 | root            | 172.28.0.3:38780 | txnmonitor | Query   |    0 | waiting for handler commit | COMMIT                |
| 17 | root            | 172.28.0.3:38786 | txnmonitor | Sleep   |    0 |                            | NULL                  |
| 18 | root            | localhost        | txnmonitor | Query   |    0 | init                       | SHOW FULL PROCESSLIST |
+----+-----------------+------------------+------------+---------+------+----------------------------+-----------------------+
12 rows in set, 1 warning (0.00 sec)

mysql> SHOW ENGINE INNODB STATUS\G
*************************** 1. row ***************************
  Type: InnoDB
  Name:
Status:
=====================================
2026-08-05 19:56:14 140133177202240 INNODB MONITOR OUTPUT
=====================================
Per second averages calculated from the last 17 seconds
-----------------
BACKGROUND THREAD
-----------------
srv_master_thread loops: 820 srv_active, 0 srv_shutdown, 502 srv_idle
srv_master_thread log flush and writes: 0
----------
SEMAPHORES
----------
OS WAIT ARRAY INFO: reservation count 23119
OS WAIT ARRAY INFO: signal count 18186
RW-shared spins 0, rounds 0, OS waits 0
RW-excl spins 0, rounds 0, OS waits 0
RW-sx spins 0, rounds 0, OS waits 0
Spin rounds per wait: 0.00 RW-shared, 0.00 RW-excl, 0.00 RW-sx
------------
TRANSACTIONS
------------
Trx id counter 393587
Purge done for trx's n:o < 387069 undo n:o < 0 state: running but idle
History list length 0
LIST OF TRANSACTIONS FOR EACH SESSION:
---TRANSACTION 421609060387944, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421609060382288, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421609060380672, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421609060379056, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 421609060378248, not started
0 lock struct(s), heap size 1128, 0 row lock(s)
---TRANSACTION 393586, ACTIVE 0 sec
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 8, OS thread handle 140133914347072, query id 3326793 172.28.0.3 root
---TRANSACTION 393585, ACTIVE (PREPARED) 0 sec
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 10, OS thread handle 140133581030976, query id 3326794 172.28.0.3 root waiting for handler commit
COMMIT
---TRANSACTION 393584, ACTIVE 0 sec
5 lock struct(s), heap size 1128, 2 row lock(s), undo log entries 3
MySQL thread id 12, OS thread handle 140133578917440, query id 3326791 172.28.0.3 root
Trx read view will not see trx with id >= 393585, sees < 393578
---TRANSACTION 393583, ACTIVE (PREPARED) 0 sec
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 17, OS thread handle 140133443524160, query id 3326784 172.28.0.3 root waiting for handler commit
COMMIT
---TRANSACTION 393582, ACTIVE (PREPARED) 0 sec
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 16, OS thread handle 140133444580928, query id 3326774 172.28.0.3 root waiting for handler commit
COMMIT
---TRANSACTION 393581, ACTIVE (PREPARED) 0 sec
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 13, OS thread handle 140133577860672, query id 3326773 172.28.0.3 root waiting for handler commit
COMMIT
---TRANSACTION 393580, ACTIVE (PREPARED) 0 sec
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 15, OS thread handle 140133445637696, query id 3326771 172.28.0.3 root waiting for handler commit
COMMIT
---TRANSACTION 393579, ACTIVE (PREPARED) 0 sec
1 lock struct(s), heap size 1128, 0 row lock(s), undo log entries 1
MySQL thread id 14, OS thread handle 140133576803904, query id 3326777 172.28.0.3 root waiting for handler commit
COMMIT
--------
FILE I/O
--------
I/O thread 0 state: waiting for completed aio requests (insert buffer thread)
I/O thread 1 state: waiting for completed aio requests (read thread)
I/O thread 2 state: waiting for completed aio requests (read thread)
I/O thread 3 state: waiting for completed aio requests (read thread)
I/O thread 4 state: waiting for completed aio requests (read thread)
I/O thread 5 state: waiting for completed aio requests (write thread)
I/O thread 6 state: waiting for completed aio requests (write thread)
I/O thread 7 state: waiting for completed aio requests (write thread)
I/O thread 8 state: waiting for completed aio requests (write thread)
Pending normal aio reads: [0, 0, 0, 0] , aio writes: [0, 0, 0, 0] ,
 ibuf aio reads:
Pending flushes (fsync) log: 1; buffer pool: 0
1650 OS file reads, 1569824 OS file writes, 663591 OS fsyncs
3.85 reads/s, 16384 avg bytes/read, 2370.07 writes/s, 845.74 fsyncs/s
-------------------------------------
INSERT BUFFER AND ADAPTIVE HASH INDEX
-------------------------------------
Ibuf: size 1, free list len 0, seg size 2, 0 merges
merged operations:
 insert 0, delete mark 0, delete 0
discarded operations:
 insert 0, delete mark 0, delete 0
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
Hash table size 34679, node heap has 0 buffer(s)
0.00 hash searches/s, 27428.39 non-hash searches/s
---
LOG
---
Log capacity                 104857600
Log capacity used            104857600
Log sequence number          373383410
Log buffer assigned up to    373383410
Log buffer completed up to   373383410
Log written up to            373383410
Log flushed up to            373382620
Added dirty pages up to      373383410
Pages flushed up to          370777856
Last checkpoint at           370777856
Log minimum file id is       97
Log maximum file id is       114
1169555 log i/o's done, 1569.41 log i/o's/second
----------------------
BUFFER POOL AND MEMORY
----------------------
Total large memory allocated 0
Dictionary memory allocated 593816
Buffer pool size   8192
Free buffers       1014
Database pages     7178
Old database pages 2629
Modified db pages  857
Pending reads      0
Pending writes: LRU 0, flush list 0, single page 0
Pages made young 17832, not young 2469
173.11 youngs/s, 10.51 non-youngs/s
Pages read 1625, created 11844, written 395757
3.85 reads/s, 15.90 creates/s, 830.00 writes/s
Buffer pool hit rate 1000 / 1000, young-making rate 1 / 1000 not 0 / 1000
Pages read ahead 0.00/s, evicted without access 0.00/s, Random read ahead 0.00/s
LRU len: 7178, unzip_LRU len: 0
I/O sum[43104]:cur[0], unzip sum[0]:cur[0]
--------------
ROW OPERATIONS
--------------
0 queries inside InnoDB, 0 queries in queue
1 read views open inside InnoDB
Process ID=1, Main thread ID=140133219161664 , state=sleeping
Number of rows inserted 1098398, updated 0, deleted 0, read 7464525
1700.08 inserts/s, 0.00 updates/s, 0.00 deletes/s, 15920.71 reads/s
Number of system rows inserted 506, updated 554, deleted 341, read 6931
0.00 inserts/s, 0.00 updates/s, 0.00 deletes/s, 0.00 reads/s
----------------------------
END OF INNODB MONITOR OUTPUT
============================

1 row in set (0.00 sec)
