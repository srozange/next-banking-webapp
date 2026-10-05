#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
mvn -B -q clean verify -DfailIfNoTests=true
mvn -B -q test -Dtest='*ArchitectureTest'
( cd frontend && npm ci --silent \
  && npx depcruise --config .dependency-cruiser.cjs src \
  && npx tsc --noEmit \
  && npx vitest run --coverage )
