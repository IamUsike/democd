# MySQL VM Migration Guide

Last updated: 06 August 2026

**Target OS:** Fedora (DB VM and app VM). Commands use `dnf`, `firewalld`, and
systemd unit `mysqld`.

Move MySQL off the application VM so the Spring Boot JVM and InnoDB no longer
compete for CPU, RAM, and disk I/O. The application code and compose files for
this are already in the repo — this guide is the **ops runbook**.

---

## Target topology

```
┌─────────────────────────────┐       ┌─────────────────────────────┐
│  App VM (Fedora)            │       │  DB VM (Fedora)             │
│  ─────────                  │       │  ──────                     │
│  • springboot-app  :8081    │ JDBC  │  • MySQL 8         :8081    │
│  • rabbitmq        :5672    │──────▶│  • txnmonitor database      │
│  • frontend nginx  :8082    │       │  • Flyway schema (auto)     │
└─────────────────────────────┘       └─────────────────────────────┘
         ▲
         │ HTTP
    Operators / k6 / seed script
```

Security group / firewall: **only the app VM private IP** may reach MySQL on
**TCP 8081**. Do not expose MySQL to the public internet.

**Why 8081?** Inbound `3306` is blocked on our VMs. Port `8081` is open.
That is fine because the API uses `:8081` only on the **app** VM; on the **DB**
VM MySQL owns `:8081`. Local laptop compose still uses MySQL `:3306` inside Docker.

---

## Prerequisites

- Two Fedora VMs in the same VPC/region (low latency between them).
- **DB VM minimum:** 2 vCPU, 4 GB RAM (give InnoDB ~50–70% of RAM via
  `innodb_buffer_pool_size`).
- **App VM:** existing Jenkins/deploy host; needs Docker.
- SSH access to both VMs.
- Root / `sudo` on both.

---

## Step 1 — Provision and harden the DB VM

Prefer **Option A (Docker)** if Docker is already on the DB VM — fewer SELinux
and package-repo issues. Use **Option B (native `mysqld`)** if you want MySQL
as a system service.

### Option A — Docker MySQL (recommended on Fedora)

```bash
sudo dnf install -y docker
sudo systemctl enable --now docker

sudo docker run -d --name mysql-txnmonitor --restart always \
  -e MYSQL_ROOT_PASSWORD='<password>' \
  -e MYSQL_DATABASE=txnmonitor \
  -p 8081:3306 \
  mysql:8

sudo docker ps
sudo ss -tlnp | grep 8081
```

Create the app user (optional) after the container is healthy:

```bash
sudo docker exec -it mysql-txnmonitor mysql -u root -p
```

```sql
CREATE USER 'txnmonitor'@'%' IDENTIFIED BY '<strong-password>';
GRANT ALL PRIVILEGES ON txnmonitor.* TO 'txnmonitor'@'%';
FLUSH PRIVILEGES;
```

