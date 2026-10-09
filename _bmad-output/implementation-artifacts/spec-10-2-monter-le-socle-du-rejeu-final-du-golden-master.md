---
title: 'Monter le socle du rejeu final du golden master'
type: 'feature'
created: '2026-10-09'
status: 'done'
baseline_revision: '679ab804a01f8f4ee740d70e2f9dbfc8a844a690'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-10-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/golden-master-architecture.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problem:** Le golden master ne peut pas encore être rejoué de façon reproductible. Le profil `rejeu` ne couvre que le cœur bancaire, les données de référence sont incomplètes (2 comptes au lieu de 7, aucune opération) et aucune commande ne les charge. Il n'existe ni `make reset-data`, ni `make rejeu-up`, ni runner Playwright installé (DW-7, DW-12, DW-14, DW-15, DW-18, DW-21, DW-30).

**Approach:**
- Monter le banc de rejeu de golden-master-architecture.md §5 et §7 :
  - profil `rejeu` sur les 8 services, avec une horloge réglée par un décalage unique ;
  - données de référence fidèles à l'`import.sql` du legacy (153 lignes), rechargées par une remise à zéro idempotente ;
  - Playwright Test installé dans `e2e/golden-master` ;
  - script de rejeu ordonné.
- Prouver ensuite le déterminisme de SC-003-04 : trois rejeux verts, puis une contre-épreuve à 23:30 qui doit échouer.

## Boundaries & Constraints

**Always:**
- **Source des données.** Les lignes viennent de l'`import.sql` du legacy : `/home/sebastienrozange/workspace/acn-fs/AppModernizationDemonstrateur/legacy/src/main/resources/import.sql`, hors du dépôt, en lecture seule.
  - Identifiants, valeurs et dates sont repris tels quels. Une date sans fuseau est lue à l'heure de Paris (`timestamptz '… Europe/Paris'`).
  - Les soldes sont ceux de l'import, même incohérents avec les opérations : compte 1 = 2450.00, compte 2 = 15000.00.
  - On réaligne sur l'import les identifiants de la watchlist (TTE.PA=1 … GOOGL=9) et des produits (Livret A=1 … Private Equity=13). La représentation cible des taux en fractions reste inchangée.
  - Chaque table cible garde son schéma. Une colonne legacy sans équivalent cible est perdue, et le commentaire d'en-tête du fichier le dit.
- **Comptages après `make reset-data`** (153 lignes en tout) :

  | Service | Table | Lignes |
  |---|---|---|
  | identite-clients | `customers` | 7 |
  | coeur-bancaire | `comptes` | 7 |
  | coeur-bancaire | `operations` | 45 |
  | coeur-bancaire | `virements` | 0 |
  | coeur-bancaire | `virements_programmes` | 3 |
  | coeur-bancaire | `beneficiaires_externes` | 5 |
  | coeur-bancaire | `alertes_fraude` | 6 |
  | coeur-bancaire | `plafonds_budget` | 6 |
  | credits | `loans` | 2 |
  | credits | `loan_repayments` | 18 |
  | credits | `credit_scores` | 4 |
  | credits | `risk_profiles` | 4 |
  | documents | `documents` | 5 |
  | notifications | `notifications` | 6 |
  | audit | `audit_logs` | 0 |
  | bourse | `watchlist_items` | 9 |
  | open-banking-conseiller | `bank_products` | 13 |
  | open-banking-conseiller | `savings_products` | 4 |
  | open-banking-conseiller | `credit_products` | 4 |
  | open-banking-conseiller | `investment_products` | 5 |

  Les projections sont en plus, toujours générées par `scripts/generer-projections-reference.sh`.
- **Profil `rejeu`.** Il est actif sur les 8 services via `compose.rejeu.yaml`.
  - `HORLOGE_DECALAGE` est passé aux 8 services.
  - Chaque service doté du port `Horloge` a un adaptateur `HorlogeReglee` en `@Profile("rejeu")` et une `HorlogeSysteme` en `@Profile("!rejeu")`, sur le modèle du cœur bancaire. L'audit n'a pas d'horloge.
  - `nb.fournisseurs.{alphavantage,azure-openai,powens}.mode` sont forcés à `simule` par des variables `NB_FOURNISSEURS_*_MODE` littérales. Le jeton dure 60 minutes.
- **`make rejeu-up`** calcule un seul décalage : `HORLOGE_DECALAGE` = `HORLOGE_DEPART` (Europe/Paris, par défaut `2026-11-17T10:00:00`) − maintenant. Il l'écrit en durée ISO-8601 en secondes (`PT<n>S`, `n` pouvant être négatif).
- **`make reset-data`** n'est jamais une recréation de conteneurs. Il arrête puis relance les conteneurs existants, ce qui garde leur environnement, donc l'horloge.
- **Golden master.** Seules corrections autorisées : `replay-mapping.json`, suivi de `derive … --force` et de `export-golden-master-tests.py`, chaque correction étant inscrite dans `ecarts-voulus-golden-master.md` (scénario, ce qui change, raison).
  - On peut créer `e2e/golden-master/package.json` et `package-lock.json`, avec `@playwright/test` en version exacte `1.63.0`.
