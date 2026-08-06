# MySQL VM Migration Guide

Last updated: 06 August 2026

Move MySQL off the application VM so the Spring Boot JVM and InnoDB no longer
compete for CPU, RAM, and disk I/O. The application code and compose files for
this are already in the repo — this guide is the **ops runbook**.

---

## Target topology

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│  App VM                     │       │  DB VM                      │
│  ─────────                  │       │  ──────                     │
│  • springboot-app  :8081    │ JDBC  │  • MySQL 8         :3306    │
│  • rabbitmq        :5672    │──────▶│  • txnmonitor database      │
│  • frontend nginx  :8082    │       │  • Flyway schema (auto)     │
└─────────────────────────────┘       └─────────────────────────────┘
         ▲
         │ HTTP
    Operators / k6 / seed script
```

Security group / firewall: **only the app VM private IP** may reach DB VM port
`3306`. Do not expose MySQL to the public internet.

---

## Prerequisites

- Two VMs in the same VPC/region (low latency between them).
- **DB VM minimum:** 2 vCPU, 4 GB RAM (give InnoDB ~50–70% of RAM via
  `innodb_buffer_pool_size`).
- **App VM:** existing Jenkins/deploy host; needs Docker.
- SSH access to both VMs.

---

## Step 1 — Provision and harden the DB VM

### 1.1 Install MySQL 8

Example on Ubuntu/Debian:

```bash
sudo apt update
sudo apt install -y mysql-server
sudo systemctl enable mysql
sudo systemctl start mysql
```

### 1.2 Create database and user

```bash
sudo mysql
```

```sql
CREATE DATABASE txnmonitor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Option A: app uses root (matches current compose dev defaults — OK for demo)
-- Option B (better): dedicated user
CREATE USER 'txnmonitor'@'%' IDENTIFIED BY '<strong-password>';
GRANT ALL PRIVILEGES ON txnmonitor.* TO 'txnmonitor'@'%';
FLUSH PRIVILEGES;
```

### 1.3 Bind MySQL to the private network

Edit `/etc/mysql/mysql.conf.d/mysqld.cnf` (path may vary):

```ini
bind-address = 0.0.0.0
```

Restart:

```bash
sudo systemctl restart mysql
```

### 1.4 Firewall — app VM only

Replace `<APP_VM_PRIVATE_IP>` with your app server's private IP:

```bash
# ufw example
sudo ufw allow from <APP_VM_PRIVATE_IP> to any port 3306
sudo ufw enable
```

Cloud consoles (AWS SG, Azure NSG, etc.): inbound TCP 3306 **source =
app VM security group / IP only**.

### 1.5 Optional MySQL tuning (DB VM)

In `mysqld.cnf` for a 4 GB RAM VM:

```ini
innodb_buffer_pool_size = 2G
max_connections = 100
```

---

## Step 2 — Migrate existing data (if you have a demo DB to keep)

Skip this section if a **fresh empty database** is fine (Flyway will create
schema on first app startup).

### 2.1 Dump from old host (single-VM setup)

On the **old** machine where MySQL currently runs:

```bash
docker exec mysql-container mysqldump -u root -p \
  --single-transaction --routines --triggers \
  txnmonitor > txnmonitor_backup.sql
```

Or if MySQL is native:

```bash
mysqldump -u root -p --single-transaction txnmonitor > txnmonitor_backup.sql
```

### 2.2 Restore on DB VM

Copy `txnmonitor_backup.sql` to the DB VM, then:

```bash
mysql -u root -p txnmonitor < txnmonitor_backup.sql
```

Verify:

```bash
mysql -u root -p -e "USE txnmonitor; SHOW TABLES;"
```

Expected tables include: `transactions`, `alerts`, `alert_transactions`,
`rule_configs`, `flyway_schema_history`.

### 2.3 Flyway checksum note

If you restored a DB that already ran Flyway on the old host, the app on the
new topology should start cleanly as long as migration files in git match what
was applied. If you hit checksum errors after a migration file rename (e.g. V3→V4),
either:

- recreate the DB and let Flyway migrate fresh, or
- run `flyway repair` (advanced — only if you know what changed).

---

## Step 3 — Test connectivity from the app VM

On the **app VM**:

```bash
# install client if needed
sudo apt install -y mysql-client

