# Réconciliation : docs/quality-gates.md contre le PRD et l'addendum

Entrée : `docs/quality-gates.md` (43 lignes, référence unique de l'architecture cible et des contrôles).
Cibles lues : `prd.md` (§7.1, NFR-QG-00 à NFR-QG-10, §7.7 NFR-OBS, SM-4, §6.1, liste des hypothèses) et `addendum.md` (A.1, A.6, A.7). Ni l'un ni l'autre n'a été modifié.

Légende : fidèle = reprise mot à mot du sens, du seuil et du moment ; reformulée = sens ou formulation changé ; ajout = le PRD dit plus que la référence ; absente = rien dans le PRD ni l'addendum.

## 1. Lignes vides ou titres

| Ligne | Contenu | Statut |
|---|---|---|
| 1, 2, 3, 18, 36, 37 | Titres et lignes vides | sans objet (A.6 le dit) |

## 2. Architecture (lignes 4 à 17), reprise dans addendum A.1

| Ligne | Contenu | Où | Statut |
|---|---|---|---|
| 4 | Microservices Java 21, Spring Boot 4, Maven, `services/<nom>/`, POM parent racine | A.1 « Microservices » | fidèle |
| 5 | Hexagone back : domain, application, adapter.in, adapter.out, infrastructure | A.1 « Hexagone back » | fidèle |
| 6 | domain et application Java pur ; seules dépendances `java.*` et le code du service | A.1, 1re sous-puce | fidèle |
| 7 | Aucun import Spring, Jakarta, Jackson ni autre bibliothèque ; aucune dépendance vers adapter ou infrastructure | A.1, 2e sous-puce | fidèle |
| 8 | Une base PostgreSQL par service, aucune lecture de la base d'un autre | A.1 « Données » | fidèle |
| 9 | Bus Kafka | A.1 « Bus entre services » | fidèle |
| 10 | Front React + TypeScript sous `frontend/`, hexagonal, 4 dossiers `src/` | A.1 « Front » | fidèle |
| 11 | `src/domain` et `src/application` en TypeScript pur, aucun paquet npm (ni React, ni bibliothèque d'état), hors fichiers de test | A.1, 1re sous-puce Front | fidèle |
| 12 | Tout ce qui est React vit dans `src/adapters` | A.1, 2e sous-puce Front | fidèle |
| 13 | `spring-boot-starter-opentelemetry`, OTLP vers collecteur, backend Jaeger v2 | A.1 « Tracing » | fidèle |
| 14 | Aucun span créé à la main ; spans JDBC via datasource-micrometer 2.x | A.1 « Tracing », puce 1 | fidèle |
| 15 | Contexte propagé sur HTTP et Kafka (observation sur KafkaTemplate et listeners) | A.1 « Tracing », puce 2 | fidèle |
| 16 | Échantillonnage à 1.0 en local | A.1 « Tracing », puce 3 | fidèle |
| 17 | `compose.yaml` racine : images, services, PostgreSQL, Kafka, collecteur, Jaeger | A.1 « Lancement local » | fidèle |

L'addendum A.1 déclare l'architecture « imposée », et que `docs/quality-gates.md` fait foi en cas d'écart. Aucun élément d'architecture omis. Il ajoute un bloc « Ce que la nouvelle architecture remplace » (EJB, JMS, `@Schedule`, API interne) qui n'est pas dans la référence ; l'hypothèse A-G-05 est signalée pour l'API interne, les deux autres puces ne le sont pas (ajouts d'origine corpus legacy, sans conflit).

## 3. Contrôles (lignes 19 à 35)

| Ligne | Contenu | Où | Statut |
|---|---|---|---|
| 19 | Contrôles exécutés par des outils, jamais évalués par un agent | PRD §7.1, 1re phrase | fidèle |
| 20–21 | ArchUnit `*ArchitectureTest` dans chaque service ; 2 règles (sens des dépendances, aucun framework dans domain et application) ; bloquant à chaque build | NFR-QG-01 | fidèle (outil, seuil, moment) |
| 22–23 | dependency-cruiser, `frontend/.dependency-cruiser.cjs`, sévérité error, sens des dépendances, aucun paquet npm dans domain et application ; bloquant | NFR-QG-02 | fidèle. Le PRD précise `src/domain` et `src/application` (déjà dans la ligne 11). |
| 24–25 | JaCoCo check, phase verify ; 100 % lignes et branches domain et application ; 80 % adapter et infrastructure ; bloquant | NFR-QG-03 | fidèle |
| 26 | Vitest `coverage.thresholds` par glob, mêmes seuils ; bloquant | NFR-QG-04 | fidèle sur l'outil et « mêmes seuils » ; **ajout** : le PRD détaille les seuils par dossier (100 % sur domain et application, 80 % sur `src/adapters` et `src/infrastructure`). C'est une déduction exacte de « mêmes seuils » mais ce n'est pas écrit dans la référence. |
| 27–28 | PIT, profil Maven `mutation`, domain et application, `mutationThreshold` 85, historique activé ; à la demande | NFR-QG-05 | fidèle |
| 29–30 | StrykerJS sur `src/domain` et `src/application`, break 85 / high 90, incremental ; à la demande | NFR-QG-06 | fidèle |
| 31–32 | Dependency-Check, profil `security`, `failBuildOnCVSS` 7, clé NVD via `settings.xml`, OSS Index et Node Audit désactivés, mise à jour de la base désactivée pendant les scans | NFR-QG-07 | outil, seuil, configuration fidèles ; **moment inventé** : « à l'exécution du profil `security` » n'est pas dans la référence (qui ne donne aucun moment) et n'est pas marqué comme hypothèse, contrairement à NFR-QG-09. |
| 33–34 | Trivy fs, HIGH et CRITICAL ; porte bloquante avec `--ignore-unfixed` ; sans correctif journalisé dans `reports/`, sans bloquer | NFR-QG-08 | fidèle |
| 35 | `trace-smoke.sh` prouve qu'une trace traverse deux services avec un span JDBC et un span Kafka | NFR-QG-09 | outil et seuil fidèles ; **moment ajouté** (« avant de déclarer terminée toute story qui touche un échange entre services »), correctement marqué A-G-03, repris dans la liste des hypothèses (ligne 2878) et dans l'en-tête §7.1. |

## 4. Règles pour les agents (lignes 38 à 43), reprises dans NFR-QG-10

| Ligne | Contenu | Où | Statut |
|---|---|---|---|
| 38 | Contrôle rouge corrigé dans le code ou les tests, jamais en modifiant le contrôle | NFR-QG-10, puce 1 | fidèle |
| 39–41 | Neuf fichiers interdits : `scripts/gates/**`, `.bmad-loop/policy.toml`, `.bmad-loop/gates.sha256`, `.claude/settings.json`, `.claude/hooks/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml` | NFR-QG-10, puce 2 | fidèle (les 9 chemins présents, dans le même ordre) |
| 42 | Interdit d'abaisser un seuil, d'ajouter une exclusion de couverture ou de mutation, de désactiver un test | NFR-QG-10, puce 3 | fidèle |
| 43 | Seuil inatteignable : s'arrêter et le signaler, ne pas contourner | NFR-QG-10, puce 4 | fidèle |

## 5. Lignes absentes

Aucune. Chaque ligne non vide de `docs/quality-gates.md` a une reprise ; la table A.6 de l'addendum est cohérente avec ce constat (lignes 4 à 17, 19 à 35, 38 à 43).

## 6. NFR-QG-00 (exigence de l'utilisateur, absente de la référence)

Présente (« Les contrôles sont livrés et verts avant toute story fonctionnelle »), reprise aussi en §6.1 et en SM-4. Rien à redire sur la lettre.

## 7. Cohérence « aucun span créé à la main » et NFR-OBS

- NFR-OBS-01 renvoie à A.1 et à la preuve NFR-QG-09 : cohérent.
- NFR-OBS-02 (trace du virement montrant le contrôle de fraude, son issue, un statut d'erreur, et l'identifiant de trace affiché au client) **ne contredit pas** la règle : il la cite explicitement et renvoie le moyen à l'architecture. A.7 reprend la même charge (« sans créer de span à la main »).
- Risque résiduel, non contradiction : la règle interdit le span manuel, mais NFR-OBS-02 exige un élément visible dans la trace pour un contrôle qui vit dans `domain` et `application`, où aucune bibliothèque n'est permise (donc ni API d'observation ni OpenTelemetry). Le moyen devra passer par un adaptateur (décorateur du port de contrôle de fraude en `adapter.out` ou `infrastructure`) et il faudra trancher si une `Observation` Micrometer ou `@Observed` compte comme « créé à la main ». De même, l'identifiant de trace affiché au client (FR-003-27) doit être exposé à la couche application par un port. Aucun texte ne précise ces deux points ; ils sont bien listés comme « à fixer » en A.7.

