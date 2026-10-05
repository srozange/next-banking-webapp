#!/usr/bin/env bash
# Contrôle statique du golden master : rien n'est rejoué, l'application n'a pas besoin de tourner.
# Le rejeu réel n'a lieu qu'une fois, à la fin du développement (étape G6 de pilote).
#   1. la dérivation à blanc du mapping sort en 0 : replay-mapping.json est complet et valide ;
#   2. le golden master dérivé est identique à une dérivation fraîche : ni retouché, ni périmé ;
#   3. les tests générés sont identiques à une génération fraîche (export-golden-master-tests.py --check).
# Un écart -> KO, code 1. Il se corrige dans replay-mapping.json, puis on redérive et on régénère.
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"
RETRO="${GM_RETRO_DIR:-docs/retro}"
GM="$RETRO/golden-master"
MAP="${GM_MAPPING:-replay-mapping.json}"
DIR="${GM_E2E_DIR:-e2e/golden-master}"
command -v uv >/dev/null || { echo "KO  uv introuvable"; exit 1; }
for f in "$MAP" "$RETRO/tools/derive-golden-master.py" "$RETRO/tools/export-golden-master-tests.py"; do
  [ -f "$f" ] || { echo "KO  $f absent"; exit 1; }
done
[ -d "$GM/derived" ] || { echo "KO  $GM/derived absent : dériver depuis le mapping"; exit 1; }

tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
ko=0
if uv run --quiet "$RETRO/tools/derive-golden-master.py" derive --gm-dir "$GM" --mapping "$MAP" \
     --output-dir "$tmp/derived" >"$tmp/derive.json" 2>"$tmp/derive.err"; then
  if diff -rq "$tmp/derived" "$GM/derived" >"$tmp/diff.txt"; then
    echo "OK  golden master dérivé à jour et intact"
  else
    echo "KO  golden master dérivé retouché ou périmé (redériver depuis le mapping) :"
    sed "s|$tmp/derived|<dérivation fraîche>|g; s/^/    /" "$tmp/diff.txt" | head -20; ko=1
  fi
else
  echo "KO  dérivation à blanc en échec (mapping incomplet ou invalide) :"
  sed 's/^/    /' "$tmp/derive.err" | head -20; ko=1
fi
if uv run --quiet "$RETRO/tools/export-golden-master-tests.py" --runner playwright --derived "$GM/derived" \
     --mapping "$MAP" --gm-dir "$GM" --output "$DIR" --check >"$tmp/check.json" 2>"$tmp/check.err"; then
  echo "OK  tests générés à jour et intacts"
else
  echo "KO  tests générés retouchés ou périmés (régénérer) :"
  sed 's/^/    /' "$tmp/check.err" | head -20; ko=1
fi
[ "$ko" -eq 0 ]
