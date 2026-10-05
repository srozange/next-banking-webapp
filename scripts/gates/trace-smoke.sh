#!/usr/bin/env bash
# Prouve qu'une trace du flux témoin traverse deux services, avec un span JDBC, un span d'émission Kafka
# et un span de consommation Kafka sous le même identifiant de trace (lu dans Jaeger). Aucune appréciation.
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"
fail=0
ok() { echo "OK  $*"; }
ko() { echo "KO  $*"; fail=1; }

compose=(docker compose -f compose.yaml)
web=http://localhost:8080
jaeger=http://localhost:16686
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT

for outil in docker curl jq; do
  command -v "$outil" >/dev/null 2>&1 || { ko "outil manquant : $outil"; exit 1; }
done

# Pile démarrée = au moins un conteneur du projet en marche ; sinon on la démarre.
if [ -z "$("${compose[@]}" ps -q --status running 2>/dev/null)" ]; then
  [ -f .env ] || make secrets >/dev/null || { ko "make secrets a échoué"; exit 1; }
  echo "..  pile arrêtée : docker compose up -d --build --wait"
  "${compose[@]}" up -d --build --wait >"$tmp/up.log" 2>&1 \
    || { tail -20 "$tmp/up.log"; ko "la pile ne démarre pas"; exit 1; }
else
  echo "..  pile déjà démarrée : images non reconstruites"
fi

# Le POST ouvre lui-même sa trace (en-tête W3C traceparent, échantillonnée) : on lit ensuite cette trace-là,
# et aucune autre, dans Jaeger.
id=$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')
parent=$(od -An -N8 -tx1 /dev/urandom | tr -d ' \n')
code=$(curl -s -o "$tmp/reponse.json" -w '%{http_code}' --max-time 30 \
  -X POST "$web/api/coeur-bancaire/temoin" -H 'Content-Type: application/json' \
  -H "traceparent: 00-$id-$parent-01" -d '{"libelle":"trace-smoke"}')
if [ "$code" = "201" ] && jq -e '.temoinId > 0' "$tmp/reponse.json" >/dev/null 2>&1; then
  ok "POST /api/coeur-bancaire/temoin par web : 201, temoinId $(jq -r .temoinId "$tmp/reponse.json"), trace $id"
else
  ko "POST /api/coeur-bancaire/temoin par web : statut $code"; exit 1
fi

# Bilan de la trace (API v3 de Jaeger, format OTLP) : {post, services, jdbc, emission, consommation, score}.
# Genre de span OTLP : 4 = producteur (émission), 5 = consommateur (en entier ou en nom d'énumération).
bilan='
  [ .result.resourceSpans[]?
    | (.resource.attributes[]? | select(.key == "service.name") | .value.stringValue) as $service
    | .scopeSpans[]?.spans[]?
    | { service: $service, nom: .name, genre: (.kind | tostring),
        jdbc: any(.attributes[]?; .key | startswith("jdbc.")) } ]
  | { post: any(.[]; .nom == "http post /api/coeur-bancaire/temoin"),
      services: (map(.service) | unique | length),
      jdbc: any(.[]; .jdbc),
      emission: any(.[]; .genre == "4" or .genre == "SPAN_KIND_PRODUCER"),
      consommation: any(.[]; .genre == "5" or .genre == "SPAN_KIND_CONSUMER") }
  | . + { score: ((if .services >= 2 then 1 else 0 end) + (if .jdbc then 1 else 0 end)
                  + (if .emission then 1 else 0 end) + (if .consommation then 1 else 0 end)) }'

trace=""
for _ in $(seq 1 30); do
  if curl -sf --max-time 10 -o "$tmp/trace.json" "$jaeger/api/v3/traces/$id" \
     && jq -e '.result' "$tmp/trace.json" >/dev/null 2>&1; then
    trace=$(jq -c "$bilan" "$tmp/trace.json")
    [ "$(jq -r .score <<<"$trace")" = "4" ] && break
  fi
  sleep 2
done

if [ -z "$trace" ] || [ "$(jq -r .post <<<"$trace")" != true ]; then
  ko "trace $id du POST témoin absente de Jaeger ($jaeger) après 30 essais espacés de 2 s"
  exit 1
fi
ok "trace $id du POST témoin trouvée dans Jaeger"
n=$(jq -r .services <<<"$trace")
if [ "$n" -ge 2 ]; then ok "trace $id : $n services distincts"; else ko "trace $id : $n service(s), deux attendus"; fi
if [ "$(jq -r .jdbc <<<"$trace")" = true ]; then ok "trace $id : span JDBC"; else ko "trace $id : aucun span JDBC (balise jdbc.*)"; fi
if [ "$(jq -r .emission <<<"$trace")" = true ]; then ok "trace $id : span d'émission Kafka"; else ko "trace $id : aucun span d'émission Kafka (span.kind=producer)"; fi
if [ "$(jq -r .consommation <<<"$trace")" = true ]; then ok "trace $id : span de consommation Kafka"; else ko "trace $id : aucun span de consommation Kafka (span.kind=consumer)"; fi

exit $fail
