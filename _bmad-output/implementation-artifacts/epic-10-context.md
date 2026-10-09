# Epic 10 Context: Recette finale et bascule

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Clore la réécriture par la preuve de parité. D'abord, corriger BUG-009 : un virement important exécuté et débité (statut technique `PENDING`) est annoncé comme une « transaction importante », sans les mots « en attente » ni « verification ». Ensuite, monter un banc de rejeu reproductible : profil `rejeu`, horloge réglée, données de référence des 8 services rechargées par une commande, comptes de test par rôle et fournisseurs simulés. Enfin, rejouer une seule fois le golden master du legacy, en entier, pour mesurer le critère de bascule : 31 scénarios réussis, 0 en échec et 2 non rejoués (SC-003-03, SC-010-02).

## Stories

- Story 10.1: Reformuler les textes d'un virement important exécuté
- Story 10.2: Monter le socle du rejeu final du golden master
- Story 10.3: Rejouer le golden master en entier et mesurer le critère de bascule

## Requirements & Constraints

- **Transaction importante (BUG-009)** : pour un virement de 5 000 à 9 999,99 € dont le score cumulé est inférieur ou égal à 75, l'exécution et le débit sont immédiats et le statut enregistré reste `PENDING`. Les textes deviennent :
  - notification ALERTE unique : « Virement de {montant} EUR vers le compte {numCompte} execute - transaction importante. » ;
  - préfixe des descriptions DEBIT et CREDIT : « [TRANSACTION IMPORTANTE] » ;
  - badge du relevé : « Transaction importante ».

  Ces textes sont sans accents, dans le style du legacy, et leur formulation est arrêtée.
- Aucun texte affiché pour ce virement ne contient « en attente » ni « verification », sans tenir compte de la casse. Plus aucun texte du cœur bancaire ne contient « en attente de validation ».
- Les opérations déjà en base qui commencent par « [EN ATTENTE] » gardent leur badge « En attente », et les 13 caractères du préfixe restent retirés du libellé.
- Les autres cas ne changent pas :
  - en dessous de 5 000 € : `SUCCESS` avec une notification CONFIRMATION ;
  - à partir de 10 000 € : `SUSPENDU` sans débit, avec la seule notification FRAUDE.
- **Parité** : le golden master se rejoue une seule fois, quand le développement est terminé, et jamais entre les stories. Le périmètre compte 33 scénarios, dont 2 exclus. Les 31 applicables se répartissent en 26 d'écran, comparés en mode « contient » sur `innerText`, et 5 HTTP. Un texte ajouté ne casse rien ; un texte legacy modifié ou retiré fait échouer le scénario.
- **Rejeu déterministe** : l'horloge est réglée au démarrage, puis elle avance. Elle n'est pas figée, car une horloge figée déclencherait RAPID_SUCCESSION. Les données sont remises à zéro avant le rejeu complet, puis avant chaque scénario `writes: true`.
- **Données de référence** : elles sont équivalentes à l'`import.sql` du legacy (153 lignes réparties sur les 8 bases) et se rechargent par une commande idempotente. Elles contiennent ce qu'exige le golden master :
  - comptes `1` et `2`, et le RIB du compte `1` ;
  - soldes et 5 dernières opérations du COURANT de `user1` ;
  - bénéficiaire `FR76***` ;
  - portefeuille de `sophie` (Jean Dupont / `user1`, Marie Lebrun / `user2`) ;
  - watchlist `BNP.PA` et `AI.PA` ;
  - « CEL - Compte Epargne Logement » et « Livret A » ;
  - compte interne `FR76-0001-0002`.