- **Secrets.** Les mots de passe ne sont jamais affichés ni écrits en clair. Les empreintes sont régénérées depuis les `RETRO_*_PASSWORD` de `.env`.
- **Tests.** Les tests nouveaux back portent `@Tag("NFR-PAR-04")` (données) ou `@Tag("NFR-PAR-05")` (horloge et profil).
  - Couverture : 100 % sur domain et application, 80 % sur adapter.
  - Les tests existants qui figent les anciennes données de référence sont mis à jour vers les valeurs de l'import, jamais désactivés.

**Never:**
- Éditer à la main `e2e/golden-master/**` (hors `package.json` et le fichier de verrouillage), `docs/retro/golden-master/derived/**`, ou les tests générés.
- Retirer un scénario ou une ligne attendue.
- Toucher les fichiers interdits de `docs/quality-gates.md` (`scripts/gates/**`, etc.).
- Écrire `mode=reel` dans un fichier versionné.
- Écrire `now()`, `current_date` ou `CURRENT_TIMESTAMP` dans une migration ou une donnée de référence.
- Corriger ici les scénarios autres que SC-003-04 qui échoueraient au premier `npx playwright test`. C'est le rôle de la story 10.3 ; on les consigne seulement.
- Passer `golden-master-architecture.md` en `status: final` : c'est une décision humaine.
- Modifier `sprint-status.yaml`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Horloge réglée | profil `rejeu`, `HORLOGE_DECALAGE=PT3600S` | `instant()` = système + 1 h ; elle avance | — |
| Décalage absent ou illisible | profil `rejeu`, `HORLOGE_DECALAGE` vide ou `abc` | — | démarrage refusé, message qui nomme `HORLOGE_DECALAGE` |
| Hors profil | aucun profil | `HorlogeSysteme` seule active | — |
| Tableau de bord | `user1` après `make rejeu-up` par défaut | `aujourdhui` = `"2026-11-17"` | — |
| Reset répété | 3 × `make reset-data` | code 0 ; mêmes comptages, conformes au tableau | — |
| Reset après écriture | un virement, puis `make reset-data` | instantané des comptages identique à celui d'avant le virement | — |
| Reset en échec | `postgres-credits` arrêté | — | `make reset-data` et `make rejeu-golden-master` sortent en code non nul en nommant l'étape ; aucun lancement du runner après l'échec |
| Date de référence trop tôt | `HORLOGE_DEPART=2020-01-01T10:00:00` | — | `verifier-date-reference.sh` sort en code non nul en nommant la table, la colonne et la date |
| Connexion sans mot de passe | `RETRO_ADMIN_PASSWORD` absente | — | `npx playwright test --project=connexion` sort en code non nul avec « variable d'environnement RETRO_ADMIN_PASSWORD absente » |
| Contre-épreuve | `HORLOGE_DEPART=2026-11-17T23:30:00` | SC-003-04 échoue (UNUSUAL_HOURS) | commande en code non nul (échec attendu) |

</intent-contract>

## Code Map

**Horloge, profil et compose**
- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/adapter/out/horloge/HorlogeReglee.java` -- modèle à reprendre : `Clock.offset`, `lireDecalage`. Avec son test `src/test/.../adapter/out/horloge/HorlogeRegleeTest.java` (`ApplicationContextRunner` + profil `rejeu`) et `infrastructure/ProfilRejeuIT.java` (seuls les relais d'outbox restent planifiés).
- `services/{bourse,credits,documents,identite-clients,notifications,open-banking-conseiller}/src/main/java/fr/nextbanking/<pkg>/adapter/out/horloge/HorlogeSysteme.java` -- `@Component` sans profil. Ajouter `@Profile("!rejeu")` et une `HorlogeReglee` sœur. `AlphaVantageSimule` (bourse) date ses séries par l'horloge.
- `services/*/src/test/.../HorlogeArchitectureTest.java` -- exceptions `adapter.out.horloge` et `adapter.out.jeton` ; les nouvelles horloges doivent y rester.
- `services/identite-clients/src/main/resources/application-rejeu.properties` -- jeton de 60 minutes, déjà fait. `EmetteurJetonNimbus` utilise l'horloge système (voulu).
- `services/*/.../Configuration{AlphaVantage,AzureOpenAi,Powens}.java`, `Proprietes*.java` -- choix `simule`/`reel` par `nb.fournisseurs.*.mode`, pas par profil.
- `compose.rejeu.yaml` -- n'active `rejeu` que pour bourse et open-banking-conseiller ; à étendre aux 8 services.
- `compose.yaml` -- `HORLOGE_DECALAGE` n'existe que sur coeur-bancaire (l. ~257), `TZ` est déjà partout, healthchecks `/actuator/health`. Le conteneur web résout les services à chaque requête (`frontend/nginx.conf`), donc redémarrer un service ne casse pas le web.
- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/adapter/in/PointEntreeVirements.java:99-104,155` -- `TableauDeBordClient(comptes, soldeTotal, dernieresOperations)` sans `aujourdhui` ; cas d'usage dans `application/ConsulterTableauDeBordCasUsage.java:31`.

