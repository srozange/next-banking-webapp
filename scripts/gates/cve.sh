#!/usr/bin/env bash
# Bases figées : la mise à jour se fait avant le run, pas pendant.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
mkdir -p reports
T="trivy fs --scanners vuln --severity HIGH,CRITICAL --skip-db-update --skip-java-db-update --quiet"

# 1. Journal non bloquant : tout, y compris les vulnérabilités sans correctif
$T --exit-code 0 --format json --output reports/cve-all.json .
jq -r '.Results[]?.Vulnerabilities[]? | select((.FixedVersion // "") == "")
       | [.Severity, .VulnerabilityID, .PkgName, .InstalledVersion] | @tsv' \
  reports/cve-all.json | sort -u > reports/cve-unfixed.tsv
echo "Sans correctif (non bloquant) : $(wc -l < reports/cve-unfixed.tsv) ligne(s), voir reports/cve-unfixed.tsv"

# 2. Porte bloquante : seulement ce qui a un correctif
$T --ignore-unfixed --exit-code 1 .

# 3. Dependency-Check : autoUpdate est à false dans le profil security
mvn -B -q -Psecurity org.owasp:dependency-check-maven:aggregate
