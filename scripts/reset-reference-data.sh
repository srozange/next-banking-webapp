#!/usr/bin/env bash
# Remise à zéro des données de référence des 8 services (make reset-data), idempotente (golden-master-architecture.md
# §5.2, AD-11) :
#   1. arrêt des 8 conteneurs de services (docker compose stop) : les consommateurs Kafka s'arrêtent avec eux ;
#   2. dans chaque base, TRUNCATE … RESTART IDENTITY CASCADE de toutes les tables du schéma public sauf
#      flyway_schema_history (tables métier, projections, outbox, outbox_audit, evenements_traites) ;
#   3. topics non internes mémorisés avec leur nombre de partitions, groupes de consommateurs supprimés, topics
#      supprimés, attente de leur disparition, puis recréation à l'identique ;
#   4. chargement de services/<id>/reference-data/reference-data.sql puis de projection-clients.sql s'il existe
#      (psql -v ON_ERROR_STOP=1 --single-transaction) ;
#   5. séquences et identités replacées au-dessus du plus grand identifiant, pour toutes les tables ;
#   6. relance des mêmes conteneurs (docker compose start, sans recréation : leur environnement, donc HORLOGE_DECALAGE,
#      est conservé) et attente de leur santé ; le redémarrage vide aussi les caches.
# Chaque étape est annoncée ; un échec sort en code non nul en nommant l'étape. Le script ne lit ni n'écrit HORLOGE_*.
# Prérequis : la pile tourne (make rejeu-up ou docker compose up), bases et Kafka démarrés.
# Usage : scripts/reset-reference-data.sh
set -uo pipefail
racine="$(cd "$(dirname "$0")/.." && pwd)"
cd "$racine" || exit 1

SERVICES=(identite-clients coeur-bancaire notifications audit credits bourse open-banking-conseiller documents)
KAFKA_BIN=/opt/kafka/bin
BOOTSTRAP=localhost:9092
ATTENTE_SANTE_SECONDES=${NB_ATTENTE_SANTE_SECONDES:-300}

compose() { docker compose -f "$racine/compose.yaml" "$@"; }

etape_courante=""
etape() { etape_courante="$1"; echo "== étape $1"; }
echec() {
  echo "échec : étape $etape_courante${1:+ ($1)}" >&2
  echo "Les conteneurs de services arrêtés ne sont pas relancés : corriger la cause, puis relancer make reset-data." >&2
  exit 1
}

# psql dans le conteneur de la base du service ; base et utilisateur portent l'identifiant du service (tirets -> _).
psql_service() {
  local service="$1"; shift
  local base="${service//-/_}"
  compose exec -T "postgres-$service" psql -X -q -v ON_ERROR_STOP=1 -U "$base" -d "$base" "$@"
}

kafka() { compose exec -T kafka "$@"; }

# 1. Arrêt des consommateurs.
etape "1/6 : arrêt des 8 conteneurs de services (consommateurs Kafka)"
compose stop "${SERVICES[@]}" || echec "docker compose stop"

# 2. Vidage des tables.
etape "2/6 : vidage des tables du schéma public des 8 bases"
for service in "${SERVICES[@]}"; do
  psql_service "$service" <<'SQL' || echec "base de $service"
do $$
declare
  tables text;
begin
  select string_agg(format('%I.%I', schemaname, tablename), ', ')
    into tables
    from pg_tables
   where schemaname = 'public' and tablename <> 'flyway_schema_history';
  if tables is not null then
    execute 'truncate table ' || tables || ' restart identity cascade';
  end if;
end $$;
SQL
  echo "   $service : tables vidées"
done

# 3. Topics Kafka : mémorisés, groupes supprimés, topics supprimés puis recréés à l'identique.
etape "3/6 : suppression et recréation des topics Kafka"
description=$(kafka "$KAFKA_BIN/kafka-topics.sh" --bootstrap-server "$BOOTSTRAP" --describe --exclude-internal) \
  || echec "lecture des topics"
# Lignes de résumé : « Topic: <nom> TopicId: … PartitionCount: <n> ReplicationFactor: <r> … ».
mapfile -t topics < <(awk '/^Topic:/ { nom = ""; parts = ""; repl = "";
    for (i = 1; i <= NF; i++) {
      if ($i == "Topic:") nom = $(i + 1);
      if ($i == "PartitionCount:") parts = $(i + 1);
      if ($i == "ReplicationFactor:") repl = $(i + 1);
    }
    if (nom != "" && parts != "") print nom, parts, repl }' <<<"$description" | sort -u)
