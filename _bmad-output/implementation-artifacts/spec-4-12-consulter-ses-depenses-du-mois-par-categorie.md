---
title: 'Consulter ses dépenses du mois par catégorie'
type: 'feature'
created: '2026-10-08'
status: 'done'
baseline_revision: '65c6974786a0653df282fccab44efc8611c8c67d'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-4-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-7-traiter-les-echeances-non-executees-et-prevenir-le-client.md'
  - '{project-root}/docs/quality-gates.md'
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** la route `/budget` (lien « Budget » du gabarit) n'a aucun écran, et le cœur bancaire n'expose ni les périodes d'un client, ni la ventilation de ses débits par catégorie, ni ses totaux du mois.

**Approach:** livrer l'écran `/budget` (période, trois indicateurs, répartition par catégorie, titre « Plafonds budgetaires » sans formulaire) et `GET /api/coeur-bancaire/budget?periode=AAAA-MM`. Le texte exact des critères est la story 4.12 de `_bmad-output/planning-artifacts/epics.md` (à partir de la ligne 3593) et FEAT-006 du PRD (FR-006-01 à FR-006-04) : la lire en entier. Le formulaire de plafond, la barre de progression et le tableau des plafonds sont la story 4.13 : ne pas les livrer ici.

## Boundaries & Constraints

