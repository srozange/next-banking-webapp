# Addendum au PRD : next-banking-webapp (rénovation)

Contenu technique ou de détail qui n'a pas sa place dans le PRD, mais que les étapes suivantes (architecture, epics, développement) doivent reprendre.

## A.1 Architecture cible : contrainte imposée

L'architecture ci-dessous est **imposée**. Ce n'est pas une option à discuter. Elle reprend la section « Architecture » de `docs/quality-gates.md`, la référence unique. En cas d'écart, c'est `docs/quality-gates.md` qui fait foi.

- **Microservices :** Java 21, Spring Boot 4, Maven. Un module par service sous `services/<nom>/`, POM parent à la racine.
- **Hexagone back :** chaque service est hexagonal, avec les packages `domain`, `application`, `adapter.in`, `adapter.out`, `infrastructure`.
  - `domain` et `application` sont du Java pur : seules dépendances autorisées, `java.*` et le code du service.
  - Aucun import Spring, Jakarta, Jackson ni autre bibliothèque ; aucune dépendance vers `adapter` ou `infrastructure`.
- **Données :** une base PostgreSQL par service. Aucun service ne lit la base d'un autre.
- **Bus entre services :** Kafka.
- **Front :** React + TypeScript sous `frontend/`, hexagonal : `src/domain`, `src/application`, `src/adapters`, `src/infrastructure`.
  - `src/domain` et `src/application` sont du TypeScript pur : aucun import de paquet npm (ni React, ni bibliothèque d'état), hors fichiers de test.
  - Tout ce qui est React vit dans `src/adapters`.
- **Tracing :** `spring-boot-starter-opentelemetry`, export OTLP vers un collecteur OpenTelemetry, backend Jaeger v2.
  - Aucun span créé à la main. Spans JDBC via datasource-micrometer 2.x.
  - Contexte propagé sur HTTP et sur Kafka (observation activée sur KafkaTemplate et sur les listeners).
  - Échantillonnage à 1.0 en local.
- **Lancement local :** `compose.yaml` à la racine construit les images et lance les services, PostgreSQL, Kafka, le collecteur et Jaeger.

Ce que la nouvelle architecture remplace dans le legacy :
- les EJB et la messagerie JMS interne (ActiveMQ Artemis) : les traitements asynchrones des virements et de l'approbation des prêts (FEAT-023, FEAT-024) passent par Kafka ;
- les timers `@Schedule` : ils deviennent des traitements planifiés des services (NFR-BATCH-01) ;
- l'absence d'API : le front passe désormais par une API interne. [ASSUMPTION A-G-05]

## A.2 Fournisseurs externes : ports et adaptateurs simulés

Décision de cadrage (réponse Q5) :

| Fournisseur | Fonctionnalités | Port | Adaptateur par défaut | Vrai appel |
|---|---|---|---|---|
| Azure OpenAI | FEAT-010 | chat conseiller | simulé (réponses déterministes) | option de configuration (endpoint, deployment, clé) |
| Powens | FEAT-012, FEAT-014, FEAT-028 | open banking | simulé (webview, callback et comptes simulés) | option de configuration (domain, client_id, secret, connector_id) |
| AlphaVantage | FEAT-009, FEAT-029 | cotations | simulé (séries fixes) | option de configuration (clé), avec cache |

- Aucun test ni aucun contrôle ne dépend du réseau ni d'une clé externe.
- Les vrais appels ont un délai d'attente et passent par un coupe-circuit (NFR-INT-02).
- Les adaptateurs simulés servent aussi au rejeu du golden master : les scénarios des fonctionnalités qui appellent un fournisseur externe se rejouent contre eux. [ASSUMPTION A-G-09 : la décision de rejeu par scénario est prise en architecture, étape G2]

## A.3 Seuils de fraude et d'alerte

Ce sont des paramètres de configuration de déploiement, avec les valeurs par défaut du code legacy : `SEUIL_FRAUDE_EUR` = 10 000, `SEUIL_ALERTE_EUR` = 5 000, `SEUIL_PENDING_EUR` = 5 000. L'administrateur ne peut pas les modifier depuis l'interface (réponse Q6). Ce choix ne dit rien de la question ouverte QO-1 (RG-027-003).

## A.4 Bugs connus conservés

Les bugs du legacy décidés « ne pas corriger » ne sont ni des exigences ni des tests. Ils sont tous listés, avec leur raison, dans `docs/retro/00-global/LEGACY-BUGS.md` (décision « Ne pas corriger »). L'équipe doit savoir qu'il est normal de les rencontrer dans le comportement repris.

## A.5 Golden master

- Le contrat de dérivation est `docs/retro/golden-master-contract.md`. Les décisions par scénario (`[ASK ARCHITECT]`) se prennent pendant `bmad-architecture`, avec le skill `corpus-bmad-golden-master` (étape G2).
- Décisions globales à prendre en architecture :
  - URL de base et mode d'authentification de la cible ;
  - sortie qui fait foi pour une fonctionnalité d'interface ;
  - table des routes ;
  - sessions de test par rôle ;
  - jeu de données de référence et son chargement (NFR-PAR-04).
- Rejeu : une seule fois, développement terminé (NFR-PAR-01).

## A.6 Traçabilité de `docs/quality-gates.md` vers le PRD

Chaque ligne de `docs/quality-gates.md` et l'endroit où elle est reprise :

| Ligne(s) de `docs/quality-gates.md` | Sujet | Reprise |
|---|---|---|
| 4 | Microservices Java 21, Spring Boot 4, Maven, `services/<nom>/`, POM parent | Addendum A.1 |
| 5–7 | Hexagone back, Java pur dans domain et application | Addendum A.1 |
| 8 | Une base PostgreSQL par service | Addendum A.1 |
| 9 | Bus Kafka | Addendum A.1 |
| 10–12 | Front React TS hexagonal, TS pur, React dans `src/adapters` | Addendum A.1 |
| 13–16 | Tracing OpenTelemetry, Jaeger v2, pas de span manuel, datasource-micrometer, propagation HTTP et Kafka, échantillonnage 1.0 | Addendum A.1 ; PRD NFR-OBS-01 |
| 17 | `compose.yaml` | Addendum A.1 |
| 19 | Contrôles exécutés par des outils | PRD §7.1 (en-tête) |
| 20–21 | ArchUnit | PRD NFR-QG-01 |
| 22–23 | dependency-cruiser | PRD NFR-QG-02 |
| 24–25 | JaCoCo 100 / 80 | PRD NFR-QG-03 |
| 26 | Vitest, mêmes seuils | PRD NFR-QG-04 |
| 27–28 | PIT 85, historique | PRD NFR-QG-05 |
| 29–30 | StrykerJS 85 / 90, incremental | PRD NFR-QG-06 |
| 31–32 | Dependency-Check, CVSS 7, NVD, OSS Index et Node Audit désactivés, base non mise à jour pendant les scans | PRD NFR-QG-07 |
| 33–34 | Trivy HIGH et CRITICAL, `--ignore-unfixed`, journal dans `reports/` | PRD NFR-QG-08 |
| 35 | `trace-smoke.sh` | PRD NFR-QG-09 |
| 38 | Contrôle rouge corrigé dans le code ou les tests | PRD NFR-QG-10 |
| 39–41 | Fichiers interdits de modification | PRD NFR-QG-10 |
| 42 | Interdit d'abaisser un seuil, d'exclure, de désactiver | PRD NFR-QG-10 |
| 43 | Seuil inatteignable : s'arrêter | PRD NFR-QG-10 |

Les lignes 1, 2, 3, 18, 36 et 37 sont des titres ou des lignes vides.
