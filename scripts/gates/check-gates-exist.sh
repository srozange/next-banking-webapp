#!/usr/bin/env bash
# Échoue si un contrôle manque ou si un seuil a été modifié. Aucune appréciation, que des faits.
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"
fail=0
ok() { echo "OK  $*"; }
ko() { echo "KO  $*"; fail=1; }
has() { # has <fichier> <motif grep -E> <libellé>
  if grep -Eq "$2" "$1" 2>/dev/null; then ok "$3"; else ko "$3"; fi
}

tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT

services=$(find services -mindepth 2 -maxdepth 2 -name pom.xml 2>/dev/null | sort)
[ -n "$services" ] || ko "aucun service trouvé sous services/*/pom.xml"

for pom in $services; do
  svc=$(dirname "$pom"); name=$(basename "$svc")
  # POM effectif : tient compte du POM parent et des profils
  mvn -q -f "$pom" help:effective-pom -Doutput="$tmp/$name.xml" >/dev/null 2>&1 \
    || ko "$name : POM effectif illisible"
  mvn -q -f "$pom" help:effective-pom -Pmutation -Doutput="$tmp/$name-mut.xml" >/dev/null 2>&1
  mvn -q -f "$pom" help:effective-pom -Psecurity -Doutput="$tmp/$name-sec.xml" >/dev/null 2>&1

  if find "$svc/src/test" -name '*ArchitectureTest.java' 2>/dev/null | grep -q .; then
    ok "$name : test ArchUnit présent"
  else
    ko "$name : aucun *ArchitectureTest.java"
  fi
  has "$tmp/$name.xml" 'archunit-junit5'            "$name : dépendance archunit-junit5"
  has "$tmp/$name.xml" 'jacoco-maven-plugin'        "$name : plugin JaCoCo"
  has "$tmp/$name.xml" '<goal>check</goal>'         "$name : goal JaCoCo check lié au build"
  has "$tmp/$name.xml" '<minimum>1(\.0+)?</minimum>' "$name : seuil 100 % présent"
  has "$tmp/$name.xml" '<minimum>0\.80?</minimum>'   "$name : seuil 80 % présent"
  if grep -Eq '<haltOnFailure>false' "$tmp/$name.xml"; then ko "$name : haltOnFailure désactivé"; fi
  if grep -Eq 'pitest-maven' "$tmp/$name.xml"; then ko "$name : PIT actif hors profil mutation"; fi
  has "$tmp/$name-mut.xml" 'pitest-maven'                         "$name : profil mutation avec PIT"
  has "$tmp/$name-mut.xml" '<mutationThreshold>85</mutationThreshold>' "$name : seuil de mutation 85"
  has "$tmp/$name-mut.xml" '<historyInputFile>'                   "$name : historique PIT configuré"
  has "$tmp/$name-sec.xml" 'dependency-check-maven'               "$name : profil security avec Dependency-Check"
  has "$tmp/$name-sec.xml" '<failBuildOnCVSS>7(\.0)?</failBuildOnCVSS>' "$name : failBuildOnCVSS 7"
  has "$tmp/$name-sec.xml" '<autoUpdate>false</autoUpdate>'       "$name : base Dependency-Check figée pendant les scans"
  has "$tmp/$name-sec.xml" '<ossIndexAnalyzerEnabled>false'         "$name : OSS Index désactivé"
  if grep -Eq '<nvdApiKey>' "$tmp/$name-sec.xml"; then ko "$name : clé NVD écrite dans le POM"; fi
done

f=frontend
has "$f/.dependency-cruiser.cjs" 'severity: *["'"'"']error' "front : règles dependency-cruiser en error"
has "$f/.dependency-cruiser.cjs" 'dependencyTypes'          "front : règle sans paquet npm dans l'hexagone"
has "$f/package.json" '"dependency-cruiser"'   "front : dependency-cruiser installé"
has "$f/package.json" '"@stryker-mutator/core"' "front : StrykerJS installé"
cfg=$(ls "$f"/vitest.config.* "$f"/vite.config.* 2>/dev/null | head -1)
has "${cfg:-/nonexistent}" 'thresholds'        "front : seuils de couverture Vitest"
has "${cfg:-/nonexistent}" 'src/domain'        "front : seuil dédié au domaine"
scfg=$(ls "$f"/stryker.config.* 2>/dev/null | head -1)
has "${scfg:-/nonexistent}" 'break["'"'"']?: *85' "front : Stryker break 85"

compose=$(ls compose.yaml compose.yml docker-compose.yml 2>/dev/null | head -1)
if [ -n "$compose" ] && docker compose -f "$compose" config --services >"$tmp/svc.txt" 2>/dev/null; then
  has "$tmp/svc.txt" 'otel|collector' "compose : collecteur OpenTelemetry"
  has "$tmp/svc.txt" 'jaeger'         "compose : Jaeger"
  for pom in $services; do
    n=$(basename "$(dirname "$pom")"); has "$tmp/svc.txt" "$n" "compose : service $n"
  done
else
  ko "compose : fichier absent ou invalide"
fi

for f2 in config/dependency-check-suppressions.xml .trivyignore; do
  if [ -f "$f2" ]; then ok "$f2 présent"; else ko "$f2 manquant"; fi
done

for s in trace-smoke.sh cve.sh; do
  if [ -x "scripts/gates/$s" ]; then ok "scripts/gates/$s exécutable"; else ko "scripts/gates/$s manquant"; fi
done

exit $fail