**Always:** hexagone (`domain`/`application` Java pur et TypeScript pur, 100 % lignes et branches) ; temps par le port `Horloge` seul (`aujourdhui()`, `fuseau()`) ; montants `BigDecimal` 2 décimales, chaîne `"1234.50"` en JSON, le front en centimes entiers (jamais de flottant) ; client déduit du jeton (`sub`), tout autre paramètre (`clientId`, `compteId`) ignoré, opérations de **tous** les comptes du client ; textes du legacy (`docs/retro/features/FEAT-006/MESSAGES.md`, casse et absence d'accents comprises), aucun texte inventé ; couleurs par les tokens du thème ; `data-testid` `budget-*` ; tests tagués `FR-006-01` à `FR-006-04` (Java `@Tag`, Vitest `[FR-006-0x]` dans le titre) ; versions de `versions-verifiees.md`.
- **API** : réponse `{aujourdhui, periode, periodes[], totalDepenses, totalRevenus, plafondsDefinis, repartition[{categorie, montant}]}` ; `aujourdhui` = date du jour Europe/Paris (`AAAA-MM-JJ`) fournie par l'`Horloge` ; `periodes` = au plus 24 mois `AAAA-MM` les plus récents ayant au moins une opération (fuseau Europe/Paris), du plus récent au plus ancien ; `periode` = le paramètre `periode` s'il est au format `AAAA-MM`, sinon (absent ou illisible, sans erreur) le mois le plus récent avec opérations, sinon le mois de `aujourdhui`. Sans jeton : 401.
- **Ventilation** : seuls les débits de la période (bornes du mois en Europe/Paris) ; catégorie = la première, dans l'ordre Logement, Alimentation, Transport, Sante, Loisirs, Abonnements, Epargne, Revenus, Assurances, dont un mot-clé figure dans le libellé, comparaison sans casse ni accents ; sinon « Autres » ; catégories de montant nul absentes ; mots-clés de `textes-nouveaux.md` (lot « Stories », clés `budget.motscles.*`) tels quels, dans un enum de domaine `CategorieBudget` (libellé + mots-clés). `repartition` ordonnée comme les catégories. Totaux = somme des débits / des crédits de la période.
- **Plafonds** : nouvelle table `plafonds_budget` (migration Flyway `V9`) `(id, client_id, categorie, mois char(7), plafond numeric(19,2), unique (client_id, categorie, mois))` et port `Plafonds.compter(long clientId, String mois)` qui alimente `plafondsDefinis` (aucune écriture ici : la story 4.13 l'étend).
- **Front** : onglet « BankApp Demo - Budget » ; titres de section « Total depenses », « Total revenus », « Repartition par categorie », « Plafonds budgetaires » rendus en capitales par CSS (`text-transform`), carte « Plafonds definis » ; `<select>` natif « Periode : » (changement → rechargement immédiat) et bouton « Afficher » (rechargement) ; sans opération la période choisie vient de `aujourdhui` (jamais l'horloge du navigateur) ; section « Depenses du mois {AAAA-MM} » avec une ligne par catégorie (nom, montant `1234.50 EUR`) ou « Aucune depense enregistree sur cette periode » ; pas de bandeau technique (MSG-006-005) ; panne du service : texte technique générique existant (`TEXTES.erreurTechnique`) sans détail.

**Never:** modifier `scripts/gates/**`, `docs/quality-gates.md`, `.claude/**`, `.bmad-loop/**`, `frontend/.dependency-cruiser.cjs`, seuils, exclusions ; désactiver un test ; toucher `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json` ; rejouer le golden master ; formulaire de plafond, barre de progression, « Aucun plafond defini », tableau des plafonds, écriture de plafond (4.13) ; `now()`/`Clock`/`Date.now()`/`new Date()` hors des adaptateurs d'horloge ; appel HTTP entre services ; span créé à la main ; écrire `sprint-status.yaml`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / état | Comportement attendu | Erreur |
|----------|--------------|----------------------|--------|
| Sans session | `/budget` ou `GET /budget` sans jeton | redirection `/login` ; 401 | — |
| Périodes | 30 mois distincts avec opérations | 24 options les plus récentes, la plus récente sélectionnée | — |
| Sans opération | horloge au 2026-11-17 | période `2026-11` (champ `aujourdhui`), « Aucune depense enregistree sur cette periode », totaux `0.00 EUR` | — |
| Ventilation | débits « LOYER mars », « Carrefour », « xyz », crédits | Logement, Alimentation, Autres ; crédits hors répartition ; catégories nulles absentes | — |
| Totaux | 1 234,50 € de débits, 2 000,00 € de crédits, 2 plafonds | `1234.50 EUR`, `2000.00 EUR`, `2` | — |
| Changement de période | sélecteur modifié puis « Afficher » | un rechargement par action pour la nouvelle période | — |
| Données d'autrui | client A, opérations et plafonds de B le même mois, paramètre `clientId`/`compteId` de B | rien de B compté ni renvoyé | — |
| Panne | 500 / corps illisible | texte technique générique, écran conservé | — |

</intent-contract>

## Code Map

Cœur bancaire sous `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/` (`M/`), tests sous `…/src/test/java/fr/nextbanking/coeurbancaire/` (`T/`) ; front sous `frontend/src/`.

- `M/application/ConsulterReleveCasUsage.java`, `ConsulterTableauDeBordCasUsage.java`, `port/in/ConsulterReleve.java` -- modèles de cas d'usage et de port entrant ; nouveaux `ConsulterBudgetCasUsage` + `port/in/ConsulterBudget` (record `Budget`).
- `M/application/port/out/{Operations,Horloge}.java`, `M/adapter/out/persistance/OperationsJdbc.java` -- ajouter à `Operations` : `List<YearMonth> moisAvecOperations(long clientId, ZoneId fuseau, int limite)` et `List<Operation> duClientSurPeriode(long clientId, Instant debut, Instant fin)` (jointure `comptes.client_id`) ; `Horloge.aujourdhui()`/`fuseau()` donnent mois courant et bornes.
- nouveaux `M/domain/CategorieBudget.java` (enum, mots-clés, normalisation `java.text.Normalizer`), `M/application/port/out/Plafonds.java`, `M/adapter/out/persistance/PlafondsJdbc.java`, `M/resources/db/migration/V9__plafonds_budget.sql` (V1–V8 existent).
- `M/adapter/in/PointEntreeBudget.java` (nouveau, modèle `PointEntreeVirements` : `@GetMapping("/api/coeur-bancaire/budget")`, `@AuthenticationPrincipal Jwt`, `@RequestParam(required=false) String periode`) ; `M/adapter/in/ConfigurationSecurite.java` à vérifier (route authentifiée) ; `M/infrastructure/AssemblageBudget.java` (nouveau, modèle `AssemblageTraitements`).
- Tests à adapter : `T/application/MemoireCoeurBancaire` (implémenter les nouvelles méthodes de `Operations` et `Plafonds`), `T/adapter/CoeurBancaireSecuriteIT`, `ComptesJdbcIT`/`OperationsJdbc` IT existants ; nouveaux `T/domain/CategorieBudgetTest`, `T/application/ConsulterBudgetCasUsageTest`, `T/adapter/out/persistance/{Operations,Plafonds}JdbcIT`, `T/adapter/BudgetIT` (HTTP : 401, isolation client A/B).
- Front : `domain/budget.ts` (pur : `BudgetBrut`, ligne de répartition, `aujourdhui` → période, montant en centimes → `1234.50 EUR`), `application/consulterBudget.ts` (port `ClientBudget.lire(periode|null)`, `chargerBudget`), `adapters/http/ClientBudgetHttp.ts` (modèle `ClientReleveHttp.ts`, validation de forme), `adapters/ecrans/budget/EcranBudget.{tsx,css}` (modèle `EcranReleve.tsx`, `ListeDeroulante`, `BoutonSecondaire`, `Carte`), `adapters/textes.ts` (`titresOnglet.budget`, bloc `budget`), `adapters/routes/routes.tsx` (`clientBudget` dans `Dependances`, route `/budget` littérale), `infrastructure/main.tsx`, `adapters/test/rendreApplication.tsx` (paramètre `clientBudget` en dernier), leurs tests Vitest.
- `scripts/golden-master/verifier-scenario.sh SC-006-01` -- statique : route `/budget` littérale dans `routes/*.tsx` et quatre titres cherchés dans `frontend/src` ; ne rien y modifier.
- Lecture seule : `docs/retro/features/FEAT-006/{FIELDS,MESSAGES,RG}.md`, `textes-nouveaux.md` (lot « Stories »), golden master, `scripts/gates/**`.

## Tasks & Acceptance

**Execution:**
- `M/domain/CategorieBudget.java`, `M/application/{ConsulterBudgetCasUsage,port/in/ConsulterBudget,port/out/{Operations,Plafonds}}` -- périodes, ventilation, totaux, décompte des plafonds -- FR-006-01..04
- `M/adapter/out/persistance/{OperationsJdbc,PlafondsJdbc}`, `V9__plafonds_budget.sql`, `M/adapter/in/PointEntreeBudget`, `M/infrastructure/AssemblageBudget` -- API et persistance -- FR-006-01, NFR-SEC-03
- `T/**` listés dans la Code Map -- 100 % lignes et branches sur domain/application, 80 % adapter/infrastructure ; `@Tag("FR-006-01")` (isolation), `FR-006-02` (périodes), `FR-006-03` (ventilation), `FR-006-04` (totaux, plafonds)
- `frontend/src/{domain,application}/budget*`, `consulterBudget*` + tests -- règles pures -- FR-006-02..04
- `frontend/src/adapters/**` (client HTTP, écran, textes, route, assemblage, `rendreApplication`) + tests Vitest `[FR-006-0x]` -- écran `/budget` -- UX
- Ne toucher à aucun contrôle ni exclusion.

**Acceptance Criteria:**
- Given la story 4.12 d'`epics.md`, when chacun de ses critères est exécuté, then il est satisfait tel que rédigé
- Given `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-006-01|FR-006-02|FR-006-03|FR-006-04'` puis `cd frontend && npx vitest run -t 'FR-006-01|FR-006-02|FR-006-03|FR-006-04'`, when lancés, then code 0 chacun
- Given `scripts/golden-master/verifier-scenario.sh SC-006-01`, when lancé, then code 0
- Given `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` (et `trace-smoke.sh` si la pile compose est disponible), when lancés, then code 0

## Spec Change Log

## Review Triage Log

### 2026-10-08 — Review pass
- verdicts: 5 findings — high 0, medium 0, low 5, false 0, maybe-false 0
- findings:
  - `[low]` `[reject]` aucun test ne traverse le vrai service et le vrai écran — chaque couche est prouvée séparément (cas d'usage, `BudgetIT` HTTP avec base réelle, `ClientBudgetHttp` et écran Vitest) ; le proxy par préfixe de service (`vite.config.ts`) couvre déjà `/api/coeur-bancaire/budget`, un test de bout en bout n'apporterait pas de preuve nouvelle.
  - `[low]` `[reject]` « quatre titres en capitales » vérifié par la classe CSS et non par le rendu calculé — jsdom ne calcule pas `text-transform` ; la règle `uppercase` est présente dans `EcranBudget.css` et `verifier-scenario.sh SC-006-01` consigne les titres.
  - `[low]` `[reject]` option de période synthétique unique (`aujourdhui` tronqué) quand le client n'a aucune opération — c'est exactement le critère (période sélectionnée `2026-11`).
  - `[low]` `[reject]` sélecteur revenu à la valeur du mock après un changement dans le test d'écran — artefact du double de test, le service rend la période demandée.
  - `[low]` `[reject]` titre « Plafonds budgetaires » sans contenu et `plafonds_budget` sans écriture — voulu : formulaire, tableau et écriture sont la story 4.13.

## Design Notes

La période par défaut est décidée par le service (le front ne lit jamais d'horloge) : `aujourdhui` est tout de même rendu pour le cas « aucune opération » et prouvé par un test d'écran (période = `aujourdhui` tronquée à `AAAA-MM`, découpe de chaîne pure). Les mois sont calculés en Europe/Paris à partir du fuseau de l'`Horloge`, pas par un fuseau écrit dans le SQL. Un débit au libellé « salaire » tombe dans « Revenus » : comportement du legacy (RG-006-028) conservé, le refus de plafond « Revenus » est l'affaire de la 4.13.

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-006-01|FR-006-02|FR-006-03|FR-006-04'` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-006-01|FR-006-02|FR-006-03|FR-006-04'` -- expected: code 0
- `scripts/golden-master/verifier-scenario.sh SC-006-01` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** l'écran `/budget` et `GET /api/coeur-bancaire/budget?periode=AAAA-MM` sont livrés : sélecteur de période (24 mois les plus récents avec opérations, mois courant du service sans opération), totaux de dépenses et de revenus à deux décimales, nombre de plafonds, répartition des débits par catégorie (mots-clés sans casse ni accents, « Autres » par défaut, catégories nulles absentes), isolation par le jeton.

**Fichiers :** cœur bancaire — `CategorieBudget`, `ConsulterBudget`/`ConsulterBudgetCasUsage`, `Plafonds`/`PlafondsJdbc`, `Operations`/`OperationsJdbc` (mois et période du client), `PointEntreeBudget`, `AssemblageBudget`, migration `V9__plafonds_budget.sql` et tests ; front — `domain/budget.ts`, `application/consulterBudget.ts`, `ClientBudgetHttp.ts`, `EcranBudget.{tsx,css}`, textes, route `/budget`, assemblage et tests.

**Revue :** 5 constats — 0 patch, 0 reporté, 5 rejetés (négligeables, voir le journal). Relance recommandée : non.

**Vérification :** `mvn … -Dgroups='FR-006-01|…|FR-006-04'` code 0 ; `vitest -t 'FR-006-0[1-4]'` 48 tests passés ; `verifier-scenario.sh SC-006-01` code 0 ; `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` code 0.

**Risques résiduels :** `trace-smoke.sh` non lancé (pile compose non démarrée) ; un débit au libellé « salaire » est classé « Revenus » (comportement du legacy, RG-006-028), le refus de plafond « Revenus » relève de la story 4.13 ; la table `plafonds_budget` n'a pas encore d'écriture (4.13).