mysql -h <DB_VM_PRIVATE_IP> -u root -p -e "SELECT 1;"
```

If this fails, fix firewall/bind-address before continuing.

---

## Step 4 — Deploy the app VM without local MySQL

The repo provides `docker-compose.prod.yml` — RabbitMQ + API + frontend only.

### 4.1 Set environment variables

On the app VM, create `.env` next to the repo (or export in shell / Jenkins):

```bash
DB_URL=jdbc:mysql://<DB_VM_PRIVATE_IP>:3306/txnmonitor?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=root
DB_PASS=<your-password>
VITE_API_BASE_URL=http://<APP_VM_PUBLIC_IP>:8081
```

Use the **public** IP (or DNS) for `VITE_API_BASE_URL` — that is what browsers
use to reach the API.

### 4.2 Build and start

```bash
git pull
docker compose -f docker-compose.prod.yml build --no-cache
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml ps
```

First startup runs Flyway against the remote DB (if schema is empty).

### 4.3 Verify

```bash
curl -f http://localhost:8081/swagger-ui.html
curl -f http://localhost:8082/
API_BASE=http://<APP_VM_PUBLIC_IP>:8081 ./scripts/seed-demo.sh
```

Open the UI → confirm transactions and alerts appear (alerts may take a second
in **async** mode — refresh the alerts list).

---

## Step 5 — Jenkins / CI changes

In Jenkins credentials or job environment, add:

| Variable | Example |
|----------|---------|
| `DB_URL` | `jdbc:mysql://10.0.1.50:3306/txnmonitor?...` |
| `DB_USER` | `root` or `txnmonitor` |
| `DB_PASS` | (secret) |
| `VITE_API_BASE_URL` | `http://<app-vm-ip>:8081` |

Change the deploy stage from:

```bash
docker-compose up -d
```

to:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Do **not** commit passwords to git.

---

## Step 6 — Decommission local MySQL on app VM (optional)

Once the remote DB is verified:

```bash
# stop old all-in-one stack if still running
docker compose down

# remove local mysql volume to free disk (destroys local DB copy!)
docker volume rm democd_mysql-data   # volume name may differ — check `docker volume ls`
```

---

## Rollback

If something goes wrong:

1. Point `DB_URL` back to local MySQL (`docker compose up -d mysql`).
2. Use the standard `docker-compose.yml` (includes local MySQL).
3. Restore from `txnmonitor_backup.sql` if needed.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `Communications link failure` | Firewall / bind-address | Open 3306 app→DB only; check `bind-address` |
| `Access denied for user` | Wrong creds or host not allowed | `CREATE USER ...@'%'` or grant for app VM IP |
| App starts but no tables | Empty DB, Flyway disabled | Check logs for Flyway; verify `spring.flyway.enabled=true` |
| Alerts delayed | Async mode (expected) | Poll `GET /alerts`; check RabbitMQ queue depth at `:15672` |
| `max_connections` errors | Pool too large vs MySQL limit | Lower Hikari pool or raise MySQL `max_connections` |
| High latency | Cross-region VMs | Keep app + DB in same region/AZ |

---

## Checklist

- [ ] DB VM: MySQL 8 installed, `txnmonitor` DB created
- [ ] Firewall: 3306 open only from app VM
- [ ] App VM: `mysql -h <db-ip> ...` connectivity test passes
- [ ] Data migrated (or fresh Flyway run accepted)
- [ ] `docker-compose.prod.yml` deployed with correct `DB_URL`
- [ ] Seed script + UI smoke test pass
- [ ] k6 baseline re-run on new topology (optional but recommended)

---

## Related

- [`PHASE3_SCALE_OUT.md`](./PHASE3_SCALE_OUT.md) — what the code does
- [`docker-compose.prod.yml`](../docker-compose.prod.yml) — prod compose file
- [`README.md`](../README.md) — quick reference
