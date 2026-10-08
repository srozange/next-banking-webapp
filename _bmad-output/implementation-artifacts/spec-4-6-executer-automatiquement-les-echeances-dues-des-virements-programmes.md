---
title: 'Exécuter automatiquement les échéances dues des virements programmés'
type: 'feature'
created: '2026-10-08'
status: 'done'
baseline_revision: 'd3cd7c2d1ac77b09362045c085772072fe91c1d3'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-4-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-4-suspendre-reactiver-ou-supprimer-un-virement-programme.md'
  - '{project-root}/docs/quality-gates.md'
warnings: [oversized]
deferred:
  - summary: >-
      Un motif de plus de 100 caractères, accepté à la création (255 à la story 4.3), fait échouer à chaque passage l'exécution de l'échéance (colonne virements.motif limitée à 100).
    evidence: |-
      DemandeVirementProgramme.MOTIF_MAX = 255 alors que Motif.LONGUEUR_MAX = 100 : Motif.depuis lève une IllegalArgumentException dans ExecuterEcheancesCasUsage.ecrire ; l'échéance est comptée en échec et retentée indéfiniment. À arbitrer avec le métier (aligner la limite à la création, ou tronquer).
    location: >-
      services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/application/ExecuterEcheancesCasUsage.java
    severity: medium
  - summary: >-
      L'attribut d'observation virement.controle.issue n'est pas posé sur la tâche planifiée des échéances.
    evidence: |-
      Le contexte d'epic 4 le prévoit pour la tâche planifiée ; seule la requête HTTP est enrichie (EnrichissementTrace). Aucun critère de la story 4.6 ne l'exige ; à traiter avec la story 4.8 (suspension d'une échéance) si la trace doit porter l'issue.
    location: >-
      services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/adapter/in/EnrichissementTrace.java
    severity: low
  - summary: >-
      Solde insuffisant, destinataire introuvable et suspension par la fraude laissent l'échéance inchangée et due, sans notification ni désactivation, jusqu'aux stories 4.7 et 4.8.
    evidence: |-
      Choix assumé de la story 4.6 (voir Design Notes) ; les tests de 4.6 assertent « reste due ». Les stories 4.7 et 4.8 devront remplacer ce comportement (notification ALERTE, désactivation après compte introuvable, suspension et avance de la date) et mettre ces tests à jour. Le cas « destinataire introuvable » journalise pour l'instant le texte legacy « Echec execution virement programme id={id} » sans compter d'échec.
    location: >-
      services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/application/ExecuterEcheancesCasUsage.java
    severity: medium
---

<intent-contract>

## Intent

**Problem:** Les virements programmés (stories 4.3 et 4.4) ne partent jamais : rien ne sélectionne les échéances dues, ne débite le compte source ni ne fait avancer la prochaine date.

**Approach:** Cœur bancaire seul : un `@Scheduled` toutes les 5 minutes (zone `Europe/Paris`) appelle un cas d'usage qui exécute, une échéance à la fois et chacune dans sa transaction, les virements programmés actifs et dus, par le même contrôle de fraude que le virement immédiat. Pas de front, pas de nouvel événement, aucune notification.

## Boundaries & Constraints

**Always:** Hexagone (domain/application Java pur). Temps uniquement par `Horloge`. Déclencheur `PlanificationEcheances` dans `adapter.in.planification`, `@Profile("!rejeu")`, cron `0 */5 * * * *`, `zone = "Europe/Paris"`, qui appelle le port entrant `ExecuterEcheances` ; chaque passage journalise (java.util.logging, via le cas d'usage) « Verification virements programmes dus ». Sélection : `actif` et `prochaine_execution <= maintenant`, par `prochaine_execution` croissante puis `id`, tous clients confondus. **Une seule échéance exécutée par virement et par passage**, même en retard de plusieurs périodes (les suivantes aux passages suivants) ; une date de première exécution passée ou une échéance due pendant un arrêt part au premier passage. Chaque échéance : une transaction (`Transaction.executer`) ; un échec isolé annule complètement cette échéance (ni débit, crédit, opération, avance) et n'empêche pas les autres (log « Echec execution virement programme id={id} »). Ordre dans la transaction : numéro du bénéficiaire → compte par `Comptes.parNumero` ; verrou des deux comptes (`Comptes.verrouiller`) ; **prise de l'échéance** par mise à jour conditionnelle `where id = :id and actif and prochaine_execution = :échéancePrévue` (zéro ligne = perdue, aucun effet : deux passages concurrents ne créent qu'un virement et un débit) qui avance `prochaine_execution`, fait `nb_executions + 1` et enregistre `derniere_execution` ; solde ; `ControleFraudeCasUsage.evaluer` puis `decider` (mêmes seuils que ExecuterVirementCasUsage) ; si EXÉCUTÉ : `Virement` EXECUTE enregistré (sans clé d'idempotence), `enregistrerAlertes`, `Comptes.debiter` (faux : annule), `crediter`, deux opérations DEBIT « Virement programme vers {libellé bénéficiaire}[ - {motif}] » et CREDIT « Virement programme de {numéro du compte source}[ - {motif}] » (motif absent : sans le suffixe, jamais de mention « [EN ATTENTE] »). Prochaine échéance = échéance prévue (jamais l'heure réelle) + 7 jours / 1, 2, 3, 6 mois / 1 an calculés en `Europe/Paris` (heure locale conservée à travers les changements d'heure) : méthode du domaine `FrequenceVirement`. Journal INFO (j.u.l.) « Virement programme execute id={id} montant=100.00 ». **Aucune notification** : ni `VirementDecide` ni `VirementATraiter` n'est consigné pour une échéance exécutée. Audit par `EcritureAudit` après la transaction, au nom du titulaire (`Long.toString(clientId)`), `adresseIp` nulle : `VIREMENT` (« Montant: 100.00 - Motif: Epargne mensuelle - Issue: EXECUTE ») puis `TRANSFER_EXECUTED`. Journal `batch_job_logs` : **une ligne `RECURRING_TRANSFERS` par passage** (début, fin, nombre d'échéances exécutées) : SUCCESS et détails « Verification virements programmes dus » si aucune échéance en échec ; avec échecs, PARTIAL (≥ 1 exécutée) ou FAILED (0 exécutée), détails « Echec execution virement programme id={id} » (ids séparés par « ; ») ; lecture des échéances en erreur : FAILED, 0, « Erreur: {message} ». Une écriture de journal en erreur est loguée, jamais relancée. Nouvelle migration Flyway `V7` : `alter table virements_programmes add column derniere_execution timestamptz` (nullable). Tests étiquetés `@Tag("FR-004-07")`, `FR-004-11`, `FR-004-14`, `FR-021-01`…`03`, `05`…`08`, `10`, `BUG-069` selon ce qu'ils prouvent ; concurrence : `*TransitionConcurrente*Test`.

**Never:** Pas de notification, de nouvel événement ni de contrat (les cas non exécutés relèvent de la story 4.7, la suspension par la fraude de la 4.8). Pas d'écriture du journal de nuit hors `RECURRING_TRANSFERS` ni de garde par date (story 4.5). Pas d'appel HTTP entre services. Pas de rejeu du golden master ; ne pas modifier scripts/gates/**, golden master, `replay-mapping.json`, seuils ou exclusions de couverture. Pas de texte inventé hors des textes du legacy ci-dessus. Pas de `now()`/`Clock` hors des adaptateurs d'horloge. Pas de span créé à la main. Pas de changement de front.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Erreur |
|----------|--------------|----------------------|--------|
| Sélection | horloge 10:00 ; actif dû 09:55, actif dû 08:00, inactif dû 07:00 | seuls les deux actifs, traités 08:00 puis 09:55 ; inactif et son compte source inchangés | — |
| Exécution | mensuel actif dû, 100.00, solde suffisant, vers `FR76-0001-0002`, motif « Epargne mensuelle » | EXECUTE ; débit source, crédit bénéficiaire ; 2 opérations ; `nb_executions` +1 ; `derniere_execution` = instant ; prochaine = échéance prévue + 1 mois ; pas d'événement de décision ; audit VIREMENT + TRANSFER_EXECUTED sans IP | — |
| Sans dérive | mensuel prévu 15/01 08:00, exécuté 08:04 ; puis 12 exécutions | 15/02 08:00 ; toujours le 15 à 08:00 | — |
| Retard | dû depuis 3 périodes | une seule exécution par passage | — |
| Sans motif | motif absent | libellés sans suffixe | — |
| Solde insuffisant | solde < montant | aucune écriture, échéance inchangée (reste due) ; log WARN « Solde insuffisant pour virement programme id={id} » ; non comptée | — |
| Destinataire introuvable | numéro sans compte | aucune écriture, échéance inchangée ; log WARN ; non comptée | — |
| Suspendu par la fraude | décision SUSPENDU | transaction annulée, échéance inchangée, rien débité ; log WARN ; traité par la story 4.8 | — |
| Contrôle en erreur | `ControleEnErreur` | annulé, retenté au passage suivant ; compté en échec | échec isolé |
| Échec isolé | 2ᵉ de 3 échoue | 1ʳᵉ et 3ᵉ exécutées, 2ᵉ intacte ; ligne PARTIAL | — |
| Concurrence | deux passages sur la même échéance | un virement, un débit | une prise perdue = sans effet |

</intent-contract>

## Code Map

Chemins Java sous `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/` (`M/`), tests sous `…/src/test/java/fr/nextbanking/coeurbancaire/` (`T/`). Modèles directs : `M/application/ExecuterVirementCasUsage.java` (transaction, contrôle de fraude, écritures, audit), `M/application/EcheanceRevueCasUsage.java` (passage planifié + ligne de journal), `M/adapter/in/planification/PlanificationRejetEcheance.java` (déclencheur), `M/infrastructure/AssemblageVirement.java`.

- `M/domain/FrequenceVirement.java` -- ajouter `Instant suivante(Instant echeance, ZoneId fuseau)` (`atZone(fuseau).plusDays/plusMonths/plusYears(...).toInstant()`).
- `M/domain/Virement.java` -- ajouter une fabrique `executerEcheance(Soumission, Compte source, Compte destinataire, String libelleBeneficiaire, SeuilsFraude)` : propriété de la source et solde contrôlés, destinataire quelconque (pas de `COMPTE_ETRANGER`), états/statut par `naitre` (privé), opérations avec les libellés programmés ; `operations(...)` à paramétrer par libellés. `M/domain/TextesVirement.java` -- `libelleDebitProgramme(libelle, motif)`, `libelleCreditProgramme(numeroSource, motif)`, `journalExecute(id, montant)`, `journalSoldeInsuffisant(id)`, `journalEchec(id)`, `detailsVerification()` (constantes legacy FEAT-021 MESSAGES.md).
- `M/application/port/out/VirementsProgrammes.java` + `M/adapter/out/persistance/VirementsProgrammesJdbc.java` -- ajouter `List<VirementProgramme> dues(Instant maintenant)` et `boolean prendreEcheance(long id, Instant echeancePrevue, Instant prochaine, Instant executeLe)` (update conditionnel ci-dessus, vrai si une ligne modifiée). `M/resources/db/migration/V7__derniere_execution_virements_programmes.sql` (V6 : colonnes de `virements_programmes`).
- `M/application/port/in/ExecuterEcheances.java` (`int executerLesDues()` : nombre d'échéances exécutées) ; `M/application/ExecuterEcheancesCasUsage.java` -- constructeur `(VirementsProgrammes, Comptes, Operations, DepotVirements, JournalAudit, JournalBatch, Transaction, Horloge, SeuilsFraude, ControleFraudeCasUsage)` ; `evaluer` et `enregistrerAlertes` sont package-private (même paquet). Issue interne d'une échéance (EXECUTEE / IGNOREE / NON_EXECUTEE) ; rollback d'une non-exécution par exception interne dans `Transaction.executer`. `JOB = "RECURRING_TRANSFERS"`.
- `M/adapter/in/planification/PlanificationEcheances.java` ; `M/infrastructure/AssemblageVirementsProgrammes.java` -- bean `ExecuterEcheances` (le contrôle de fraude est le bean `controleFraude` d'`AssemblageVirement`).
- Tests : `T/domain/FrequenceVirementTest` (nouveau, six fréquences, 12 mois sans dérive, changement d'heure) ; `T/domain/VirementTest` ou nouveau test de `executerEcheance` ; `T/application/MemoireCoeurBancaire.java` (implémenter `dues`/`prendreEcheance`, mémoriser `derniereExecution` par id, ajouter `virementsProgrammes` en rollback dans `executer`) ; `T/application/ExecuterEcheancesCasUsageTest` (matrice) ; `T/adapter/in/planification/PlanificationEcheancesTest` (modèle `PlanificationRejetEcheanceTest`) ; `T/adapter/out/persistance/VirementsProgrammesJdbcIT` (dues, prise conditionnelle) ; `T/adapter/EcheancesIT` (bout en bout base réelle : exécution, opérations, avance, audit sans IP, aucun événement de décision) ; `T/adapter/EcheanceTransitionConcurrenteTest` (modèle `VirementProgrammeTransitionConcurrenteTest`) ; `T/infrastructure/ProfilRejeuIT` doit rester vert (le déclencheur est `!rejeu`). `T/adapter/DonneesDeReference` donne comptes et clients.
- Lecture seule : `docs/retro/features/FEAT-021/{RG,MESSAGES}.md`, golden master.

## Tasks & Acceptance

**Execution:**
- `FrequenceVirement`, `Virement`, `TextesVirement` -- date suivante sans dérive, fabrique et libellés programmés -- FR-021-05/06/08, FR-004-14, BUG-069
- `VirementsProgrammes` + Jdbc + `V7` -- échéances dues, prise conditionnelle, dernière exécution -- FR-021-02/07, concurrence
- `ExecuterEcheances`, `ExecuterEcheancesCasUsage`, `AssemblageVirementsProgrammes` -- passage, transaction par échéance, fraude, audit, journal -- FR-021-03/10, FR-026-03/05, NFR-BATCH-02
- `PlanificationEcheances` -- déclencheur 5 min Paris, `!rejeu` -- FR-021-01, NFR-BATCH-01/03
- Tests listés dans la Code Map -- 100 % lignes et branches sur domain/application, 80 % adapter

**Acceptance Criteria:**
- Given le cœur bancaire hors `rejeu`, when j'inspecte ses déclencheurs, then un `@Scheduled` `0 */5 * * * *` en `Europe/Paris` appelle `ExecuterEcheances` (absent en profil `rejeu`) et chaque passage journalise « Verification virements programmes dus »
- Given trois virements (actif dû 09:55, actif dû 08:00, inactif dû 07:00) à 10:00, when un passage s'exécute, then les deux actifs seulement, dans l'ordre 08:00 puis 09:55
- Given un mensuel de 100 € dû vers un compte interne, when il s'exécute, then débit/crédit, deux opérations aux libellés du legacy, compteur +1, dernière exécution enregistrée, aucun événement de décision, audit VIREMENT + TRANSFER_EXECUTED sans IP, journal « Virement programme execute id=… montant=100.00 »
- Given 15/01 08:00 exécuté à 08:04, then prochaine le 15/02 08:00 et toujours le 15 à 08:00 après douze exécutions ; +7 jours, +1/2/3/6 mois, +1 an (`FR-021-08`, `FR-004-14`, `BUG-069`)
- Given trois périodes de retard, then une seule exécution par passage ; échéance passée ou due pendant un arrêt exécutée au passage suivant
- Given un passage terminé, then une ligne `RECURRING_TRANSFERS` (nombre exécuté, statut)
- Given un échec isolé, then les autres échéances sont exécutées et la fautive est intacte ; deux passages concurrents : un virement, un débit
- Given la story terminée, when `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-004-07|FR-004-11|FR-004-14|FR-021-01|FR-021-02|FR-021-03|FR-021-05|FR-021-06|FR-021-07|FR-021-08|FR-021-10|BUG-069'`, then code 0
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, then code 0 (`trace-smoke.sh` si la pile compose est disponible)

## Spec Change Log

## Review Triage Log

### 2026-10-08 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 3, false 1, maybe-false 0
- findings:
  - `[low]` `[reject]` le déclencheur n'est vérifié que par réflexion, jamais en contexte Spring — même méthode que PlanificationRejetEcheanceTest ; ProfilRejeuIT prouve en contexte réel que seuls les relais tournent en profil `rejeu`, et @EnableScheduling est déjà actif ; un test de plus n'apporterait pas de preuve nouvelle.
  - `[false]` `[reject]` la prise conditionnelle précède le contrôle du solde, contrairement à « après les contrôles bénéficiaire et solde » — le solde est contrôlé avant l'évaluation de fraude, et la prise est annulée avec la transaction (rollback prouvé par `un_solde_insuffisant_ne_change_rien_et_reste_du` et EcheancesIT) ; aucun effet durable.
  - `[low]` `[reject]` le virement est écrit par `Virement.executerEcheance` et non par le cas d'usage du virement immédiat — le « même cas d'usage de contrôle de fraude » est bien partagé (`ControleFraudeCasUsage.evaluer` et `enregistrerAlertes`) ; la propriété des deux comptes du virement immédiat ne s'applique pas à un bénéficiaire interne d'un autre client.
  - `[low]` `[defer]` solde insuffisant, destinataire introuvable et suspension laissés « sans effet » figent un comportement que 4.7 et 4.8 remplaceront — consigné dans `deferred`.

## Design Notes

La story 4.5 (journal des relevés, garde par date, section d'administration) n'est pas livrée : cette story n'utilise que `JournalBatch` et la table `batch_job_logs` déjà posés par l'epic 3 (`V4`, `V5`). Les trois non-exécutions (solde, destinataire, suspension par la fraude) laissent l'échéance inchangée — elle reste due et est retentée à chaque passage — jusqu'aux stories 4.7 (notification et désactivation) et 4.8 (suspension). La prise conditionnelle de l'échéance avant les écritures sert de barrière : un second passage bloque sur le verrou de ligne puis ne trouve plus l'échéance prévue. L'observation de la tâche planifiée (attribut `virement.controle.issue`) n'est pas ajoutée ici : elle relève d'un enrichissement dédié si la story 4.8 l'exige. Un motif de plus de 100 caractères (accepté à 255 à la création, 4.3) fait échouer l'exécution de façon isolée à chaque passage (colonne `virements.motif`) : à arbitrer avec le métier.

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-004-07|FR-004-11|FR-004-14|FR-021-01|FR-021-02|FR-021-03|FR-021-05|FR-021-06|FR-021-07|FR-021-08|FR-021-10|BUG-069'` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** les échéances dues des virements programmés sont exécutées toutes les 5 minutes (zone `Europe/Paris`, absent en profil `rejeu`), une par virement et par passage, chacune dans sa transaction : prise conditionnelle de l'échéance, solde, même contrôle de fraude que le virement immédiat, débit, crédit, deux opérations aux libellés du legacy, compteur +1, dernière exécution enregistrée, prochaine échéance calculée depuis l'échéance prévue (sans dérive). Aucune notification ; audit `VIREMENT` + `TRANSFER_EXECUTED` au nom du titulaire, sans IP ; une ligne `RECURRING_TRANSFERS` par passage.

**Fichiers :** `PlanificationEcheances` (déclencheur), `ExecuterEcheances` et `ExecuterEcheancesCasUsage`, `VirementsProgrammes`/`VirementsProgrammesJdbc` (`dues`, `prendreEcheance`), migration `V7__derniere_execution_virements_programmes.sql`, `FrequenceVirement.suivante`, `Virement.verifierEcheance`/`executerEcheance`, textes legacy dans `TextesVirement`, `AssemblageVirementsProgrammes` ; tests `FrequenceVirementTest`, `VirementTest`, `ExecuterEcheancesCasUsageTest`, `PlanificationEcheancesTest`, `VirementsProgrammesJdbcIT`, `EcheancesIT`, `EcheanceTransitionConcurrenteTest`, `MemoireCoeurBancaire`.

**Revue :** 4 constats — 0 patch, 1 reporté (3 points dans `deferred`), 3 rejetés (2 négligeables, 1 faux). Relance recommandée : non.

**Vérification :** `mvn … -Dgroups='FR-004-07|…|BUG-069'` code 0 ; `scripts/gates/build.sh`, `check-gates-exist.sh` et `golden-master.sh` code 0 ; `EcheanceTransitionConcurrenteTest` vert dans `build.sh`.

**Risques résiduels :** `trace-smoke.sh` non lancé (pile compose non démarrée ; aucun span ni Kafka ajouté) ; motif > 100 caractères en échec permanent ; les trois cas non exécutés (solde, destinataire, fraude) attendent les stories 4.7 et 4.8 ; l'avance « 31 du mois » peut se tasser sur la fin de mois (échéance prévue recalculée mois par mois).