Skip to [§1.4 Firewall](#14-firewall--firewalld--app-vm-only-to-port-8081).

### Option B — Native MySQL Community Server

#### 1.1 Install MySQL 8

Fedora’s default DB package is often **MariaDB**. For **MySQL 8**, use the
official community packages (adjust the release URL for your Fedora version —
check [dev.mysql.com/downloads/repo/yum](https://dev.mysql.com/downloads/repo/yum/)):

```bash
# Example: MySQL Yum repo RPM (pick the Fedora release that matches your VM)
sudo dnf install -y https://dev.mysql.com/get/mysql84-community-release-fc41-1.noarch.rpm
# If the URL 404s, download the matching RPM from the MySQL site for your Fedora version.

sudo dnf install -y mysql-community-server
sudo systemctl enable --now mysqld
sudo systemctl status mysqld
```

First-time temporary root password (MySQL 8 community):

```bash
sudo grep 'temporary password' /var/log/mysqld.log
sudo mysql_secure_installation
```

Or set root password interactively after login with the temp password.

#### 1.2 Create database and user

```bash
sudo mysql -u root -p
```

```sql
CREATE DATABASE txnmonitor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Option A: app uses root (matches current compose demos — OK for training)
-- Option B (better): dedicated user
CREATE USER 'txnmonitor'@'%' IDENTIFIED BY '<strong-password>';
GRANT ALL PRIVILEGES ON txnmonitor.* TO 'txnmonitor'@'%';
FLUSH PRIVILEGES;
```

#### 1.3 Bind MySQL on port 8081

On Fedora, drop a snippet under `/etc/my.cnf.d/` (do not fight the main
`/etc/my.cnf` layout):

```bash
sudo tee /etc/my.cnf.d/txnmonitor.cnf <<'EOF'
[mysqld]
bind-address = 0.0.0.0
port = 8081
EOF
```

**SELinux:** non-default MySQL ports need an explicit allow (required on Fedora
enforcing mode):

```bash
# install tools if missing
sudo dnf install -y policycoreutils-python-utils

sudo semanage port -a -t mysqld_port_t -p tcp 8081
# If it already exists: sudo semanage port -m -t mysqld_port_t -p tcp 8081

sudo systemctl restart mysqld
sudo ss -tlnp | grep 8081   # should show mysqld
```

If MySQL fails to start after changing the port, check:

```bash
sudo journalctl -u mysqld -e --no-pager
sudo ausearch -m avc -ts recent | grep mysql
```

#### 1.5 Optional MySQL tuning (native)

Add to `/etc/my.cnf.d/txnmonitor.cnf` for a 4 GB RAM VM:

```ini
innodb_buffer_pool_size = 2G
max_connections = 100
```

Then `sudo systemctl restart mysqld`.

### 1.4 Firewall — firewalld — app VM only to port 8081

Fedora uses **firewalld** (not `ufw`).

Replace `<APP_VM_PRIVATE_IP>` with the app server’s private IP:

```bash
# Allow TCP 8081 only from the app VM
sudo firewall-cmd --permanent --add-rich-rule='
  rule family="ipv4"
  source address="<APP_VM_PRIVATE_IP>/32"
  port protocol="tcp" port="8081" accept'

sudo firewall-cmd --reload
sudo firewall-cmd --list-rich-rules
```

Also open **8081** in the cloud security group / NSG: inbound TCP **8081**,
source = app VM IP or security group only.

If you temporarily need to verify without a rich rule (lab only — not for prod):

```bash
sudo firewall-cmd --permanent --add-port=8081/tcp
sudo firewall-cmd --reload
```

---

## Step 2 — Migrate existing data (if you have a demo DB to keep)

Skip this section if a **fresh empty database** is fine (Flyway will create
schema on first app startup).

### 2.1 Dump from old host (single-VM setup)

On the **old** machine where MySQL currently runs:

```bash
sudo docker exec mysql-container mysqldump -u root -p \
  --single-transaction --routines --triggers \
  txnmonitor > txnmonitor_backup.sql
```

Or if MySQL is native:

```bash
mysqldump -u root -p --single-transaction txnmonitor > txnmonitor_backup.sql
```

### 2.2 Restore on DB VM

Copy `txnmonitor_backup.sql` to the DB VM, then:

**Docker MySQL:**

```bash
sudo docker exec -i mysql-txnmonitor mysql -u root -p txnmonitor < txnmonitor_backup.sql
sudo docker exec -it mysql-txnmonitor mysql -u root -p -e "USE txnmonitor; SHOW TABLES;"
```

**Native mysqld** (client must use `-P 8081`):

```bash
mysql -u root -p -P 8081 txnmonitor < txnmonitor_backup.sql
mysql -u root -p -P 8081 -e "USE txnmonitor; SHOW TABLES;"
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

On the **app VM** (Fedora):

```bash
sudo dnf install -y community-mysql
# package name may be mysql or mysql-community-client depending on repos

mysql -h <DB_VM_PRIVATE_IP> -P 8081 -u root -p -e "SELECT 1;"
```

If this fails, check in order:

1. `firewall-cmd --list-rich-rules` on DB VM
2. Cloud security group allows app → DB `:8081`
3. MySQL listening: `ss -tlnp | grep 8081` on DB VM
4. SELinux port mapping (native install only)
5. `bind-address` / Docker `-p 8081:3306`

---

## Step 4 — Deploy the app VM without local MySQL

The repo provides `docker-compose.prod.yml` — RabbitMQ + API + frontend only.

### 4.1 Set environment variables

On the app VM, create `.env` next to the repo (or export in shell / Jenkins):

```bash
DB_URL=jdbc:mysql://<DB_VM_PRIVATE_IP>:8081/txnmonitor?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=root
DB_PASS=<your-password>
VITE_API_BASE_URL=http://<APP_VM_PUBLIC_IP>:8081
```

Use the **public** IP (or DNS) for `VITE_API_BASE_URL` — that is what browsers
use to reach the API on the **app** VM (`:8081` there is Spring Boot, not MySQL).

### 4.2 Build and start

```bash
git pull
sudo docker compose -f docker-compose.prod.yml build --no-cache
sudo docker compose -f docker-compose.prod.yml up -d
sudo docker compose -f docker-compose.prod.yml ps
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
| `DB_URL` | `jdbc:mysql://10.0.1.50:8081/txnmonitor?...` |
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
sudo docker compose down

# remove local mysql volume to free disk (destroys local DB copy!)
sudo docker volume ls
sudo docker volume rm <project>_mysql-data
```

---

## Rollback

If something goes wrong:

1. Point `DB_URL` back to local MySQL (`sudo docker compose up -d mysql`).
2. Use the standard `docker-compose.yml` (includes local MySQL).
3. Restore from `txnmonitor_backup.sql` if needed.

---

## Troubleshooting (Fedora-specific)

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `Communications link failure` | firewalld / SG / wrong port | Rich rule + cloud SG for **8081**; confirm `DB_URL` has `:8081` |
| `mysqld` won’t start after `port=8081` | SELinux | `semanage port -a -t mysqld_port_t -p tcp 8081` then restart |
| `Access denied for user` | Wrong creds or host not allowed | `CREATE USER ...@'%'` or grant for app VM IP |
| `dnf` installs MariaDB instead of MySQL | Wrong package | Use MySQL community repo or **Docker** Option A |
| App starts but no tables | Empty DB, Flyway disabled | Check app logs for Flyway; verify `spring.flyway.enabled=true` |
| Alerts delayed | Async mode (expected) | Poll `GET /alerts`; RabbitMQ UI `:15672` |
| `max_connections` errors | Pool too large vs MySQL limit | Lower Hikari pool or raise MySQL `max_connections` |

Useful Fedora checks:

```bash
sudo systemctl status mysqld          # native
sudo journalctl -u mysqld -e
sudo firewall-cmd --list-all
sudo semanage port -l | grep mysql
getenforce                            # should be Enforcing or Permissive
```

---

## Checklist

- [ ] DB VM: MySQL 8 via Docker **or** native `mysqld`, `txnmonitor` DB created
- [ ] MySQL listening on **8081** (`ss -tlnp | grep 8081`)
- [ ] SELinux port allow (native only): `mysqld_port_t` on TCP 8081
- [ ] firewalld rich rule: app VM → TCP 8081
- [ ] Cloud SG: app → DB `:8081`
- [ ] App VM: `mysql -h <db-ip> -P 8081 ...` works
- [ ] Data migrated (or fresh Flyway run accepted)
- [ ] `docker-compose.prod.yml` deployed with correct `DB_URL`
- [ ] Seed script + UI smoke test pass
- [ ] k6 baseline re-run on new topology (optional but recommended)

---

## Related

- [`PHASE3_SCALE_OUT.md`](./PHASE3_SCALE_OUT.md) — what the code does
- [`docker-compose.prod.yml`](../docker-compose.prod.yml) — prod compose file
- [`README.md`](../README.md) — quick reference
