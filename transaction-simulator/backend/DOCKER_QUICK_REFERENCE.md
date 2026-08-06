# Docker Quick Reference - Transaction Simulator Backend

## Essential Commands

### Build and Run

```bash
# Build image
docker build -t transaction-simulator:latest .

# Start service (builds if needed)
docker compose up --build

# Start in background
docker compose up -d --build

# Start without rebuild
docker compose up

# Start in background without rebuild
docker compose up -d
```

### Verify Running

```bash
# Check if service is running
docker compose ps

# Check health status
docker compose ps --format "table {{.Names}}\t{{.Status}}\t{{.Health}}"

# Check container ID
docker compose ps --quiet
```

### View Logs

```bash
# Stream logs (live)
docker compose logs -f

# Stream simulator backend only
docker compose logs -f transaction-simulator-backend

# Last 50 lines
docker compose logs --tail 50

# Last 100 lines + follow
docker compose logs --tail 100 -f
```

### Test Endpoints

```bash
# Health check
curl http://localhost:8080/health

# Start simulator
curl -X POST http://localhost:8080/api/simulator/start \
  -H "Content-Type: application/json" \
  -d '{"tps": 10, "duration": 60, "mode": "NORMAL"}'

# Stop simulator
curl -X POST http://localhost:8080/api/simulator/stop

# Get simulator status
curl http://localhost:8080/api/simulator/status
```

### Stop and Clean Up

```bash
# Stop gracefully
docker compose down

# Stop and remove volumes
docker compose down -v

# Kill container (force stop)
docker compose kill

# Remove image
docker rmi transaction-simulator:latest

# Remove all unused images
docker image prune -a
```

## Environment Variables

### Set via .env file

```bash
# Copy template
cp .env.example .env

# Edit .env
TRANSACTION_API_URL=http://localhost:8081/api/v1/transactions
SERVER_PORT=8080
LOG_LEVEL=debug
```

### Override via command line

```bash
# Single variable
SIMULATOR_PORT=9090 docker compose up

# Multiple variables
TRANSACTION_API_URL=http://api:8080 \
SERVER_PORT=8080 \
LOG_LEVEL=debug \
docker compose up
```

### Check variables in container

```bash
# All environment variables
docker exec transaction-simulator-backend env

# Specific variable
docker exec transaction-simulator-backend env | grep TRANSACTION_API_URL

# Sorted
docker exec transaction-simulator-backend env | sort
```

## Container Operations

### Execute commands

```bash
# Access shell
docker exec -it transaction-simulator-backend /bin/sh

# Run command
docker exec transaction-simulator-backend ls -la /app

# Check running process
docker exec transaction-simulator-backend ps aux
```

### Resource monitoring

```bash
# Live resource usage
docker stats transaction-simulator-backend

# CPU and memory in one line
docker exec transaction-simulator-backend ps aux | head -1 && \
  docker exec transaction-simulator-backend ps aux | grep transaction-simulator
```

### Container inspection

```bash
# Full container info
docker inspect transaction-simulator-backend

# Just IP address
docker inspect -f '{{.NetworkSettings.IPAddress}}' transaction-simulator-backend

# Just hostname
docker inspect -f '{{.Config.Hostname}}' transaction-simulator-backend

# Environment variables
docker inspect transaction-simulator-backend | grep -A 50 Env

# Port mappings
docker inspect -f '{{.NetworkSettings.Ports}}' transaction-simulator-backend
```

## Image Operations

### Build specific

```bash
# Verbose build with output
docker build --progress=plain -t transaction-simulator:latest .

# Specify Dockerfile
docker build -f Dockerfile -t transaction-simulator:latest .

# With build arguments
docker build --build-arg VERSION=1.0.0 -t transaction-simulator:latest .

# No cache (full rebuild)
docker build --no-cache -t transaction-simulator:latest .
```

### Image inspection

```bash
# List images
docker images | grep transaction-simulator

# Image size
docker images --format "table {{.Repository}}\t{{.Size}}" | grep transaction-simulator

# Image history/layers
docker history transaction-simulator:latest

# Full image info
docker inspect transaction-simulator:latest

# Size of all layers
docker inspect transaction-simulator:latest | grep -i size
```

## Networking

### Check network

```bash
# List networks
docker network ls

# Inspect monitoring-network
docker network inspect monitoring-network

# Services on the network
docker network inspect monitoring-network | grep -A 20 "Containers"
```

### Test connectivity

```bash
# From host to container
curl http://localhost:8080/health

# From container to host (if needed)
docker exec transaction-simulator-backend \
  wget -q -O- http://localhost:8080/health

# From container to another service (in same network)
docker exec transaction-simulator-backend \
  wget -q -O- http://transaction-monitoring-backend:8080/health
```

## Troubleshooting

### Build issues

```bash
# Check if Docker daemon is running (Linux/Mac)
docker version

# Verify Dockerfile syntax
docker build --dry-run .

# Build with verbose output
docker build --progress=plain -t transaction-simulator:latest .

# Check Docker resources (no space)
docker system df
```

### Runtime issues

```bash
# Container logs (full)
docker compose logs

# Container logs (20 lines, follow)
docker compose logs --tail 20 -f

# Check for container crash on startup
docker compose up  # don't use -d, watch output

# Inspect error state
docker inspect transaction-simulator-backend | grep -A 5 State

# Check if port is already in use
netstat -ano | findstr :8080  # Windows
lsof -i :8080                  # Linux/Mac
```

### Network issues

