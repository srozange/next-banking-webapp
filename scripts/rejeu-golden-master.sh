#!/usr/bin/env bash
# Rejeu complet ordonné du golden master (make rejeu-golden-master ; golden-master-architecture.md §7.3, NFR-PAR-05).
# Les tests générés (e2e/golden-master) ne contiennent pas de remise à zéro et ne sont jamais édités : le script
# enchaîne donc plusieurs lancements du runner, depuis e2e/golden-master :
#   1. make reset-data, puis tous les scénarios sauf ceux qui écrivent (--grep-invert), en rapport blob ;
#   2. pour chaque scénario qui écrit (writes: true, lu dans les scenario.json dérivés avec jq, jamais recopié) :
#      make reset-data, puis ce scénario seul (--grep), en rapport blob ; chaque lancement refait la connexion des rôles ;
#   3. fusion des rapports blob en un seul rapport html (report/) et json (results.json).
# TZ=Europe/Paris et LANG=fr_FR.UTF-8 sont exportés : le playwright.config.ts généré ne porte ni timezoneId ni locale.
# Un échec de remise à zéro arrête tout immédiatement, en nommant l'étape, sans lancer le runner. Un échec du runner
# est noté : les lancements continuent, puis la fusion, et le script sort en code non nul en listant les lancements en
# échec.
# Prérequis : pile démarrée par make rejeu-up ; RETRO_ADMIN_PASSWORD, RETRO_CONSEILLER_PASSWORD et RETRO_CLIENT_PASSWORD
# dans l'environnement (par exemple set -a; . ./.env; set +a) ; npm ci et npx playwright install chromium faits dans
# e2e/golden-master.
# Usage : scripts/rejeu-golden-master.sh
set -uo pipefail
racine="$(cd "$(dirname "$0")/.." && pwd)"
export TZ=Europe/Paris
export LANG=fr_FR.UTF-8

for variable in RETRO_ADMIN_PASSWORD RETRO_CONSEILLER_PASSWORD RETRO_CLIENT_PASSWORD; do
  if [ -z "${!variable:-}" ]; then
    echo "échec : variable d'environnement $variable absente" >&2
    exit 1
  fi
done
command -v jq >/dev/null || { echo "échec : jq introuvable" >&2; exit 1; }

cd "$racine/e2e/golden-master" || { echo "échec : dossier e2e/golden-master introuvable" >&2; exit 1; }
[ -x node_modules/.bin/playwright ] || {
  echo "échec : Playwright absent de e2e/golden-master (lancer npm ci puis npx playwright install chromium)" >&2
  exit 1
}

writes=$(jq -r 'select(.writes == true) | .id' "$racine"/docs/retro/golden-master/derived/*/*/scenario.json | sort) \
  || { echo "échec : lecture des scenario.json dérivés" >&2; exit 1; }
[ -n "$writes" ] || { echo "échec : aucun scénario writes: true dans le golden master dérivé" >&2; exit 1; }
echo "Scénarios qui écrivent (rejoués chacun après une remise à zéro) : $(paste -sd' ' <<<"$writes")"

rm -rf blob-report
echecs=()

remise_a_zero() {
  echo "== remise à zéro avant $1"
  make -C "$racine" reset-data || { echo "échec : remise à zéro avant $1" >&2; exit 1; }
}

remise_a_zero "les lectures"
echo "== lancement : lectures (tous les scénarios sauf ceux qui écrivent)"
PLAYWRIGHT_BLOB_OUTPUT_FILE=blob-report/lectures.zip \
  npx playwright test --grep-invert "$(paste -sd'|' <<<"$writes")" --reporter=blob || echecs+=(lectures)

for scenario in $writes; do
  remise_a_zero "$scenario"
  echo "== lancement : $scenario"
  PLAYWRIGHT_BLOB_OUTPUT_FILE="blob-report/$scenario.zip" \
    npx playwright test --grep "$scenario" --reporter=blob || echecs+=("$scenario")
done

echo "== fusion des rapports (report/ et results.json)"
PLAYWRIGHT_HTML_OUTPUT_DIR=report PLAYWRIGHT_HTML_OPEN=never PLAYWRIGHT_JSON_OUTPUT_FILE=results.json \
  npx playwright merge-reports --reporter=html,json ./blob-report || echecs+=(fusion)

if [ "${#echecs[@]}" -gt 0 ]; then
  echo "échec : lancement(s) en échec : ${echecs[*]} (détail dans e2e/golden-master/report/index.html)" >&2
  exit 1
fi
echo "Rejeu terminé sans échec : e2e/golden-master/report/index.html et results.json"
