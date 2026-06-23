#!/usr/bin/env bash

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$PROJECT_DIR"

echo "[1/4] Pulling latest code..."
git pull --rebase

echo "[2/4] Building and starting containers..."
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build

echo "[3/4] Current container status:"
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps

echo "[4/4] Deployment finished."
echo "Open: http://$(hostname -I | awk '{print $1}')/"
