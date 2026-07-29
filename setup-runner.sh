#!/usr/bin/env bash
# Self-hosted GitHub Actions runner setup — Amazon Linux 2023
# Usage: ./setup-runner.sh <GITHUB_REPO_URL> <RUNNER_TOKEN>
# Example: ./setup-runner.sh https://github.com/Amaan-Khan14/SpringBootDev BB36FX...
#
# Note: the token is short-lived (~1hr). Get a fresh one from:
# Repo -> Settings -> Actions -> Runners -> New self-hosted runner

set -e

REPO_URL="$1"
TOKEN="$2"

if [ -z "$REPO_URL" ] || [ -z "$TOKEN" ]; then
  echo "Usage: $0 <GITHUB_REPO_URL> <RUNNER_TOKEN>"
  exit 1
fi

RUNNER_VERSION="2.336.0"

# 1. Download
mkdir -p actions-runner && cd actions-runner
curl -o actions-runner-linux-x64-${RUNNER_VERSION}.tar.gz -L \
  "https://github.com/actions/runner/releases/download/v${RUNNER_VERSION}/actions-runner-linux-x64-${RUNNER_VERSION}.tar.gz"

# 2. Extract
tar xzf "actions-runner-linux-x64-${RUNNER_VERSION}.tar.gz"

# 3. Install .NET dependencies (AL2023 fallback, in case installdependencies.sh
#    fails to detect the OS)
sudo ./bin/installdependencies.sh || sudo dnf install -y icu libicu

# 4. Configure the runner
./config.sh --url "$REPO_URL" --token "$TOKEN" --unattended

# 5. Install and start as a systemd service (persists after SSH/DCV disconnect)
sudo ./svc.sh install
sudo ./svc.sh start
sudo ./svc.sh status
