#!/usr/bin/env bash
# Ouvre VS Code sur les documents de cadrage (brief, GAPS, PRD, architecture).
# Jira et Confluence : définir JIRA_URL et CONFLUENCE_URL pour les ouvrir aussi dans le navigateur.
set -euo pipefail

racine="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

fichiers=(
  # Brief : en-tête (liens vers la doc retro), bugs, questions ouvertes
  "_bmad-output/planning-artifacts/briefs/brief-next-banking-webapp-2026-10-05/brief.md"
  # GAPS
  "docs/retro/GAPS.md"
  # PRD : chercher RG-027-003 (traçabilité)
  "_bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/prd.md"
  # Architecture : section hexagonale ; voir aussi scripts/gates/build.sh
  "_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/ARCHITECTURE-SPINE.md"
  "scripts/gates/build.sh"
  # Registre des écarts voulus du golden master
  "_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/ecarts-voulus-golden-master.md"
)

chemins=()
for f in "${fichiers[@]}"; do
  if [[ -f "$racine/$f" ]]; then
    chemins+=("$racine/$f")
  else
    echo "Absent, ignoré : $f" >&2
  fi
done

code --new-window "$racine" "${chemins[@]}"

ouvrir_url() {
  local url="$1"
  if command -v wslview >/dev/null 2>&1; then wslview "$url"
  elif command -v xdg-open >/dev/null 2>&1; then xdg-open "$url"
  else echo "Ouvrir à la main : $url"; fi
}

[[ -n "${JIRA_URL:-}" ]] && ouvrir_url "$JIRA_URL"
[[ -n "${CONFLUENCE_URL:-}" ]] && ouvrir_url "$CONFLUENCE_URL"
exit 0