## 8. Écarts principaux

1. NFR-QG-07 : moment « à l'exécution du profil `security` » inventé et non signalé comme hypothèse (la référence n'en donne pas). Soit le marquer comme hypothèse (comme A-G-03), soit écrire « Moment : non précisé par la référence ».
2. NFR-QG-04 : le détail des seuils par dossier est un ajout (déduction de « mêmes seuils »). Acceptable, mais à marquer ou à remplacer par la formule exacte « mêmes seuils que NFR-QG-03 ».
3. NFR-QG-09 : moment ajouté (signalé A-G-03). Tension avec NFR-QG-00, qui exige que NFR-QG-01 à NFR-QG-09 soient verts avant toute story fonctionnelle : avant la première story, il n'existe ni deux services, ni échange Kafka, ni span JDBC, donc `trace-smoke.sh` et ArchUnit « dans chaque service » ne peuvent pas être verts de façon significative. À clarifier (par exemple un service d'exemple de socle, ou une activation progressive).
4. NFR-QG-00 englobe NFR-QG-05, 06, 07, qui sont « à la demande » ou sans moment ; SM-4 dit « contrôles bloquants » mais valide QG-00 à QG-09, ce qui inclut des contrôles non bloquants. Ambiguïté de portée entre « livrés » et « exécutés verts ».
5. NFR-OBS-02 : voir §7, pas de contradiction mais un moyen non tranché, contraint par l'hexagone pur.