- **Secrets** : les mots de passe des comptes de test (`admin` ADMIN, `sophie` CONSEILLER, `user1` CLIENT) sont générés hachés depuis `RETRO_ADMIN_PASSWORD`, `RETRO_CONSEILLER_PASSWORD` et `RETRO_CLIENT_PASSWORD`. Ils n'apparaissent jamais en clair dans un fichier versionné.
- **Fournisseurs externes** : Powens, AlphaVantage et Azure OpenAI restent sur leur adaptateur simulé. Aucun test ne dépend du réseau, et aucune variable `NB_FOURNISSEURS_*_MODE` ne vaut `reel`.
- **Couverture** : l'outil de couverture du PRD doit sortir en code 0 avec 100 % des règles reprises. Chaque FR doit être portée par au moins un test, tagué `@Tag("FR-xxx-yy")` côté JUnit ou titré `[FR-xxx-yy]` côté Vitest. Chaque bug corrigé a son test.

## Technical Decisions

- **Textes du cœur bancaire** : « [TRANSACTION IMPORTANTE] » et « transaction importante » sont définis dans un seul fichier de textes. La notification est construite par le producteur (cœur bancaire, type ALERTE) et publiée dans `coeur-bancaire.virement-decide.v1`. Les tests portent `@Tag("FR-003-29")` et `@Tag("BUG-009")` en JUnit, ou `[FR-003-29]` et `[BUG-009]` dans les titres Vitest.
- **Profil `rejeu`** : il est défini dans `compose.rejeu.yaml` et actif sur les 8 services. Dans ce profil :
  - l'horloge démarre à `HORLOGE_DEPART`, par défaut `2026-11-17T10:00:00` Europe/Paris ;
  - `make rejeu-up` calcule un seul `HORLOGE_DECALAGE` et le passe à tous les services ;
  - `TZ=Europe/Paris` est posé sur chaque conteneur ;
  - le JWT dure 60 minutes ;
  - les traitements planifiés métier sont désactivés, sauf le relais de l'outbox.
- **Temps** : il passe uniquement par le port `Horloge`. Aucun `*.now()`, `Clock.system` ni `System.currentTimeMillis()` n'apparaît hors de `adapter/out/horloge` et `adapter/out/jeton`.
- **Cibles Makefile** :
  - `reset-data` appelle `scripts/reset-reference-data.sh` ;
  - `rejeu-up` lance `compose.yaml` et `compose.rejeu.yaml`, puis `reset-data`, et attend que tout soit sain ;
  - `rejeu-golden-master` appelle `scripts/rejeu-golden-master.sh`.
- **Remise à zéro** : elle enchaîne ces étapes, sans toucher à l'horloge :
  1. arrêter les consommateurs Kafka ;
  2. lancer `TRUNCATE … RESTART IDENTITY CASCADE` sur les tables métier, les projections, `outbox`, `outbox_audit` et `evenements_traites` ;
  3. supprimer puis recréer les topics ;
  4. recharger les 8 `services/<id>/reference-data/reference-data.sql` ;
  5. replacer les séquences ;
  6. vider les caches ;
  7. relancer les consommateurs.

  Les projections de référence sont générées par `scripts/generer-projections-reference.sh` et ne sont jamais écrites à la main.
- **Runner** : Playwright Test `@playwright/test` en version exacte 1.63.0, installé dans `e2e/golden-master`, avec le `package-lock.json` versionné. L'URL de base est `http://localhost:8080/`. Les sessions sont enregistrées dans `.auth/` (ignoré par git) par le projet `connexion`.
- **Script de rejeu** : `scripts/rejeu-golden-master.sh` procède ainsi :
  1. exporter `TZ=Europe/Paris` et `LANG=fr_FR.UTF-8`, car le `playwright.config.ts` généré ne porte ni `timezoneId` ni `locale` et ne doit pas être modifié ;
  2. lancer `reset-data`, puis `--grep-invert` sur les scénarios qui écrivent, en `--reporter=blob` ;
  3. pour chaque scénario `writes: true`, lu dans les `scenario.json` dérivés et jamais recopié dans une liste, lancer `reset-data` puis `--grep "<SC>"` ;
  4. fusionner les rapports avec `merge-reports --reporter=html,json`.

  Le script s'arrête en code non nul dès qu'une étape échoue, en la nommant. `blob-report/` est ignoré par git.
