#!/usr/bin/env bash
# Prouve que le test ArchUnit mord : introduit une dépendance domaine -> adaptateur et attend un échec.
# usage : scripts/gates/canary-arch.sh services/orders com/acme/orders
set -uo pipefail
svc="$1"; pkg="$2"
src="$svc/src/main/java"
f="$src/$pkg/domain/CanaryViolation.java"
trap 'rm -f "$f"' EXIT

target=$(grep -rl --include='*.java' -E '^public (final )?(class|record|interface)' "$src/$pkg/adapter" 2>/dev/null | head -1)
[ -n "$target" ] || { echo "KO  aucun type public sous $pkg/adapter"; exit 2; }
fqcn=$(echo "${target#"$src/"}" | sed 's#/#.#g; s#\.java$##')

cat > "$f" <<JAVA
package $(echo "$pkg" | tr / .).domain;
class CanaryViolation { static final Class<?> LEAK = $fqcn.class; }
JAVA

mvn -q -f "$svc/pom.xml" test -Dtest='*ArchitectureTest' >/dev/null 2>&1 && {
  echo "KO  le test d'architecture passe malgré la violation"; exit 1; }
if grep -rqs 'Architecture Violation' "$svc/target/surefire-reports"; then
  echo "OK  ArchUnit détecte la dépendance domaine -> adaptateur"
else
  echo "KO  le build échoue, mais pas à cause d'ArchUnit (voir target/surefire-reports)"; exit 1
fi
