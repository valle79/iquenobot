#!/usr/bin/env bash
set -euo pipefail

ENV=${1:-prod}

echo "=== IquenoBot Deployment ==="

case "$ENV" in
  prod)
    echo "Deploying to production..."
    docker compose -f docker-compose.prod.yml --env-file .env up -d --build
    ;;
  dev)
    echo "Deploying to development..."
    docker compose -f docker-compose.yml up -d --build
    ;;
  down)
    echo "Stopping services..."
    docker compose -f docker-compose.prod.yml down
    ;;
  logs)
    echo "Tailing logs..."
    docker compose -f docker-compose.prod.yml logs -f
    ;;
  *)
    echo "Usage: ./scripts/deploy.sh [prod|dev|down|logs]"
    exit 1
    ;;
esac

echo "=== Done ==="
