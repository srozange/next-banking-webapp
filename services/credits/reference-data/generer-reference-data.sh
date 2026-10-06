#!/usr/bin/env bash
# Génère reference-data.sql (2 prêts EN_COURS de user1 et leurs 18 échéances, dates relatives à la date du jour lue
# par l'Horloge). Aucun secret n'est lu ni écrit.
# Usage : reference-data/generer-reference-data.sh
set -euo pipefail
module="$(cd "$(dirname "$0")/.." && pwd)"

# Générateur en portée test : compilé par Maven, lancé sur le classpath de test.
classpath="$(mktemp)"; trap 'rm -f "$classpath"' EXIT
mvn -B -q -f "$module/pom.xml" test-compile dependency:build-classpath \
  -Dmdep.includeScope=test -Dmdep.outputFile="$classpath"
java -cp "$module/target/test-classes:$module/target/classes:$(cat "$classpath")" \
  fr.nextbanking.credits.referencedata.GenerateurReferenceData "$module/reference-data/reference-data.sql"
