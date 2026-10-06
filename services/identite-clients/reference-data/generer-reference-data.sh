#!/usr/bin/env bash
# Génère reference-data.sql (7 utilisateurs du legacy, mots de passe hachés {bcrypt}) depuis RETRO_ADMIN_PASSWORD,
# RETRO_CONSEILLER_PASSWORD et RETRO_CLIENT_PASSWORD. Les mots de passe ne sont ni affichés ni écrits.
# Usage : RETRO_ADMIN_PASSWORD=… RETRO_CONSEILLER_PASSWORD=… RETRO_CLIENT_PASSWORD=… reference-data/generer-reference-data.sh
set -euo pipefail
module="$(cd "$(dirname "$0")/.." && pwd)"

for variable in RETRO_ADMIN_PASSWORD RETRO_CONSEILLER_PASSWORD RETRO_CLIENT_PASSWORD; do
  if [ -z "${!variable:-}" ]; then
    echo "variable d'environnement $variable absente" >&2
    exit 1
  fi
done

# Générateur en portée test : compilé par Maven, lancé sur le classpath de test.
classpath="$(mktemp)"; trap 'rm -f "$classpath"' EXIT
mvn -B -q -f "$module/pom.xml" test-compile dependency:build-classpath \
  -Dmdep.includeScope=test -Dmdep.outputFile="$classpath"
java -cp "$module/target/test-classes:$module/target/classes:$(cat "$classpath")" \
  fr.nextbanking.identiteclients.referencedata.GenerateurReferenceData "$module/reference-data/reference-data.sql"
