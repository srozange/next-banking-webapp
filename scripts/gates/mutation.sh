#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
mvn -B -q -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage
( cd frontend && npx stryker run --incremental )