echo "   ${#topics[@]} topic(s) mémorisé(s)"
groupes=$(kafka "$KAFKA_BIN/kafka-consumer-groups.sh" --bootstrap-server "$BOOTSTRAP" --list) \
  || echec "lecture des groupes de consommateurs"
for groupe in $groupes; do
  kafka "$KAFKA_BIN/kafka-consumer-groups.sh" --bootstrap-server "$BOOTSTRAP" --delete --group "$groupe" >/dev/null \
    || echec "suppression du groupe $groupe"
done
echo "   groupes de consommateurs supprimés"
if [ "${#topics[@]}" -gt 0 ]; then
  noms=$(printf '%s\n' "${topics[@]}" | awk '{ print $1 }' | paste -sd, -)
  kafka "$KAFKA_BIN/kafka-topics.sh" --bootstrap-server "$BOOTSTRAP" --delete --topic "$noms" >/dev/null \
    || echec "suppression des topics"
  for _ in $(seq 1 60); do
    restants=$(kafka "$KAFKA_BIN/kafka-topics.sh" --bootstrap-server "$BOOTSTRAP" --list --exclude-internal) \
      || echec "liste des topics"
    [ -z "$restants" ] && break
    sleep 1
  done
  [ -z "$restants" ] || echec "topics toujours présents après suppression : $restants"
  for ligne in "${topics[@]}"; do
    read -r nom partitions replication <<<"$ligne"
    kafka "$KAFKA_BIN/kafka-topics.sh" --bootstrap-server "$BOOTSTRAP" --create --topic "$nom" \
      --partitions "$partitions" --replication-factor "${replication:-1}" >/dev/null \
      || echec "recréation du topic $nom"
  done
  echo "   topics recréés : $noms"
fi

# 4. Chargement des données de référence.
etape "4/6 : chargement des données de référence"
for service in "${SERVICES[@]}"; do
  dossier="services/$service/reference-data"
  [ -f "$dossier/reference-data.sql" ] || echec "$dossier/reference-data.sql absent"
  psql_service "$service" --single-transaction <"$dossier/reference-data.sql" >/dev/null \
    || echec "chargement de $dossier/reference-data.sql"
  if [ -f "$dossier/projection-clients.sql" ]; then
    psql_service "$service" --single-transaction <"$dossier/projection-clients.sql" >/dev/null \
      || echec "chargement de $dossier/projection-clients.sql"
  fi
  echo "   $service : données chargées"
done

# 5. Séquences et identités au-dessus du plus grand identifiant.
etape "5/6 : séquences replacées au-dessus du plus grand identifiant"
for service in "${SERVICES[@]}"; do
  psql_service "$service" <<'SQL' >/dev/null || echec "séquences de $service"
do $$
declare
  colonne record;
  sequence_associee text;
  plus_grand bigint;
begin
  for colonne in
    select c.table_name, c.column_name
      from information_schema.columns c
      join pg_tables t on t.schemaname = c.table_schema and t.tablename = c.table_name
     where c.table_schema = 'public'
  loop
    sequence_associee := pg_get_serial_sequence(format('public.%I', colonne.table_name), colonne.column_name);
    if sequence_associee is not null then
      execute format('select coalesce(max(%I), 0) from public.%I', colonne.column_name, colonne.table_name)
        into plus_grand;
      perform setval(sequence_associee, greatest(plus_grand, 1), plus_grand > 0);
    end if;
  end loop;
end $$;
SQL
done
echo "   séquences replacées"

# 6. Relance des mêmes conteneurs et attente de leur santé.
etape "6/6 : relance des 8 conteneurs de services et attente de leur santé"
compose start "${SERVICES[@]}" || echec "docker compose start"
limite=$((SECONDS + ATTENTE_SANTE_SECONDES))
for service in "${SERVICES[@]}"; do
  conteneur=$(compose ps -q "$service") || echec "conteneur de $service introuvable"
  [ -n "$conteneur" ] || echec "conteneur de $service introuvable"
  until [ "$(docker inspect -f '{{.State.Health.Status}}' "$conteneur" 2>/dev/null)" = "healthy" ]; do
    [ "$SECONDS" -lt "$limite" ] || echec "$service pas sain après ${ATTENTE_SANTE_SECONDES} s"
    sleep 2
  done
  echo "   $service : sain"
done
echo "Remise à zéro terminée : les 8 bases sont à l'état de référence."
