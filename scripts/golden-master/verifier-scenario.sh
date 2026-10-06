#!/usr/bin/env bash
# Vérifie statiquement qu'un scénario d'écran du golden master trouve sa place dans le front, sans rien rejouer :
#   1. sa start_url est une route déclarée (littéral '…' ou "…" dans frontend/src/adapters/routes/*.tsx, hors tests) ;
#   2. chaque data-testid de ses sélecteurs est écrit dans frontend/src (hors tests et adapters/test/) ;
#   3. chaque texte attendu (expected-dom.txt, wait_for.text) est cherché dans frontend/src, puis dans
#      services/*/reference-data/reference-data.sql, puis dans les valeurs saisies par le scénario.
# Échec (code 1, message nommant le scénario et l'élément) sur les points 1 et 2, ou si le scénario est invalide,
# inconnu ou sans input-actions.json. Les textes non trouvés ne font pas échouer : ils sont listés dans la section
# « ## SC-… » de reports/golden-master/textes-non-trouves.md (section remplacée, autres sections conservées).
# Lit seulement docs/retro/golden-master/derived/ ; n'écrit que le rapport.
# Usage : scripts/golden-master/verifier-scenario.sh SC-NNN-MM
set -euo pipefail
cd "$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"

DERIVE=docs/retro/golden-master/derived
FRONT=frontend/src
RAPPORT=reports/golden-master/textes-non-trouves.md

scenario="${1:-}"
echouer() {
  echo "KO  ${scenario:-<aucun scénario>} : $*" >&2
  exit 1
}

command -v jq >/dev/null || echouer "jq introuvable"
[[ "$scenario" =~ ^SC-([0-9]{3})-[0-9]{2}$ ]] || echouer "identifiant invalide (attendu SC-NNN-MM)"
dossier="$DERIVE/FEAT-${BASH_REMATCH[1]}/$scenario"
[ -d "$dossier" ] || echouer "scénario inconnu (aucun dossier $dossier)"
actions="$dossier/input-actions.json"
[ -f "$actions" ] || echouer "scénario sans input-actions.json (pas un scénario d'écran)"

# Fichiers sources du front, hors tests et outils de test.
sources_front() {
  find "$FRONT" -type f ! -name '*.test.*' ! -path "$FRONT/adapters/test/*" -print0
}

# 1. start_url déclarée dans le routeur.
start_url=$(jq -r '.start_url // empty' "$actions")
[ -n "$start_url" ] || echouer "start_url absente de $actions"
route="${start_url%%[?#]*}"
mapfile -d '' fichiers_routes < <(find "$FRONT/adapters/routes" -maxdepth 1 -type f -name '*.tsx' ! -name '*.test.*' -print0)
if [ "${#fichiers_routes[@]}" -eq 0 ] \
  || ! grep -qF -e "'$route'" -e "\"$route\"" -- "${fichiers_routes[@]}"; then
  echouer "route $route non déclarée dans $FRONT/adapters/routes/*.tsx"
fi
echo "OK  $scenario : route $route déclarée"

# 2. data-testid des sélecteurs écrits dans le front.
mapfile -t testids < <(jq -r '.actions[]?.selector // empty
  | scan("data-testid\\s*=\\s*[\"'"'"']([^\"'"'"']+)[\"'"'"']") | .[0]' "$actions" | sort -u)
for testid in "${testids[@]}"; do
  if ! sources_front | xargs -0 grep -qF -e "'$testid'" -e "\"$testid\"" --; then
    echouer "data-testid $testid absent de $FRONT"
  fi
  echo "OK  $scenario : data-testid $testid présent"
done

# 3. Textes attendus.
attendus="$dossier/expected-dom.txt"
mapfile -t textes < <({
  if [ -f "$attendus" ]; then cat "$attendus"; fi
  jq -r '.actions[]? | select(.type == "wait_for") | .text // empty' "$actions"
} | sed 's/[[:space:]]*$//' | grep -v '^$' | awk '!vu[$0]++' || true)
valeurs=$(jq -r '.actions[]?.value // empty' "$actions")

non_trouves=()
for texte in "${textes[@]}"; do
  if sources_front | xargs -0 grep -qF -e "$texte" --; then
    continue
  fi
  if compgen -G 'services/*/reference-data/reference-data.sql' >/dev/null \
    && grep -qF -e "$texte" -- services/*/reference-data/reference-data.sql; then
    continue
  fi
  if [ -n "$valeurs" ] && grep -qF -e "$texte" <<<"$valeurs"; then
    continue
  fi
  non_trouves+=("$texte")
done
echo "OK  $scenario : $(( ${#textes[@]} - ${#non_trouves[@]} ))/${#textes[@]} textes trouvés"

# Rapport : section du scénario remplacée (ou retirée si tout est trouvé), autres sections conservées.
mkdir -p "$(dirname "$RAPPORT")"
temporaire=$(mktemp)
trap 'rm -f "$temporaire"' EXIT
if [ -f "$RAPPORT" ]; then
  # Copie sans la section du scénario ; les lignes vides en fin de fichier sont retirées.
  awk -v titre="## $scenario" '
    BEGIN { garder = 1 }
    /^## / { garder = ($0 != titre) }
    !garder { next }
    /^[[:space:]]*$/ { vides = vides $0 "\n"; next }
    { printf "%s%s\n", vides, $0; vides = "" }
  ' "$RAPPORT" >"$temporaire"
else
  printf '# Textes attendus non trouvés\n\nProduit par scripts/golden-master/verifier-scenario.sh : textes du golden master introuvables tels quels dans frontend/src, les données de référence et les valeurs saisies (texte composé à l'"'"'exécution, ou écran pas encore livré).\n' >"$temporaire"
fi
if [ "${#non_trouves[@]}" -gt 0 ]; then
  {
    printf '\n## %s\n\n' "$scenario"
    for texte in "${non_trouves[@]}"; do
      printf -- '- « %s »\n' "$texte"
      echo "    texte non trouvé : $texte" >&2
    done
  } >>"$temporaire"
fi
cp "$temporaire" "$RAPPORT"
echo "OK  $scenario : textes non trouvés consignés dans $RAPPORT (${#non_trouves[@]})"