```bash
# Test DNS resolution
docker exec transaction-simulator-backend \
  nslookup transaction-monitoring-backend

# Test connectivity
docker exec transaction-simulator-backend \
  ping -c 4 transaction-monitoring-backend

# Check network settings
docker inspect transaction-simulator-backend | grep -A 20 NetworkSettings
```

## Advanced Operations

### Database/Volume operations

```bash
# List volumes
docker volume ls

# Remove unused volumes
docker volume prune

# Inspect volume
docker volume inspect monitoring-network
```

### Multi-compose files

```bash
# Combine multiple compose files
docker compose -f docker-compose.yml \
               -f docker-compose.override.yml \
               up -d

# Production override
docker compose -f docker-compose.yml \
               -f docker-compose.prod.yml \
               up -d
```

### Debugging

```bash
# Interactive shell with debugging
docker run -it --rm transaction-simulator:latest /bin/sh

# Run with different command
docker run -it --rm \
  -e TRANSACTION_API_URL=http://localhost:8081 \
  transaction-simulator:latest \
  /bin/sh

# Verbose logging
LOG_LEVEL=debug docker compose up

# Trace system calls
docker exec transaction-simulator-backend \
  strace -e trace=network wget http://localhost:8080/health
```

## Performance Testing

### Load testing

```bash
# Simple request loop (Bash)
for i in {1..100}; do
  curl http://localhost:8080/health &
done
wait

# Send transactions
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/simulator/start \
    -H "Content-Type: application/json" \
    -d '{"tps": 50, "duration": 30, "mode": "NORMAL"}'
done
```

### Monitor performance

```bash
# CPU/Memory usage during load
while true; do docker stats --no-stream; sleep 2; done

# Network I/O
docker exec transaction-simulator-backend \
  cat /proc/net/dev

# Check for resource limits being hit
docker compose ps
docker logs transaction-simulator-backend | grep -i "memory\|cpu"
```

## Common Error Solutions

| Error | Solution |
|-------|----------|
| `Error response from daemon: No such image` | Build first: `docker build -t transaction-simulator:latest .` |
| `port 8080 is already allocated` | Change port: `SIMULATOR_PORT=9090 docker compose up` |
| `Container exits immediately` | Check logs: `docker compose logs --tail 50` |
| `Cannot connect to container` | Check health: `docker compose ps` |
| `TRANSACTION_API_URL error` | Set env var: `docker compose config` or update `.env` |
| `Permission denied` | Use `sudo` or add user to docker group |
| `Disk space full` | Prune: `docker system prune -a -f` |

## File Size Optimization

```bash
# Check image size
docker images transaction-simulator

# Check layers
docker history transaction-simulator:latest

# Total size on disk
du -sh $(docker inspect -f '{{.GraphDriver.Data.MergedDir}}' transaction-simulator-backend)

# Remove duplicate images/tags
docker image prune

# Remove intermediate build layers
docker builder prune
```

## Health Monitoring

```bash
# Health status
docker compose ps --format "table {{.Names}}\t{{.Health}}"

# Health log
docker inspect transaction-simulator-backend | grep -A 20 "Health"

# Manual health check
curl -i http://localhost:8080/health

# Continuous monitoring
watch -n 5 'docker compose ps --format "table {{.Names}}\t{{.Health}}"'
```

## Version Information

```bash
# Docker version
docker --version

# Docker Compose version
docker compose --version

# Go version in container
docker exec transaction-simulator-backend go version

# Alpine version
docker exec transaction-simulator-backend cat /etc/alpine-release

# Check if binary is static
docker exec transaction-simulator-backend file /app/transaction-simulator
```

## Useful Aliases (Bash/Zsh)

```bash
# Add to ~/.bashrc or ~/.zshrc

# Start simulator
alias sim-start='docker compose up -d --build'

# Stop simulator
alias sim-stop='docker compose down'

# View logs
alias sim-logs='docker compose logs -f'

# Restart simulator
alias sim-restart='docker compose restart'

# Health check
alias sim-health='curl http://localhost:8080/health'

# Interactive shell
alias sim-shell='docker exec -it transaction-simulator-backend /bin/sh'

# Full rebuild
alias sim-rebuild='docker compose down && docker build --no-cache -t transaction-simulator:latest . && docker compose up -d'
```

## Windows-Specific (PowerShell)

```powershell
# Start simulator
function Start-Simulator { docker compose up -d --build }

# Stop simulator
function Stop-Simulator { docker compose down }

# View logs
function Get-SimulatorLogs { docker compose logs -f }

# Health check
function Test-SimulatorHealth { 
    (Invoke-RestMethod -Uri http://localhost:8080/health).status 
}

# Alias definitions
Set-Alias -Name sim-start -Value Start-Simulator
Set-Alias -Name sim-stop -Value Stop-Simulator
Set-Alias -Name sim-logs -Value Get-SimulatorLogs
Set-Alias -Name sim-health -Value Test-SimulatorHealth
```

## Useful Scripts

### Restart on failure

```bash
#!/bin/bash
# save as: restart-on-fail.sh

while true; do
  docker compose up
  echo "Service stopped, restarting..."
  sleep 5
done
```

### Monitor and alert

```bash
#!/bin/bash
# save as: monitor.sh

while true; do
  status=$(docker compose ps --format "{{.Health}}")
  if [ "$status" != "healthy" ]; then
    echo "WARNING: Service unhealthy!" 
    docker compose logs --tail 10
  fi
  sleep 30
done
```

---

**Quick Help**: `docker compose --help` or `docker help`

**More Info**: See `DOCKER_SETUP.md` for comprehensive guide