**Kafka**
- Topics : 12, tous déclarés par un bean `NewTopic` (3 partitions, 1 réplique) dans les `infrastructure/ConfigurationEvenements.java` de coeur-bancaire, identite-clients et credits.
- Consommateurs : coeur-bancaire, notifications, audit, documents, credits.
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` ; aucun endpoint ne permet de mettre les écouteurs en pause, d'où l'arrêt des conteneurs.

**Données de référence**
- `services/*/reference-data/reference-data.sql`, plus `projection-clients.sql` pour coeur-bancaire et credits -- fichiers chargés dans une base déjà migrée.
  - notifications et audit n'en ont pas encore.
  - Le bloc `-- DEBUT/FIN PROJECTIONS GENEREES` de `documents/reference-data/reference-data.sql` est écrit par le script.
- `scripts/generer-projections-reference.sh` -- lit les comptes sur une seule ligne, au format exact `insert into comptes (id, numero, type, solde, client_id) values (…);` ; une ligne qui n'a pas ce format est ignorée sans erreur. Les opérations doivent avoir les colonnes `id, compte_id, date_operation, description, type, montant, solde_apres`.
- `services/identite-clients/reference-data/generer-reference-data.sh` + `src/test/.../referencedata/GenerateurReferenceData.java` -- n'écrit pas `date_creation`, qui prend alors `default current_date` (migration V3). Ajouter `date '2026-10-06'`, la valeur des projections.
- `services/credits/reference-data/generer-reference-data.sh` + `src/test/.../referencedata/GenerateurReferenceData.java` -- prêts reconstitués avec des dates relatives : ils sont faux et seront remplacés par ceux de l'import.
- Migrations :
  - `services/notifications/src/main/resources/db/migration/V3__notifications_d_echeance.sql` : CHECK `(virement_id is null) <> (virement_programme_id is null)`, qui refuse les 6 notifications legacy sans origine. La lecture (`NotificationsJdbc`) n'en dépend pas, et le domaine `Notification` garde sa règle pour les écritures.
  - `services/credits/src/main/resources/db/migration/V1..V4` : il n'y a ni `credit_scores` ni `risk_profiles`.
- Tests qui lisent les données de référence, à mettre à jour :
  - `*/referencedata/ReferenceDataIT.java` (bourse, credits, documents, identite-clients, open-banking-conseiller) et `*/referencedata/ProjectionClientsReferenceIT.java` ;
  - coeur-bancaire : `adapter/DonneesDeReference.java`, `PortefeuilleIT`, `AlerteFraudeAdministrationIT`, `CoeurBancaireSecuriteIT` ;
  - `bourse/adapter/WatchlistIT`, `open-banking-conseiller/adapter/ProduitsIT`, `credits/adapter/{CreditsSecuriteIT,PortefeuilleHttpIT,OutilsHttp}`, `documents/adapter/OutilsHttp`.

**Golden master**
- `e2e/golden-master/playwright.config.ts`, `auth.setup.ts`, `specs/*.spec.ts`, `.gitignore` -- générés, intouchables. Projets `connexion` puis `rejeu`, `workers: 1`, reporter list + html (`report/`) + json (`results.json`). Les scénarios qui écrivent portent le tag `@ecriture`.
- `docs/retro/golden-master/derived/*/*/scenario.json` -- champ `writes` (aujourd'hui SC-001-03, SC-001-05, SC-003-04, SC-004-02, SC-013-02).
- `scripts/gates/golden-master.sh` -- dérive dans un dossier temporaire, puis `diff -rq` avec le dérivé, puis `export … --check`. Lecture seule.
- `scripts/golden-master/verifier-scenario.sh` -- exemple de script du dossier `scripts/golden-master/`.
- `_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/golden-master-architecture.md` §5.3, §7.3, §8 points 9 et 10 -- à régler.

## Tasks & Acceptance

**Execution:**

*Horloge, profil et compose*
- `services/{bourse,credits,documents,identite-clients,notifications,open-banking-conseiller}/src/main/java/.../adapter/out/horloge/HorlogeReglee.java` (+ `@Profile("!rejeu")` sur `HorlogeSysteme`) et `src/test/.../adapter/out/horloge/HorlogeRegleeTest.java` -- copie du modèle du cœur bancaire ; tests `@Tag("NFR-PAR-05")` sur le décalage, le refus et le choix de l'adaptateur selon le profil -- horloge commune aux services.
- `services/coeur-bancaire/.../application/ConsulterTableauDeBordCasUsage.java`, `port/in/ConsulterTableauDeBord.java`, `adapter/in/PointEntreeVirements.java` et leurs tests -- ajouter `aujourdhui` (date ISO `yyyy-MM-dd` lue par `Horloge.aujourdhui()`) à la vue et à `TableauDeBordClient` -- preuve observable de l'horloge réglée. Le front ignore ce champ, pas de changement de son côté.
- `compose.rejeu.yaml` -- pour les 8 services : `SPRING_PROFILES_ACTIVE: rejeu` et `HORLOGE_DECALAGE: ${HORLOGE_DECALAGE:-}` ; `NB_FOURNISSEURS_ALPHAVANTAGE_MODE: simule` sur bourse ; `NB_FOURNISSEURS_AZUREOPENAI_MODE: simule` et `NB_FOURNISSEURS_POWENS_MODE: simule` sur open-banking-conseiller. Mettre l'en-tête à jour -- §5.3.

*Données de référence*
- `services/coeur-bancaire/reference-data/reference-data.sql` -- les données de l'import :
  - 7 comptes, chacun sur une ligne ;
  - 45 opérations (`virement_id` nul) ;
  - 3 virements programmés (correspondance des colonnes en Design Notes) ;
  - les 5 bénéficiaires, à la place de la ligne inventée ;
  - 6 alertes de fraude ;
  - 6 plafonds de budget pour le client 1, mois `2025-11`.

  Puis `setval`, et relancer `scripts/generer-projections-reference.sh` -- DW-12, DW-15.
- `services/notifications/src/main/resources/db/migration/V4__notifications_de_reference.sql` -- remplacer le CHECK par `not (virement_id is not null and virement_programme_id is not null)` -- les 6 lignes legacy n'ont pas d'origine.
- `services/notifications/reference-data/reference-data.sql` (nouveau) -- les 6 notifications de l'import : type, titre, message exact, lue, cree_le. Écrire `overriding system value` pour les identifiants, puis `setval`. Le commentaire d'en-tête liste les colonnes perdues.
- `services/credits/src/main/resources/db/migration/V5__credit_scores_et_risk_profiles.sql` -- tables `credit_scores` et `risk_profiles`, colonnes de DATA-DICTIONARY.md, sans valeur par défaut temporelle -- les tables legacy appartiennent à credits (spine l. 226).
- `services/credits/reference-data/reference-data.sql` -- prêts et échéances de l'import (prêt 1 AUTO 12000.00, prêt 2 TRAVAUX 5000.00, 18 échéances), 4 scores, 4 profils. Supprimer `generer-reference-data.sh` et `GenerateurReferenceData.java` de credits, qui produisaient des valeurs fausses. Mettre à jour `ReferenceDataIT`.
- `services/identite-clients/src/test/.../referencedata/GenerateurReferenceData.java` -- écrire `date_creation` = `date '2026-10-06'`, puis régénérer `reference-data.sql` avec les `RETRO_*` de `.env` (`set -a; . ./.env; set +a`, sans jamais afficher les valeurs).
- `services/bourse/reference-data/reference-data.sql`, `services/open-banking-conseiller/reference-data/reference-data.sql` -- réaligner les identifiants sur l'import.
- `services/audit/reference-data/reference-data.sql` (nouveau) -- aucun INSERT, seulement le commentaire « AUDIT_LOGS : 0 ligne dans l'import ».
- Tous les tests de la Code Map qui lisent les données de référence -- attentes mises à jour, avec `@Tag("NFR-PAR-04")` sur les `ReferenceDataIT` -- couverture des données.

*Scripts et Makefile*
- `scripts/reset-reference-data.sh` (nouveau, exécutable) -- enchaîne les étapes suivantes. Chaque étape est annoncée, et un échec sort en code non nul en nommant l'étape.
  1. Arrêter les 8 conteneurs de services (`docker compose stop`) : c'est l'arrêt des consommateurs.
  2. Dans chaque base, `TRUNCATE … RESTART IDENTITY CASCADE` de toutes les tables du schéma `public` sauf `flyway_schema_history`.
  3. Mémoriser les topics non internes avec leur nombre de partitions, supprimer les groupes de consommateurs, supprimer les topics, attendre leur disparition, puis les recréer à l'identique.
  4. Charger `reference-data.sql` puis `projection-clients.sql` s'il existe, avec `psql -v ON_ERROR_STOP=1 --single-transaction`.
  5. Replacer chaque séquence et identité au-dessus du plus grand identifiant, de façon générique.
  6. Relancer les mêmes conteneurs (`docker compose start`, sans recréation) et attendre qu'ils soient sains. Ce redémarrage vide aussi les caches.

  Le script ne lit ni n'écrit `HORLOGE_*`.
- `scripts/golden-master/compter-donnees-reference.sh` (nouveau) -- sans option : instantané trié `service table lignes` de toutes les tables des 8 bases. Avec `--verifier` : contrôle du tableau des comptages ; code non nul qui nomme chaque table en écart.
- `scripts/golden-master/verifier-date-reference.sh` (nouveau) -- lit `HORLOGE_DEPART` (défaut `2026-11-17T10:00:00`).
  - Contrôle 1 : toute colonne `date`, `timestamp` ou `timestamptz` des tables chargées doit être strictement antérieure à cette date. Exceptions, les dates planifiées : `loans.date_fin`, `loan_repayments.date_echeance`, `virements_programmes.prochaine_execution`, `virements.heure_butoir`.
  - Contrôle 2 : au plus 8 opérations des comptes du client 1 datées de ce jour à Paris.
  - En cas d'écart, code non nul en nommant base, table, colonne et date.
- `scripts/rejeu-golden-master.sh` (nouveau) -- les étapes sont détaillées en Design Notes.
  - Exporter `TZ=Europe/Paris` et `LANG=fr_FR.UTF-8`, puis contrôler la présence des 3 `RETRO_*_PASSWORD`.
  - Lire la liste `writes: true` dans les `scenario.json` dérivés (avec `jq`).
  - Un échec de remise à zéro arrête tout immédiatement. Un échec du runner est noté ; les lancements continuent, puis on fusionne et on sort en code non nul en listant les lancements en échec.
- `Makefile` -- cibles `.PHONY` :
  - `reset-data` appelle `scripts/reset-reference-data.sh` ;
  - `rejeu-up` : `HORLOGE_DEPART ?= 2026-11-17T10:00:00`, calcul du décalage, `docker compose -f compose.yaml -f compose.rejeu.yaml up -d --build --wait`, puis `$(MAKE) reset-data` ;
  - `rejeu-golden-master` appelle le script.

  Les commentaires suivent le style de la cible `secrets`.
- `e2e/golden-master/package.json` (nouveau : `private`, `@playwright/test` `1.63.0` exact, sans `^`) et `e2e/golden-master/package-lock.json` (par `npm install`).
- `.gitignore` -- ajouter `e2e/golden-master/blob-report/`.

*Documentation*
- `golden-master-architecture.md` -- les points 9 et 10 du §8 et le §5.3 (timezoneId/locale) passent à « réglé » avec un renvoi aux scripts. `updated: 2026-10-09`, `status` inchangé.
- `ecarts-voulus-golden-master.md` -- une ligne par correction de `replay-mapping.json`, s'il y en a.

**Acceptance Criteria:**
- Given le dépôt, when `cd e2e/golden-master && npm ci && npx playwright install chromium` est lancé, then le code de sortie est 0 ; and `scripts/gates/golden-master.sh` sort en code 0 ; and `uv run docs/retro/tools/derive-golden-master.py derive --gm-dir docs/retro/golden-master --mapping replay-mapping.json --output-dir "$(mktemp -d)"` annonce `derived` = 31 et `dropped` = 2.
- Given une ligne retirée à la main d'un `expected-dom.txt` du dérivé (contre-épreuve faite puis annulée), when `scripts/gates/golden-master.sh` est lancé, then le code de sortie est non nul.
- Given compose, when `docker compose -f compose.yaml -f compose.rejeu.yaml config --format json | jq '[.services[] | select(.environment.SPRING_PROFILES_ACTIVE // "" | test("rejeu"))] | length'` est lancé, then il affiche `8` ; and la même configuration ne contient aucune valeur `reel` pour une variable `NB_FOURNISSEURS_*_MODE`, et `TZ=Europe/Paris` figure sur chaque conteneur.
- Given `HORLOGE_DEPART` absent, when `make rejeu-up` est lancé, then le code de sortie est 0, les conteneurs sont sains et `GET /api/coeur-bancaire/tableau-de-bord` avec le jeton de `user1` renvoie `aujourdhui` = `2026-11-17`.
- Given l'application en profil `rejeu`, when `make reset-data && scripts/golden-master/compter-donnees-reference.sh --verifier` est lancé 3 fois, then chaque exécution sort en code 0 et les 3 instantanés sont identiques ; and après un virement suivi de `make reset-data`, l'instantané est identique au précédent.
- Given `postgres-credits` arrêté volontairement, when `make rejeu-golden-master` est lancé, then le code de sortie est non nul, le message nomme l'étape de remise à zéro en échec, et aucun lancement du runner n'a lieu ensuite ; puis la base est relancée.
- Given les données chargées, when `scripts/golden-master/verifier-date-reference.sh` est lancé, then le code de sortie est 0 ; and avec `HORLOGE_DEPART=2020-01-01T10:00:00`, le code est non nul et le message nomme la table et la date en cause.
- Given les données chargées, when on les interroge, then on trouve les comptes `1`/`2`, le bénéficiaire `FR76…` SEPA de user1, le portefeuille de `sophie` (Jean Dupont, Marie Lebrun), `BNP.PA`/`AI.PA`, « CEL - Compte Epargne Logement »/« Livret A », `FR76-0001-0002`, et 3 comptes de connexion dont le mot de passe est haché ; and `git grep -n "$RETRO_CLIENT_PASSWORD"` ne trouve rien.
- Given les `RETRO_*` posées, when `cd e2e/golden-master && npx playwright test --project=connexion` est lancé, then le code de sortie est 0 et `.auth/{ADMIN,CONSEILLER,CLIENT}.json` existent (ignorés par git) ; and sans `RETRO_ADMIN_PASSWORD`, le code est non nul avec « variable d'environnement RETRO_ADMIN_PASSWORD absente ».
- Given les données remises à zéro, when `npx playwright test` est lancé une fois dans `e2e/golden-master`, then `report/index.html` et `results.json` sont produits ; les réussites et échecs sont consignés dans l'Auto Run Result pour la story 10.3.
- Given `make rejeu-up` par défaut, when `make reset-data && (cd e2e/golden-master && npx playwright test --grep "SC-003-04")` est lancé 3 fois, then les 3 rejeux sont verts ; and avec `HORLOGE_DEPART=2026-11-17T23:30:00 make rejeu-up`, la même commande sort en code non nul (UNUSUAL_HOURS) ; puis la pile est relancée par `make rejeu-up` par défaut.
- Given le code des services, when `! grep -rnE '\b(Instant|LocalDate|LocalDateTime|LocalTime|ZonedDateTime|OffsetDateTime|OffsetTime|Year|YearMonth)\.now\(|Clock\.system|System\.currentTimeMillis\(' services/*/src/main --include='*.java' | grep -vE '/adapter/out/(horloge|jeton)/'` est lancé, then le code de sortie est 0.
- Given la story terminée, when `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh` sont lancés, then chacun sort en code 0 ; and `git diff` sur `e2e/golden-master` (hors `package*.json`), le dérivé et `replay-mapping.json` est vide, sauf correction tracée au registre.

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- verdicts: 11 findings — high 0, medium 0, low 1, false 10, maybe-false 0
- findings:
  - `[false]` `[reject]` Décalage illisible : le refus de démarrage n'est vérifié que par `hasFailed()`, sans le message — l'exception levée au démarrage est celle de `HorlogeReglee.lireDecalage`, dont le message « La variable HORLOGE_DECALAGE … » est vérifié par `un_decalage_absent_ou_illisible_est_refuse` ; le message nomme donc bien la variable.
  - `[false]` `[reject]` Tableau de bord : `aujourdhui` = `2026-11-17` jamais observé sur la pile — rejoué dans cette passe : `GET /api/coeur-bancaire/tableau-de-bord` avec le jeton de `user1` après `make rejeu-up` renvoie `"aujourdhui": "2026-11-17"`.
  - `[false]` `[reject]` Remise à zéro répétée et après écriture sans preuve — rejoué : 3 × `make reset-data` + `--verifier` en code 0, instantanés identiques ; virement de 10.00 EXECUTE, puis `make reset-data` : instantané identique à celui d'avant.
  - `[low]` `[reject]` Après un échec de `reset-data`, les 8 conteneurs de services restent arrêtés — réel, mais le message le dit et demande de relancer `make reset-data` ; cas rare (panne de base), et un redémarrage automatique ajouterait une branche à un script d'échec.
  - `[false]` `[reject]` Remise à zéro en échec non éprouvée — rejoué : `postgres-credits` arrêté, `make rejeu-golden-master` sort en code non nul sur « étape 2/6 … (base de credits) » puis « échec : remise à zéro avant les lectures », sans lancer le runner.
  - `[false]` `[reject]` Date de référence trop tôt non éprouvée — rejoué : code 0 au départ par défaut, code 1 avec `HORLOGE_DEPART=2020-01-01T10:00:00`, chaque écart nommant base, table, colonne et date.
  - `[false]` `[reject]` Connexion sans mot de passe non éprouvée sur `auth.setup.ts` — rejoué : `npx playwright test --project=connexion` sans `RETRO_ADMIN_PASSWORD` sort en code 1 avec « variable d'environnement RETRO_ADMIN_PASSWORD absente ».
  - `[false]` `[reject]` Contre-épreuve à 23:30 seulement décrite — rejouée : `npx playwright test --grep SC-003-04` sort en code 1 (pas de notification CONFIRMATION), puis `make rejeu-up` par défaut.
  - `[false]` `[reject]` Variables des rapports de Playwright 1.63 non vérifiées dans `rejeu-golden-master.sh` — `node_modules/playwright/lib/runner/index.js:1536-1548` lit `PLAYWRIGHT_${name}_OUTPUT_FILE`/`_OUTPUT_DIR`, et `:3507` lit `PLAYWRIGHT_HTML_OUTPUT_DIR` : les noms utilisés sont valides.
  - `[false]` `[reject]` Contrainte CHECK des notifications assouplie, contraire à « chaque table garde son schéma » — les 6 notifications de l'import, exigées par le tableau des comptages, n'ont pas d'origine : la migration V4 est la seule façon de les charger ; le domaine `Notification` garde la règle « exactement une origine » pour toute écriture du service.
  - `[false]` `[reject]` `date_creation = 2026-10-06` inventée pour les clients — colonne cible sans valeur dans l'import ; sans valeur explicite elle prendrait `default current_date`, interdit dans une donnée de référence et rejeté par `verifier-date-reference.sh` ; la valeur est celle des projections déjà versionnées.

## Design Notes

**Correspondance des colonnes de l'import :**
- Opérations : `transaction_date` → `date_operation`.
- Virements programmés :
  - `target_account_number` → `numero_beneficiaire` ;
  - `target_label` → `libelle_beneficiaire` ;
  - `execution_count` → `nb_executions` ;
  - `created_date` → `cree_le` ;
  - `last_executed` → `derniere_execution` ;
  - `prochain_execution` → `prochaine_execution` ;
  - `frequence` → `MENSUEL`.
- Alertes : `rule` → `regle`, `status` → `statut`, `reviewed_*` → `revu_*`, `virement_id` nul.
- Budgets : `year_month` → `mois`, `category` → `categorie`.

Toutes les dates de l'import sont antérieures au 2026-11-17, hors échéances planifiées. Le budget prend le dernier mois qui a des opérations (`2025-11`) : les 6 plafonds y apparaissent.

**Remise à zéro par arrêt des conteneurs.** Aucun service n'expose de quoi mettre ses écouteurs en pause. Arrêter puis relancer les conteneurs coupe les consommateurs et vide les caches (Caffeine, coupe-circuits, état de `PowensSimule`). `docker compose start` relance le même conteneur avec le même `HORLOGE_DECALAGE`. Un `up` recréerait le conteneur, avec un décalage vide si la variable n'est pas exportée : le service refuserait alors de démarrer.

Les topics sont recréés par le script lui-même, à partir de la liste mémorisée : on ne dépend ni de l'ordre de démarrage ni d'une liste recopiée. Les groupes de consommateurs sont supprimés explicitement.

**Rejeu ordonné** (`e2e/golden-master` comme dossier courant) :
```bash
rm -rf blob-report
writes=$(jq -r 'select(.writes==true)|.id' ../../docs/retro/golden-master/derived/*/*/scenario.json | sort)
make -C ../.. reset-data || { echo "échec : remise à zéro avant les lectures" >&2; exit 1; }
PLAYWRIGHT_BLOB_OUTPUT_FILE=blob-report/lectures.zip npx playwright test --grep-invert "$(paste -sd'|' <<<"$writes")" --reporter=blob || echecs+=(lectures)
for sc in $writes; do make -C ../.. reset-data || { echo "échec : remise à zéro avant $sc" >&2; exit 1; }
  PLAYWRIGHT_BLOB_OUTPUT_FILE=blob-report/$sc.zip npx playwright test --grep "$sc" --reporter=blob || echecs+=("$sc"); done
