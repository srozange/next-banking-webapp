#!/usr/bin/env bash
# Génère les projections de référence des services consommateurs depuis les données de référence de leur producteur :
# la projection des clients de credits (services/credits/reference-data/projection-clients.sql) est dérivée des
# utilisateurs de services/identite-clients/reference-data/reference-data.sql (identifiant, login, nom complet, rôle,
# actif, conseiller). Chaque client est en version 1, non supprimé ; date_creation est la date de référence posée à la
# migration d'identite-clients pour les clients existants (2026-10-06), identique d'une génération à l'autre.
# Aucun mot de passe n'est lu ni recopié. Le fichier produit n'est jamais écrit à la main.
# Usage : scripts/generer-projections-reference.sh
set -euo pipefail
racine="$(cd "$(dirname "$0")/.." && pwd)"
source_sql="$racine/services/identite-clients/reference-data/reference-data.sql"
sortie="$racine/services/credits/reference-data/projection-clients.sql"

python3 -I - "$source_sql" "$sortie" <<'PYTHON'
import re
import sys

source, sortie = sys.argv[1], sys.argv[2]
DATE_CREATION = "2026-10-06"
JETON = re.compile(r"'(?:[^']|'')*'|date\s+'[^']*'|[^,\s()]+")
INSERT = re.compile(
    r"insert into customers \(([^)]*)\)\s*values \((.*)\);\s*$", re.IGNORECASE | re.DOTALL)

# Une instruction par paire de lignes : « insert into customers (...) » puis « values (...); ».
contenu = re.sub(r"\)\s*\nvalues", ") values", open(source, encoding="utf-8").read())
lignes = []
for instruction in contenu.splitlines():
    correspondance = INSERT.match(instruction.strip())
    if not correspondance:
        continue
    colonnes = [c.strip() for c in correspondance.group(1).split(",")]
    valeurs = JETON.findall(correspondance.group(2))
    if len(colonnes) != len(valeurs):
        sys.exit("reference-data.sql : colonnes et valeurs ne correspondent pas : " + instruction)
    ligne = dict(zip(colonnes, valeurs))
    lignes.append((ligne["id"], ligne["username"], ligne["full_name"], ligne["role"], ligne["active"],
                   ligne["advisor_id"]))

if not lignes:
    sys.exit("reference-data.sql : aucun utilisateur trouvé")

entete = (
    "-- Projection de référence des clients du service credits : un client par utilisateur du legacy, en version 1.\n"
    "-- Hors migrations : chargé dans une base déjà migrée par Flyway.\n"
    "-- Généré par scripts/generer-projections-reference.sh depuis services/identite-clients/reference-data/"
    "reference-data.sql : ne pas modifier à la main.\n")
corps = "".join(
    "insert into clients (client_id, login, nom_complet, role, actif, conseiller_id, date_creation, supprime,"
    " version_agregat)\n"
    f"values ({id_}, {login}, {nom}, {role}, {actif}, {conseiller}, date '{DATE_CREATION}', false, 1);\n"
    for id_, login, nom, role, actif, conseiller in lignes)
with open(sortie, "w", encoding="utf-8") as fichier:
    fichier.write(entete + corps)
PYTHON
echo "Projection des clients de credits générée : $sortie"