- **Écarts voulus** : ils sont adaptés en `map` dans `replay-mapping.json` :
  - SC-001-02 : 401 sur `GET /api/coeur-bancaire/tableau-de-bord` ;
  - SC-017-01 : 401 sur `GET /api/documents/telechargement?type=RIB&compteId=1` ;
  - SC-001-03 : 401 et `{"message":"Identifiants incorrects"}` ;
  - SC-001-01 : 200 sur `/login` ;
  - SC-014-01 : 302.

  Deux scénarios sont exclus : SC-003-03 (BUG-011, couvert par FR-003-06) et SC-010-02 (BUG-038, couvert par FR-010-09 avec le simulateur Azure en mode 401/403).
- **Golden master protégé** : `e2e/golden-master/**`, `docs/retro/golden-master/derived/**` et `replay-mapping.json` ne sont jamais édités à la main. Seule la story 10.2 peut corriger `replay-mapping.json`, et elle suit alors toujours la même voie :
  1. redériver avec `uv run docs/retro/tools/derive-golden-master.py derive … --force` ;
  2. régénérer les tests avec `export-golden-master-tests.py` ;
  3. inscrire la correction dans `ecarts-voulus-golden-master.md` (scénario, ce qui change, raison).

  La dérivation doit rester à `derived` = 31 et `dropped` = 2. En 10.1 et 10.3, `git diff --exit-code` sur ces chemins doit sortir en code 0 : un écart se corrige dans l'application, dans les données de référence ou dans la remise à zéro. Les scénarios se comptent avec les outils de `docs/retro/tools/`, jamais à la main.

## UX & Interaction Patterns

- **Relevé** : une ligne « [TRANSACTION IMPORTANTE] » affiche le badge `tag` « Transaction importante » avant le libellé, et le préfixe est retiré du libellé, sur le même modèle que « [EN ATTENTE] ». Le même virement s'affiche aussi au tableau de bord (5 dernières opérations, centre de notifications) et dans le suivi des virements du client, sans « en attente » ni « verification ».
- **Wording** : les textes legacy sont intouchables. Les textes ajoutés sont libres, puisque la comparaison se fait en mode « contient ». La liste des écarts de texte validés est fermée, et aucun ne touche une ligne attendue.
- **Écran de connexion** : il utilise un vrai `<form>` soumis par Entrée, avec les `data-testid` `login-identifiant`, `login-mot-de-passe` et `login-me-connecter`. Après connexion, chaque rôle arrive sur son écran : `admin` sur `/administration`, `sophie` sur `/espace-conseiller` et `user1` sur `/tableau-de-bord`.

## Cross-Story Dependencies

- **10.1** dépend de la fin de toutes les stories des epics 3 à 9 et de la démonstration déjà faite (décision B1 révisée). Elle ne touche aucune ligne attendue du golden master.
- **10.2** dépend de 10.1 et de l'existence des 8 services et des 15 routes de la SPA. Elle s'appuie sur :
  - le port Horloge et le profil `rejeu` du cœur bancaire (epic 3) ;
  - les simulateurs Powens, AlphaVantage et Azure OpenAI (epics 7 et 8) ;
  - les variables `RETRO_*_PASSWORD` fournies par l'humain.

  Elle prouve le déterminisme de SC-003-04 par trois rejeux verts, puis par une contre-épreuve à 23:30 qui doit échouer (UNUSUAL_HOURS). Elle règle les points ouverts sur la date de référence, la remise à zéro par scénario et le fuseau/la locale, ce qui permet de passer `golden-master-architecture.md` en `status: final`.
- **10.3** dépend de 10.2, d'un développement terminé (aucune story en cours) et des `RETRO_*_PASSWORD`. C'est le seul rejeu complet.
- Chaque story se termine avec `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh` et `golden-master.sh` en code 0.
