---
title: 'Évaluer les signaux de fraude de chaque virement'
type: 'feature'
created: '2026-10-06'
baseline_revision: '9479d46514a5adfcd44c2244965cd7c9e7ba5b10'
status: 'done'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
  - '{project-root}/AGENTS.md'
  - '{project-root}/docs/quality-gates.md'
warnings: ['oversized']
deferred:
  - summary: >-
      Lignes de référence des bénéficiaires externes du legacy absentes : la table `beneficiaires_externes` est créée vide.
    evidence: |-
      Aucune ligne d'EXTERNAL_BENEFICIARIES ne figure dans docs/retro (seule la structure est dans DATA-DICTIONARY.md) et la spec interdit d'en inventer. Le parcours nominal SC-003-04 lève bien NEW_BENEFICIARY seul ; à compléter par la story « Socle du rejeu final » ou la gestion des bénéficiaires (epic 4).
    location: >-
      services/coeur-bancaire/reference-data/reference-data.sql
    severity: low
---

<intent-contract>

## Intent

**Problem:** le virement de la story 3.1 est exécuté sans aucun contrôle de fraude : les huit signaux de FEAT-027 ne sont pas évalués, aucune alerte n'est écrite, et le IBAN peut apparaître en clair dans un journal.

**Approach:** ajouter, dans le cœur bancaire seul, un contrôle de fraude synchrone et sans appel réseau, écrit en Java pur dans `domain`/`application`, appelé par le cas d'usage de virement après les contrôles de saisie, de propriété et de solde et avant tout débit. Il lit l'historique par le seul port `HistoriqueVirements`, additionne les scores et, au-delà de 75, enregistre une alerte `OPEN` par signal dans `alertes_fraude`, dans la transaction du virement. Le virement reste exécuté quel que soit son score (la suspension est la story 3.4). Le texte exact de chaque critère est la story 3.2 de `_bmad-output/planning-artifacts/epics.md` (lignes 2316-2398) : elle fait foi, la lire en entier.

## Boundaries & Constraints

**Always:** reprendre les critères d'acceptation de la story 3.2 d'`epics.md` tels que rédigés (scores, textes anglais des descriptions du legacy, bornes) ; `domain` et `application` en Java pur, 100 % lignes et branches ; temps uniquement par le port `Horloge` (heure inhabituelle au fuseau `horloge.fuseau()`, jamais `TZ`) ; montants `BigDecimal` ; entre services aucune dépendance (tout reste dans le cœur bancaire) ; objet-valeur `Iban` dont `toString()` masque (`FR76****0189` : 4 premiers caractères, `****`, 4 derniers) et qui sert à toute journalisation ; seuils `nb.fraude.*` (score cumulé 75, seuils de LARGE_AMOUNT 5 000 et 15 000, plafond quotidien 8, secondes de succession rapide 30, fenêtre 60 min, nombre max 5, montant de vélocité 500) lus dans la configuration et validés au démarrage : valeur négative, nulle ou absente = démarrage refusé (`ConfigurationFraude*Test`) ; verrou `pg_advisory_xact_lock(clientId)` pris par le port `HistoriqueVirements` au début du contrôle, dans la transaction de soumission ; un seul signal LARGE_AMOUNT par virement (A-003-4) ; HIGH_FREQUENCY compte les opérations au relevé du jour courant, débits et crédits, avant l'écriture des deux opérations du virement en cours (A-027-11) ; RAPID_SUCCESSION compare au précédent virement enregistré du client (A-003-5) ; VELOCITY_CHECK compte les virements exécutés de plus de 500 € (strictement) des 60 dernières minutes (A-003-9) ; chaque test tagué `@Tag` avec les identifiants `FR-027-xx`, `FR-003-xx`, `BUG-010`, `BUG-085`, `BUG-086` que les `-Dgroups` de la story citent ; versions de `versions-verifiees.md`.

**Never:** suspendre, refuser ou notifier (stories 3.3, 3.4) ; point d'entrée public d'évaluation isolée du score (RP-08) ; appel réseau ou HTTP dans le contrôle ; créer un span à la main ; texte inventé (tout texte absent est consigné dans `deferred`) ; inventer des lignes de bénéficiaires absentes de `docs/retro` ; modifier `scripts/gates/**`, `docs/quality-gates.md`, `.claude/**`, `.bmad-loop/**`, `frontend/.dependency-cruiser.cjs`, seuils de couverture ou de mutation, exclusions ; désactiver un test ; toucher `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json` ; rejouer le golden master ; écrire `sprint-status.yaml` ; toucher au front ni aux contrats d'événements (aucun nouvel événement).

## I/O & Edge-Case Matrix

