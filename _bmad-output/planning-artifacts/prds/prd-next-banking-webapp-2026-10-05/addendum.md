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

Ce sont des paramètres de configuration de déploiement, avec les valeurs par défaut du code legacy : `SEUIL_FRAUDE_EUR` = 10 000, `SEUIL_ALERTE_EUR` = 5 000, `SEUIL_PENDING_EUR` = 5 000. L'administrateur ne peut pas les modifier depuis l'interface (réponse Q6). La décision RG-027-003 (D2, D10) ajoute deux paramètres de même nature : le seuil de score de suspension (75) et le plafond du cumul glissant (10 000 €). Tous ces paramètres sont validés au démarrage (NFR-SEC-08).

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

## A.7 Décision RG-027-003 : options écartées et conséquences techniques

Source : la table ronde du 2026-10-05, transcript `_bmad-output/party-rg-027-003-script.md`, version de présentation `_bmad-output/party-mode/party-rg-027-003-2026-10-05.html`.

**Options examinées :**

| Option | (a) suspicion | (b) erreur du contrôle | Pourquoi elle n'est pas retenue |
|---|---|---|---|
| P1 prudente | Score à seuils, suspension, cumul dès le socle | Suspension, quel que soit le montant | Chaque panne alimente la file de l'administrateur ; panne et suspicion se confondent. |
| **P2 équilibrée (retenue)** | Score à seuils, suspension avant débit | Refus pur, état distinct | — |
| P3 minimale | Suspension seulement à partir de 10 000 € | Refus | Laisse passer tout virement sous 10 000 €, même au score élevé, et le découpage. |

Une variante de John (suspendre à partir de 5 000 € en cas d'erreur) est écartée : en cas d'erreur, c'est un refus pur.

**Conséquences pour `bmad-architecture` :**
- La décision est **synchrone et locale** au service de virement. Le score est un calcul du domaine, derrière un port, avec un adaptateur simulé. Il n'y a pas d'appel réseau, donc pas de délai métier.
- **Machine d'états** du virement : EXÉCUTÉ, SUSPENDU, LIBÉRÉ, REJETÉ, REFUSÉ_ERREUR_CONTRÔLE (FR-027-16).
- **Événements** publiés sur Kafka après la décision (outbox), vers les notifications et la file de revue. L'unique notification d'un virement est produite à partir de l'événement de décision du contrôle, et non par le traitement asynchrone du virement exécuté (FR-002-10, FR-023-03). La remise d'un virement exécuté au traitement asynchrone est enregistrée dans la même écriture que sa validation, puis publiée : son échec fait échouer la validation (FR-023-02, A-023-3).
- **Commandes** Libérer et Rejeter, réservées à l'administrateur (FR-016-19). La libération refait le contrôle du solde. Dans la même action, les alertes encore ouvertes du virement passent à REVIEWED (décision de finalisation B4).
- **Traitement planifié** du rejet automatique à 24 heures (FR-027-17), testé avec une horloge injectée. L'heure butoir fait foi (décision de finalisation B5) : la commande Libérer compare l'horloge à l'heure butoir et refuse à l'heure butoir ou après, que le traitement planifié soit passé ou non ; la concurrence entre libération et expiration se tranche par l'horloge.
- **Virements programmés** : chaque échéance appelle le même cas d'usage de contrôle que le virement immédiat (FR-021-10, décision de finalisation B2).
- **À fixer dans la décision d'architecture :**
  - la valeur de l'expiration technique de l'accès aux données d'historique (NFR-SEC-09) ;
  - le moyen d'observer le contrôle dans la trace sans créer de span à la main (NFR-OBS-02).
- **Golden master** : SC-003-01, SC-003-02 et SC-003-04 divergent du legacy (contrôle avant le virement, erreur non avalée, avertissement remplacé). Il faut les reprendre avec une exception documentée, ou les écarter avec justification, à l'étape G2.

## A.8 Mesures de référence du legacy (pas des cibles)

Elles viennent du brief et de `docs/retro/00-global/NFR.md`, et ont été mesurées sur l'environnement de démonstration du legacy.

**Volumétrie** (données de référence `import.sql`) :
- transactions 45, loan_repayments 18, bank_products 13, watchlist_items 9 ;
- customers 7, bank_accounts 7, notifications 6, fraud_alerts 6, budgets 6 ;
- investment_products 5, external_beneficiaries 5, documents 5 ;
- savings_products 4, risk_profiles 4, credit_scores 4, credit_products 4 ;
- recurring_transfers 3, loans 2.

**Temps de réponse** (127 requêtes capturées, en local) :
- minimum 1 ms, moyenne 22 ms, maximum 1 217 ms ;
- page bourse : 1 217 ms au chargement initial, puis 469 ms et 303 ms (appel AlphaVantage) ;
- administration 48 ms, bénéficiaires 38 ms, budget 32 ms, relevé 32 ms.

**Traitements planifiés du legacy** (horaires repris par NFR-BATCH-01) :

| Nom legacy | Horaire | Fonctionnalité |
|---|---|---|
| INTEREST_CALCULATION | tous les jours à 02h00 | FEAT-018 |
| FRAUD_DETECTION | tous les jours à 02h30 | FEAT-019 |
| MONTHLY_STATEMENTS | le 1er du mois à 03h00 | FEAT-020 |
| RECURRING_TRANSFERS | toutes les 5 minutes | FEAT-021 |
| NOTIFICATION_CHECK | toutes les 30 minutes | FEAT-022 |

**Constats de sécurité du legacy et exigences qui les traitent :**

| Constat | Gravité | Traité par |
|---|---|---|
| Session fixation non corrigée | HAUTE | NFR-SEC-02 |
| IDOR sur le téléchargement de documents | HAUTE | NFR-SEC-03 |
| IDOR sur les virements programmés | HAUTE | NFR-SEC-03 |
| Pile d'appels Java exposée dans un message d'erreur | MOYENNE | NFR-SEC-04 |
| Erreurs de champs masquées en production | FAIBLE | NFR-SEC-04 |
| Journaux sans masquage de l'IBAN et du montant | FAIBLE | NFR-SEC-05 (IBAN seul, risque R-3) |
