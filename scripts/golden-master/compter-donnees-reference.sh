#!/usr/bin/env bash
# Comptage des lignes des 8 bases de la pile locale (données de référence du rejeu du golden master).
# - Sans option : instantané trié « service table lignes » de toutes les tables du schéma public des 8 bases
#   (comptages exacts), sur la sortie standard. Deux instantanés pris après make reset-data sont identiques.
# - --verifier : contrôle les comptages attendus après make reset-data (153 lignes de l'import.sql du legacy, réparties
#   ci-dessous ; les projections, générées par scripts/generer-projections-reference.sh, sont en plus et ne sont pas
#   contrôlées). Code non nul en nommant chaque table en écart.
# Prérequis : les 8 conteneurs de bases tournent. Aucun secret n'est lu.
# Usage : scripts/golden-master/compter-donnees-reference.sh [--verifier]
set -uo pipefail
racine="$(cd "$(dirname "$0")/../.." && pwd)"

SERVICES=(identite-clients coeur-bancaire notifications audit credits bourse open-banking-conseiller documents)

# Comptages attendus après make reset-data : service table lignes.
ATTENDUS="identite-clients customers 7
coeur-bancaire comptes 7
coeur-bancaire operations 45
coeur-bancaire virements 0
coeur-bancaire virements_programmes 3
coeur-bancaire beneficiaires_externes 5
coeur-bancaire alertes_fraude 6
coeur-bancaire plafonds_budget 6
credits loans 2
credits loan_repayments 18
credits credit_scores 4
credits risk_profiles 4
documents documents 5
notifications notifications 6
audit audit_logs 0
bourse watchlist_items 9
open-banking-conseiller bank_products 13
open-banking-conseiller savings_products 4
open-banking-conseiller credit_products 4
open-banking-conseiller investment_products 5"

mode="instantane"
case "${1:-}" in
  "") ;;
  --verifier) mode="verifier" ;;
  *) echo "usage : $0 [--verifier]" >&2; exit 2 ;;
esac

instantane() {
  local service base
  for service in "${SERVICES[@]}"; do
    base="${service//-/_}"
    docker compose -f "$racine/compose.yaml" exec -T "postgres-$service" \
      psql -X -q -At -F ' ' -v ON_ERROR_STOP=1 -U "$base" -d "$base" -c "
        select '$service', tablename,
               (xpath('/row/c/text()', query_to_xml(format('select count(*) as c from public.%I', tablename),
                                                     false, true, '')))[1]::text
          from pg_tables
         where schemaname = 'public'
         order by tablename" \
      || { echo "échec : comptage de la base de $service" >&2; return 1; }
  done | LC_ALL=C sort
}

resultat=$(instantane) || exit 1

if [ "$mode" = "instantane" ]; then
  printf '%s\n' "$resultat"
  exit 0
fi

ecarts=0
while read -r service table attendu; do
  trouve=$(awk -v s="$service" -v t="$table" '$1 == s && $2 == t { print $3 }' <<<"$resultat")
  if [ -z "$trouve" ]; then
    echo "KO  $service $table : table absente (attendu $attendu)" >&2
    ecarts=$((ecarts + 1))
  elif [ "$trouve" != "$attendu" ]; then
    echo "KO  $service $table : $trouve ligne(s), attendu $attendu" >&2
    ecarts=$((ecarts + 1))
  fi
done <<<"$ATTENDUS"

if [ "$ecarts" -gt 0 ]; then
  echo "échec : $ecarts table(s) en écart avec les données de référence" >&2
  exit 1
fi
total=$(awk '{ s += $3 } END { print s }' <<<"$ATTENDUS")
echo "OK  comptages conformes aux données de référence ($total lignes de l'import)"
printf '%s\n' "$resultat"