| Scénario | Entrée / état | Comportement attendu | Erreur |
|----------|--------------|---------------------|--------|
| Rejet de saisie ou de solde | comptes identiques, montant -5, solde insuffisant | aucun signal évalué, aucune alerte, et il n'entre pas dans RAPID_SUCCESSION du suivant (FR-003-07) | erreur de 3.1 inchangée |
| LARGE_AMOUNT | 4 999,99 / 5 000,00 / 14 999,99 / 15 000,00 | rien / 40 « …exceeds the large threshold of {threshold} » / 40 / 80 « …exceeds the very-large threshold of {threshold} » ; un seul signal | — |
| HIGH_FREQUENCY | 100 anciennes + 8 du jour, puis 9 (Europe/Paris) | rien, puis 50 « Customer has {count} transactions today, exceeding the daily limit of {maxDailyTransactions} » | — |
| NEW_BENEFICIARY | destinataire non bénéficiaire actif / bénéficiaire actif | 35 « Target IBAN {iban} is not registered as a trusted beneficiary » / rien | — |
| UNUSUAL_HOURS | 05:59, 10:00, 23:30 | 45 « …hour {hourOfDay}… (06:00-23:00) » à 05:59 et 23:30, rien à 10:00 | — |
| ROUND_AMOUNT | 999, 1 000, 1 500, 3 000 | 20 « …suspicious round figure (multiple of 1000) » pour 1 000 et 3 000 seulement | — |
| FOREIGN_IBAN | « DE89… », « fr76… », « FR76… » | 30 « Target IBAN {iban} does not start with FR (possible foreign transfer) » pour DE89 seulement | — |
| RAPID_SUCCESSION | précédent virement 29 s puis 31 s avant | 55 « Previous transaction was only {elapsedSeconds} seconds ago (threshold: {rapidSuccessionSeconds}s) » à 29 s, rien à 31 s | — |
| VELOCITY_CHECK | 4 puis 5 virements exécutés > 500 € sur 60 min | rien, puis 65 « Customer executed {highValueCount} transactions above 500 EUR in the last {velocityWindowMinutes} minutes (limit: {velocityMaxCount}) » | — |
| Score cumulé | 75, puis 80 | aucune alerte à 75 ; à 80 une ligne `alertes_fraude` OPEN par signal levé (score, description), dans la transaction du virement ; virement exécuté dans les deux cas | — |
| Parcours nominal SC-003-04 | horloge `2026-11-17T10:00:00` Europe/Paris, `user1` vire 100 € du `1` au `2` | seul NEW_BENEFICIARY (35), aucune alerte, virement exécuté | — |
| Journal | virement vers « FR7630006000011234567890189 » | aucune ligne ne contient l'IBAN en clair ; « FR76****0189 » (BUG-086) ; IBAN en clair réintroduit → `-Dgroups='FR-027-15'` sort non nul | — |
| Configuration | seuil `nb.fraude.*` négatif ou nul | le service refuse de démarrer | démarrage refusé |

</intent-contract>

## Code Map

- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/application/ExecuterVirementCasUsage.java` -- méthode `ecrire(...)` (transaction de soumission) : après `Virement.executer(...)` (propriété, solde) et avant `comptes.debiter`, appeler le contrôle ; après `virements.enregistrer(...)`, enregistrer les alertes (leur `virement_id` a besoin de la ligne). Le constructeur reçoit les nouveaux ports ; `AssemblageVirement` l'assemble.
- `.../domain/Virement.java` -- `verifierSaisie`/`executer` portent FR-003-07 : le contrôle est hors de ces méthodes, appelé seulement après leur succès. `SeuilsFraude` (seuils 3.1) reste ; modèle de validation à imiter pour les nouveaux paramètres.
- `.../domain/` (à créer) -- `Iban` (record, `toString()` masqué, `commencePar`), `TypeSignal` (huit valeurs), `Signal` (type, score, description), `ParametresControleFraude` (validé comme `SeuilsFraude`), `ControleFraude` (évaluation pure, somme, décision « alertes à écrire si somme > score cumulé max »), `AlerteFraude`, `StatutAlerte` (OPEN, CONFIRMED, FALSE_POSITIVE, REVIEWED), `HistoriqueClient` (valeurs lues : opérations du jour, dernier virement, virements > 500 € de la fenêtre) ; textes des descriptions dans `TextesVirement` (seul fichier de textes ; descriptions du legacy en anglais, `FEAT-027/MESSAGES.md` §3).
- `.../application/port/out/` (à créer) -- `HistoriqueVirements` (verrou consultatif du client, comptage des opérations depuis le début du jour, dernier virement, virements exécutés > 500 € depuis un instant), `Beneficiaires` (bénéficiaire actif du client pour un numéro, lecture seule), `AlertesFraude` (enregistrer) ; `application/ControleFraudeCasUsage` (ou méthode du cas d'usage de virement) orchestre lecture, évaluation, journalisation (IBAN par `Iban`) ; modèle de port : `Comptes.java`, `Transaction.java`, `Horloge.java` (`fuseau()`).
- `.../adapter/out/persistance/` (à créer) -- `HistoriqueVirementsJdbc` (`select pg_advisory_xact_lock(:clientId)`, requêtes de comptage sur `operations`/`comptes`/`virements`), `BeneficiairesJdbc`, `AlertesFraudeJdbc` ; modèles : `ComptesJdbc.java`, `DepotVirementsJdbc.java`, test `ComptesJdbcIT`.
- `.../infrastructure/ConfigurationFraude.java` et `AssemblageVirement.java` -- bean `ParametresControleFraude` lu dans `nb.fraude.*` ; câblage ; `src/main/resources/application.properties` : nouvelles propriétés `nb.fraude.*` avec les valeurs par défaut de la story.
- `services/coeur-bancaire/src/main/resources/db/migration/V3__alertes_fraude_et_beneficiaires.sql` -- tables `alertes_fraude` (id, client_id, virement_id, regle, score, description, statut OPEN par défaut, cree_le, revu_le, revu_par) et `beneficiaires_externes` (colonnes d'`EXTERNAL_BENEFICIARIES` de `docs/retro/00-global/DATA-DICTIONARY.md` : actif, banque, bic, créé le, titulaire, iban, libellé, zone SEPA, client) ; index sur `operations (compte_id, date_operation)` et `virements (client_id, soumis_le)` si utile ; aucun `now()`.
- `services/coeur-bancaire/reference-data/reference-data.sql` -- aucune ligne de bénéficiaire dans `docs/retro` : ne rien inventer (consigner dans `deferred`) ; le parcours nominal lève NEW_BENEFICIARY.
- `services/coeur-bancaire/src/test/java/…` -- modèles : `application/ExecuterVirementCasUsageTest` et `MemoireCoeurBancaire` (faux en mémoire à compléter pour les nouveaux ports), `domain/VirementTest`, `domain/SeuilsFraudeTest`, `infrastructure/ConfigurationFraudeTest`, `adapter/VirementIT` (intégration Postgres via `Conteneurs`, horloge réglée), `HorlogeArchitectureTest` et `HexagonalArchitectureTest` (doivent rester verts).
- `docs/retro/features/FEAT-027/{RG,MESSAGES}.md`, `docs/retro/00-global/{LEGACY-BUGS,DATA-DICTIONARY}.md` ; PRD FR-003-15 à FR-003-25 et FR-027-01 à FR-027-10 ; spine AD-12 (`ARCHITECTURE-SPINE.md` l. 187-201) -- sources de vérité.

## Tasks & Acceptance

**Execution:**
- `services/coeur-bancaire/src/main/java/**/domain/**` -- `Iban`, `TypeSignal`, `Signal`, `ParametresControleFraude`, `ControleFraude`, `AlerteFraude`, `StatutAlerte`, textes des descriptions -- logique pure des huit signaux, score cumulé et seuil d'alerte
- `services/coeur-bancaire/src/main/java/**/application/**` -- ports `HistoriqueVirements`, `Beneficiaires`, `AlertesFraude`, intégration au cas d'usage de virement (ordre : saisie → propriété/solde → contrôle → enregistrement du virement → alertes → débit/crédit) -- contrôle avant tout débit, alertes dans la transaction
- `services/coeur-bancaire/src/main/java/**/adapter/out/persistance/**`, `infrastructure/**`, `application.properties`, `V3__…sql` -- adaptateurs JDBC, verrou consultatif, configuration validée, migration -- accès aux données
- Tests (tagués `FR-027-01|03|05|06|07|08|09|10|15`, `FR-003-07|15|17..25`, `BUG-010|085|086`) : `ControleFraude*Test` (une classe par groupe de signaux, bornes de la matrice, 100 % lignes et branches), `ExecuterVirementCasUsageTest` complété (ordre, alertes à 75/80, rejet de saisie sans signal), `IbanTest`, `ConfigurationFraude*Test` (seuils négatifs ou nuls), test de journal capturant les messages (IBAN masqué, échec si l'IBAN en clair y figure, tag `FR-027-15`), `VirementIT` complété (nominal SC-003-04, alertes en base, sérialisation par client) -- toute la matrice

**Acceptance Criteria:**
- Given la story 3.2 d'`epics.md`, when chacun de ses critères est exécuté, then il est satisfait tel que rédigé
- Given `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-027-01 | FR-027-03 | FR-027-05 | FR-027-06 | FR-027-07 | FR-027-08 | FR-027-09 | FR-027-10 | FR-027-15 | FR-003-07 | FR-003-15 | FR-003-17 | FR-003-18 | FR-003-19 | FR-003-20 | FR-003-21 | FR-003-22 | FR-003-23 | FR-003-24 | FR-003-25'`, puis `-Dgroups='BUG-010 | BUG-085 | BUG-086'`, when lancés, then code de sortie 0 chacun
- Given l'IBAN en clair réintroduit dans un message de journal du contrôle, when `-Dgroups='FR-027-15'`, then code de sortie non nul
- Given `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` et `trace-smoke.sh`, when lancés, then code de sortie 0 (les `KO` de `check-gates-exist.sh` ne concernent que des stories non livrées)

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 3 findings — high 0, medium 0, low 1, false 2, maybe-false 0
- findings:
  - `[false]` `[reject]` table nommée `alertes_fraude` et non `FRAUD_ALERTS` — convention du projet (noms en français, comme `comptes`, `virements` de la 3.1) ; aucun critère n'exige le nom physique.
  - `[low]` `[defer]` aucune ligne de référence de bénéficiaire — absente de docs/retro, la spec interdit d'en inventer ; consigné dans `deferred`.
  - `[false]` `[reject]` pas de test de démarrage refusé sur seuil négatif — `ConfigurationFraudeTest` et `ConfigurationFraudeSignauxTest` existent et passent (`build.sh` code 0).

## Design Notes

- Un échec du contrôle (accès à l'historique) n'est pas encore un refus : il fait échouer la transaction et suit le chemin « remise impossible » de la 3.1 ; REFUSÉ_ERREUR_CONTRÔLE et le délai `nb.fraude.acces-historique.delai` sont la story 3.4. Toute erreur est journalisée.
- Heure inhabituelle : `heure < 6 ou heure >= 23` au fuseau `Horloge.fuseau()` (le texte « {hourOfDay} » est l'heure) ; 23:00:00 à 23:59 compte comme hors plage, seul 23:30 est fixé par la story.
- La description d'une alerte conserve le numéro de compte destinataire comme le legacy (« Target IBAN {iban} ») ; seul le journal masque. Le texte stocké est lu par l'administrateur en 3.4.
- Le virement est enregistré avant les alertes (clé `virement_id`) ; les alertes sont écrites seulement si la somme des scores > 75 (une alerte par signal levé).

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml verify` -- expected: code 0 (couverture JaCoCo, ArchUnit)
- les deux commandes `-Dgroups` de la section Acceptance -- expected: code 0
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh`, `scripts/gates/trace-smoke.sh` -- expected: code 0
- `! grep -rnE 'Observation\.(createNotStarted|start)\(|@Observed|@NewSpan|spanBuilder\(|nextSpan\(' services/*/src/main --include=*.java` -- expected: aucune ligne

## Auto Run Result

Status: done

**Résumé :** contrôle de fraude synchrone du cœur bancaire : huit signaux (LARGE_AMOUNT 40/80, HIGH_FREQUENCY, NEW_BENEFICIARY, UNUSUAL_HOURS, ROUND_AMOUNT, FOREIGN_IBAN, RAPID_SUCCESSION, VELOCITY_CHECK) évalués dans le domaine avant tout débit, historique lu par le seul port `HistoriqueVirements`, verrou `pg_advisory_xact_lock(clientId)`, alertes OPEN écrites dans la transaction du virement quand le score dépasse 75, IBAN masqué dans le journal (`Iban`), seuils `nb.fraude.*` validés au démarrage. Le virement reste exécuté quel que soit son score (suspension : story 3.4).

**Fichiers :** `services/coeur-bancaire/**` : domaine (`Iban`, `ControleFraude`, `Signal`, `TypeSignal`, `ParametresControleFraude`, `AlerteFraude`, `StatutAlerte`, `HistoriqueClient`, textes), application (`ControleFraudeCasUsage`, trois ports, intégration à `ExecuterVirementCasUsage`), adaptateurs JDBC, migration V3, configuration, tests unitaires et d'intégration.

**Revue :** 0 patch ; 1 élément différé (lignes de bénéficiaires absentes du corpus) ; 2 rejetés (nom de table suivant la convention française ; tests de configuration présents). Recommandation de nouvelle revue : `false`.

**Vérification (relancée) :** `scripts/gates/build.sh` (code 0 au second passage ; le premier a échoué sur un délai de 5 s du test front `main.test.ts`, sans lien avec le diff), `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` code 0 ; les deux `mvn test -Dgroups` code 0 ; grep des spans manuels vide.

**Risques résiduels :** une erreur d'accès à l'historique fait échouer la transaction (chemin « remise impossible ») faute de REFUSÉ_ERREUR_CONTRÔLE (story 3.4) ; VELOCITY_CHECK ne compte que l'état EXECUTE ; test front `main.test.ts` sensible à la charge machine.