PLAYWRIGHT_HTML_OUTPUT_DIR=report PLAYWRIGHT_HTML_OPEN=never PLAYWRIGHT_JSON_OUTPUT_FILE=results.json \
  npx playwright merge-reports --reporter=html,json ./blob-report
```
Vérifier les noms exacts des variables d'environnement des reporters de Playwright 1.63 : chaque blob doit garder un nom distinct, sinon le lancement suivant l'écrase.

## Auto Run Result

Premier `npx playwright test` dans `e2e/golden-master` (2026-10-09, après `make rejeu-up` par défaut puis une seule `make reset-data`, lancement unique non ordonné, sans remise à zéro entre les scénarios qui écrivent) : `report/index.html` et `results.json` produits ; 3 connexions réussies, 29 scénarios réussis, 2 en échec, 2 non rejoués (SC-003-03, SC-010-02). Échecs consignés pour la story 10.3, non corrigés ici :

- SC-004-01 (page Virements programmés) : ligne attendue « MONTANT (EUR) », l'écran rend « Montant (EUR) » (casse du libellé du champ montant).
- SC-011-01 (page Produits) : ligne attendue « CATALOGUE DES PRODUITS », l'écran rend « Catalogue des produits » (casse du titre).

Déterminisme de SC-003-04 : trois rejeux verts après `make reset-data` ; contre-épreuve avec `HORLOGE_DEPART=2026-11-17T23:30:00 make rejeu-up` en échec attendu (virement SUSPENDU, alertes UNUSUAL_HOURS et NEW_BENEFICIARY) ; pile relancée ensuite par `make rejeu-up` par défaut.

Status: done

**Résumé.** Banc de rejeu final du golden master monté : profil `rejeu` sur les 8 services avec une horloge réglée par un décalage unique (`HORLOGE_DECALAGE`), données de référence des 8 bases reprises de l'`import.sql` du legacy (153 lignes), remise à zéro idempotente (`make reset-data`), démarrage de la pile réglée (`make rejeu-up`), rejeu ordonné (`make rejeu-golden-master`), Playwright Test 1.63.0 installé dans `e2e/golden-master`. Déterminisme de SC-003-04 prouvé (3 rejeux verts, contre-épreuve à 23:30 en échec).

**Fichiers modifiés.**
- `compose.rejeu.yaml` : profil `rejeu` et `HORLOGE_DECALAGE` sur les 8 services ; `NB_FOURNISSEURS_*_MODE: simule` sur bourse et open-banking-conseiller.
- `Makefile` : cibles `reset-data`, `rejeu-up` (décalage `PT<n>S` calculé depuis `HORLOGE_DEPART`), `rejeu-golden-master`.
- `scripts/reset-reference-data.sh` : remise à zéro en 6 étapes annoncées (arrêt, vidage, topics Kafka recréés, chargement, séquences, relance sans recréation).
- `scripts/rejeu-golden-master.sh` : lectures puis chaque scénario `writes: true` après une remise à zéro, rapports blob fusionnés en html et json.
- `scripts/golden-master/compter-donnees-reference.sh` : instantané des comptages, et contrôle du tableau avec `--verifier`.
- `scripts/golden-master/verifier-date-reference.sh` : dates chargées antérieures à `HORLOGE_DEPART`, au plus 8 opérations de user1 ce jour-là.
- `services/{bourse,credits,documents,identite-clients,notifications,open-banking-conseiller}/.../adapter/out/horloge/HorlogeReglee.java` (+ tests `NFR-PAR-05`) ; `HorlogeSysteme` passée en `@Profile("!rejeu")`.
- `services/coeur-bancaire/.../ConsulterTableauDeBord*`, `PointEntreeVirements.java`, `AssemblageVirement.java` : champ `aujourdhui` du tableau de bord, lu par le port `Horloge`.
- `services/*/reference-data/reference-data.sql` : données de l'import (cœur bancaire, crédits, notifications et audit nouveaux, identifiants de la bourse et des produits réalignés, `date_creation` des clients fixée) ; projections régénérées par le script.
- `services/credits/.../V5__credit_scores_et_risk_profiles.sql`, `services/notifications/.../V4__notifications_de_reference.sql` : tables legacy de crédits, et notification sans origine acceptée en base.
- `services/credits/reference-data/generer-reference-data.sh` et `GenerateurReferenceData.java` de credits supprimés (prêts faux).
- Tests lisant les données de référence mis à jour (aucun désactivé) ; `ReferenceDataIT` taguées `NFR-PAR-04`, nouvelles pour le cœur bancaire et les notifications.
- `e2e/golden-master/package.json`, `package-lock.json` ; `.gitignore` (`blob-report/`) ; `golden-master-architecture.md` (§5.3, §8 points 9 et 10 réglés, `updated: 2026-10-09`, `status` inchangé).

**Revue.** 11 constats : 0 correctif appliqué, 0 report, 11 rejetés (10 faux, rejoués ou réfutés dans cette passe ; 1 faible, les conteneurs restent arrêtés après un échec de remise à zéro, ce que le message signale). Détail dans le Review Triage Log.

**Recommandation de revue complémentaire :** `false` (aucun correctif appliqué : 0 high, 0 medium, 0 low).

**Vérifications de cette tentative (toutes refaites à neuf).**
- `scripts/gates/build.sh` : code 0 (build propre, ArchUnit, couverture JaCoCo et Vitest, `tsc`).
- `scripts/gates/check-gates-exist.sh` : code 0, aucun KO.
- `scripts/gates/golden-master.sh` : code 0 ; dérivation fraîche : `derived` = 31, `dropped` = 2 ; aucun diff sur le dérivé, les tests générés et `replay-mapping.json` (seuls `package*.json` ajoutés).
- `scripts/gates/trace-smoke.sh` : code 0.
- Compose : 8 services en `rejeu`, `NB_FOURNISSEURS_*_MODE` tous à `simule`, `TZ=Europe/Paris` sur chaque conteneur.
- Recherche de `now()`/`Clock.system`/`currentTimeMillis` hors `adapter/out/(horloge|jeton)` : rien.
- `make rejeu-up` : code 0 ; tableau de bord de `user1` : `aujourdhui` = `2026-11-17`.
- 3 × `make reset-data` + `--verifier` : code 0, instantanés identiques ; après un virement puis `make reset-data`, instantané identique.
- `verifier-date-reference.sh` : 0 sur données fraîches, 1 avec `HORLOGE_DEPART=2020-01-01T10:00:00`.
- `postgres-credits` arrêté : `make rejeu-golden-master` en échec sur l'étape 2/6, runner non lancé ; base relancée.
- `npm ci`, `npx playwright install chromium`, `--project=connexion` : code 0, `.auth/*.json` ignorés par git ; sans `RETRO_ADMIN_PASSWORD` : code 1 avec le message attendu.
- SC-003-04 : 3 rejeux verts ; contre-épreuve à 23:30 en code 1 ; pile relancée par `make rejeu-up` par défaut.
- `git grep` du mot de passe client : rien.

**Risques résiduels.**
- Le rejeu complet ordonné (`make rejeu-golden-master`) n'a pas tourné en entier : il revient à la story 10.3, avec les 2 échecs de casse consignés plus haut (SC-004-01, SC-011-01).
- `verifier-date-reference.sh` doit tourner sur des données tout juste remises à zéro : une connexion écrit une ligne d'audit datée du jour, qu'il signale alors comme écart.
- Après un échec de remise à zéro, les conteneurs de services restent arrêtés jusqu'au `make reset-data` suivant.
- Hors story : la valeur de `RETRO_ADMIN_PASSWORD` figure déjà dans `docs/retro/00-global/PERMISSIONS.md` (corpus du legacy), à traiter à part.

## Verification

**Commands:**
- `mvn -B -q -f services/<service>/pom.xml test -Dgroups='NFR-PAR-04 | NFR-PAR-05'` pour chaque service modifié -- expected: code 0
- `make rejeu-up && for i in 1 2 3; do make reset-data && scripts/golden-master/compter-donnees-reference.sh --verifier || exit 1; done` -- expected: code 0
- `scripts/golden-master/verifier-date-reference.sh` -- expected: code 0
- `set -a; . ./.env; set +a; cd e2e/golden-master && npm ci && npx playwright install chromium && npx playwright test --project=connexion` -- expected: code 0
- `for i in 1 2 3; do make reset-data && (cd e2e/golden-master && npx playwright test --grep "SC-003-04") || exit 1; done` -- expected: code 0
- `HORLOGE_DEPART=2026-11-17T23:30:00 make rejeu-up && make reset-data && (cd e2e/golden-master && npx playwright test --grep "SC-003-04")` -- expected: code non nul, puis `make rejeu-up`
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh && scripts/gates/trace-smoke.sh` -- expected: code 0
