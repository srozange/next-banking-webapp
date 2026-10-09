#!/usr/bin/env bash
# Vérifie la date de référence du profil rejeu contre les données chargées (golden-master-architecture.md §5.3, §8
# point 9). HORLOGE_DEPART : date et heure de départ de l'horloge réglée, à l'heure de Paris (défaut
# 2026-11-17T10:00:00, celui de make rejeu-up).
#   Contrôle 1 : toute valeur d'une colonne date, timestamp ou timestamptz des tables chargées (schéma public des
#   8 bases, hors flyway_schema_history) est strictement antérieure à HORLOGE_DEPART (une colonne date : antérieure à
#   son jour). Exceptions, les dates planifiées : loans.date_fin, loan_repayments.date_echeance,
#   virements_programmes.prochaine_execution, virements.heure_butoir. Un timestamp sans fuseau est lu à l'heure de
#   Paris.
#   Contrôle 2 : les comptes du client 1 (user1) ont au plus 8 opérations datées du jour de HORLOGE_DEPART à Paris
#   (marge avant RAPID_SUCCESSION / HIGH_FREQUENCY pour SC-003-04).
# En cas d'écart : code non nul, chaque écart nommant base, table, colonne et date. À lancer sur des données tout juste
# remises à zéro (make reset-data). Aucun secret n'est lu.
# Usage : [HORLOGE_DEPART=2026-11-17T10:00:00] scripts/golden-master/verifier-date-reference.sh
set -uo pipefail
racine="$(cd "$(dirname "$0")/../.." && pwd)"

SERVICES=(identite-clients coeur-bancaire notifications audit credits bourse open-banking-conseiller documents)
DEPART="${HORLOGE_DEPART:-2026-11-17T10:00:00}"
OPERATIONS_MAX=8

if ! [[ "$DEPART" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}(:[0-9]{2})?$ ]]; then
  echo "échec : HORLOGE_DEPART illisible ($DEPART), format attendu AAAA-MM-JJTHH:MM:SS" >&2
  exit 2
fi

psql_service() {
  local service="$1"; shift
  local base="${service//-/_}"
  docker compose -f "$racine/compose.yaml" exec -T "postgres-$service" \
    psql -X -q -At -F ' ' -v ON_ERROR_STOP=1 -U "$base" -d "$base" -v depart="$DEPART" "$@"
}

ecarts=0
for service in "${SERVICES[@]}"; do
  base="${service//-/_}"
  # Pour chaque colonne temporelle : la plus grande valeur, si elle n'est pas antérieure au départ.
  sortie=$(psql_service "$service" <<'SQL'
set nb.depart = :'depart';
create temporary table ecarts (tab text, col text, valeur text);
do $$
declare
  depart timestamptz := (current_setting('nb.depart')::timestamp at time zone 'Europe/Paris');
  colonne record;
  plus_grande text;
begin
  for colonne in
    select c.table_name, c.column_name, c.data_type
      from information_schema.columns c
      join pg_tables t on t.schemaname = c.table_schema and t.tablename = c.table_name
     where c.table_schema = 'public'
       and c.table_name <> 'flyway_schema_history'
       and c.data_type in ('date', 'timestamp without time zone', 'timestamp with time zone')
       and (c.table_name, c.column_name) not in (('loans', 'date_fin'), ('loan_repayments', 'date_echeance'),
                                                 ('virements_programmes', 'prochaine_execution'),
                                                 ('virements', 'heure_butoir'))
  loop
    if colonne.data_type = 'date' then
      execute format('select max(%I)::text from public.%I where %I >= $1', colonne.column_name,
                     colonne.table_name, colonne.column_name)
        into plus_grande using (depart at time zone 'Europe/Paris')::date;
    elsif colonne.data_type = 'timestamp without time zone' then
      execute format('select max(%I)::text from public.%I where (%I at time zone ''Europe/Paris'') >= $1',
                     colonne.column_name, colonne.table_name, colonne.column_name)
        into plus_grande using depart;
    else
      execute format('select to_char(max(%I) at time zone ''Europe/Paris'', ''YYYY-MM-DD"T"HH24:MI:SS'')'
                     || ' from public.%I where %I >= $1', colonne.column_name, colonne.table_name,
                     colonne.column_name)
        into plus_grande using depart;
    end if;
    if plus_grande is not null then
      insert into ecarts values (colonne.table_name, colonne.column_name, plus_grande);
    end if;
  end loop;
end $$;
select tab, col, valeur from ecarts order by tab, col;
SQL
  ) || { echo "échec : lecture de la base $base" >&2; exit 1; }
  while read -r table colonne valeur; do
    [ -n "$table" ] || continue
    echo "KO  base $base, table $table, colonne $colonne : date $valeur non antérieure à HORLOGE_DEPART $DEPART" >&2
    ecarts=$((ecarts + 1))
  done <<<"$sortie"
done

# Contrôle 2 : opérations de user1 le jour du départ.
nombre=$(psql_service coeur-bancaire <<'SQL'
set nb.depart = :'depart';
select count(*)
  from operations o
  join comptes c on c.id = o.compte_id
 where c.client_id = 1
   and (o.date_operation at time zone 'Europe/Paris')::date = (current_setting('nb.depart')::timestamp)::date;
SQL
) || { echo "échec : lecture de la base coeur_bancaire" >&2; exit 1; }
if [ "$nombre" -gt "$OPERATIONS_MAX" ]; then
  echo "KO  base coeur_bancaire, table operations, colonne date_operation : $nombre opérations de user1 le" \
    "${DEPART%%T*}, au plus $OPERATIONS_MAX" >&2
  ecarts=$((ecarts + 1))
fi

if [ "$ecarts" -gt 0 ]; then
  echo "échec : $ecarts écart(s) avec la date de référence HORLOGE_DEPART $DEPART" >&2
  exit 1
fi
echo "OK  toutes les dates chargées sont antérieures à $DEPART (hors dates planifiées) ;" \
  "$nombre opération(s) de user1 ce jour-là (au plus $OPERATIONS_MAX)"
