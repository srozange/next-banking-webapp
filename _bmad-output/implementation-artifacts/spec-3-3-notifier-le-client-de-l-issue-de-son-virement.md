---
title: "Notifier le client de l'issue de son virement"
type: 'feature'
created: '2026-10-06'
baseline_revision: 'd3d0c5c389445221f7029c9db80f7f9ce79067df'
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
      Titre des notifications de virement : aucun texte défini, `titre` laissé à null dans l'événement et en base.
    evidence: |-
      AD-25 prévoit un `titre` rendu par le producteur, mais le legacy n'en a pas pour un virement (MESSAGES.md) et textes-nouveaux.md n'en propose pas ; aucun texte inventé. À fixer par le métier (point ouvert) avant l'affichage de la story 3.9.
    location: >-
      services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/domain/TextesVirement.java
    severity: low
---

<intent-contract>

## Intent

**Problem:** un virement exécuté (stories 3.1 et 3.2) ne produit aucune notification : le service `notifications` ne reçoit que l'événement témoin, ne stocke rien et n'a ni plafond par utilisateur ni persistance des notifications.

**Approach:** le cœur bancaire décide tout le contenu de la notification (AD-25) et publie, par l'outbox et dans la transaction de la décision, l'événement `coeur-bancaire.virement-decide.v1` ; le service `notifications` le consomme de façon idempotente, stocke la notification telle que rendue, applique le plafond par utilisateur `nb.notifications.plafond` et la conserve en base. Le texte exact de chaque critère est la story 3.3 de `_bmad-output/planning-artifacts/epics.md` (lignes 2399-2451) : elle fait foi, la lire en entier.

## Boundaries & Constraints

**Always:** reprendre les critères de la story 3.3 d'`epics.md` tels que rédigés ; `domain` et `application` en Java pur, 100 % lignes et branches (le `check` JaCoCo s'applique même avec `-Dgroups`) ; événement `coeur-bancaire.virement-decide.v1` : clé `virementId`, enveloppe d'AD-18 (`idEvenement`, `type` = `virement-decide`, `version` 1, `horodatage` lu sur `Horloge`, `producteur`, `donnees` = `virementId`, `clientId`, `etat`, `notification`), écrit dans la même transaction que le débit et publié après commit comme `virement-a-traiter` ; `donnees.notification` porte `clientId`, `type`, `titre`, `message` déjà rendus ; CONFIRMATION si `montant < seuilAlerte` (`SeuilsFraude.seuilAlerte`, paramètre distinct du seuil d'attente, ne pas les fusionner), ALERTE sinon ; messages « Virement de {montant} EUR vers le compte {numCompte} confirme » (+ « (motif : {motif}) » si motif non vide ; un motif absent, vide ou d'espaces donne le message sans motif, `Virement.motif()` est déjà normalisé à `null`) et « Virement de {montant} EUR en attente de validation - transaction importante, verification en cours. » (`TextesVirement`, seul fichier de textes du virement) ; `{montant}` = `montant.toPlainString()` (« 100.00 »), `{numCompte}` = numéro du compte destinataire ; schéma `contrats/evenements/coeur-bancaire.virement-decide.v1.schema.json` avec `examples`, modèle `coeur-bancaire.virement-a-traiter.v1.schema.json` ; `etat` = `EtatVirement.EXECUTE` (seul état produit aujourd'hui) ; `notifications` stocke sans composer de texte ni lire de seuil (`! grep -rn 'nb\.fraude\.' services/notifications/src/main`) ; consommateur idempotent par `evenements_traites` (copier `RecevoirTemoinCasUsage`, `EvenementsTraitesJdbc`) ; plafond par utilisateur : la 21e supprime la plus ancienne dans la même transaction, la nouvelle est en tête ; `nb.notifications.plafond` (20 par défaut) validé au démarrage : 0 ou négatif = démarrage refusé ; migration Flyway `V2` du service `notifications` (notifications persistées en base, base `pgdata-notifications`) : mêmes notifications, même ordre, même état lu/non lu après redémarrage ; chaque test tagué `@Tag` avec les identifiants cités par les `-Dgroups` ; versions de `versions-verifiees.md`.

**Never:** notification pour la remise asynchrone (`virement-a-traiter`), la libération, le rejet, le rejet automatique ou le refus pour erreur ; deux notifications pour un même virement ; texte inventé : le `titre` n'a aucun texte défini dans le legacy ni dans `textes-nouveaux.md` pour un virement, il reste `null` (schéma `["string","null"]`) et est consigné dans `deferred` comme point ouvert ; seuil métier ou `nb.fraude.*` dans `notifications` ; créer un span à la main ; toucher au front (l'affichage est la story 3.9) ; modifier `scripts/gates/**`, `docs/quality-gates.md`, `.claude/**`, `.bmad-loop/**`, `frontend/.dependency-cruiser.cjs`, seuils de couverture ou de mutation, exclusions ; désactiver un test ; toucher `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json` ; rejouer le golden master ; écrire `sprint-status.yaml`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / état | Comportement attendu | Erreur |
|----------|--------------|---------------------|--------|
| Confirmation sans motif | 100,00 € vers « FR76-0001-0002 », motif absent ou d'espaces | CONFIRMATION « Virement de 100.00 EUR vers le compte FR76-0001-0002 confirme » | — |
| Confirmation avec motif | idem, motif « loyer » | « … confirme (motif : loyer) » | — |
| Alerte | 5 000,00 € puis 4 999,99 € | ALERTE « Virement de 5000.00 EUR en attente de validation - transaction importante, verification en cours. » puis CONFIRMATION | — |
| Décision et outbox | virement exécuté | une ligne `virement-decide` dans l'outbox, dans la transaction du débit ; échec d'écriture = opération en échec | — |
| Livraison double | même événement brut deux fois | une seule notification (`evenements_traites`) | — |
| Remise asynchrone | `virement-a-traiter` consommé | aucune notification, aucune ligne `virement-decide` | — |
| Plafond | 20 notifications puis une 21e | la plus ancienne supprimée, la nouvelle en tête, 20 restantes | — |
| Redémarrage | notifications lues et non lues | contenu, ordre et état de lecture identiques | — |
| Configuration | `nb.notifications.plafond` = 0 ou négatif | `notifications` refuse de démarrer | démarrage refusé |

</intent-contract>

## Code Map

- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/application/ExecuterVirementCasUsage.java` -- `ecrire(...)` (l.100-121) : après `virements.enregistrer`/opérations, consigner une 2e ligne d'outbox `virement-decide` ; `Ecriture(virement, ligneOutbox)` doit porter les deux lignes, publiées après commit (l.90) ; `SeuilsFraude seuils` déjà injecté ; `seuilAlerte` n'est lu nulle part aujourd'hui.
- `.../domain/` -- `VirementDecide` (événement), `Notification`/type `TypeNotification` (CONFIRMATION, ALERTE ; FRAUDE viendra en 3.4), rendu du message dans `TextesVirement` (`TRANSACTION_IMPORTANTE` existe déjà, ajouter les gabarits de confirmation avec et sans motif) ; `Virement.java` (motif normalisé l.59, `EtatVirement.EXECUTE`).
- `.../application/port/out/EvenementsSortants.java` -- ajouter `consigner(VirementDecide)` ; `MemoireCoeurBancaire` (test, l.185-199) à compléter.
- `.../adapter/out/evenements/EvenementsSortantsKafka.java` + `EnveloppeVirementATraiter.java` -- modèle pour `EnveloppeVirementDecide` (`TOPIC`, `de(...)`, `Donnees`) ; `infrastructure/ConfigurationEvenements.java` l.16-36 : `NewTopic` `coeur-bancaire.virement-decide.v1` (3 partitions) ; aucune migration (table `outbox` générique).
- `.../adapter/in/evenements/RecepteurVirementATraiter.java` et `application/TraiterVirementATraiterCasUsage.java` -- ne produisent aucune notification : ajouter un test `FR-023-03` (remise après décision : aucune ligne `virement-decide`), modèle `ReceptionVirementATraiterIT`.
- `contrats/evenements/coeur-bancaire.virement-decide.v1.schema.json` (à créer) ; test producteur `VirementDecideContratEvenementTest` sur le modèle `VirementATraiterContratEvenementTest` (champs produits = `required` = `properties`, y compris `donnees.notification` ; `const` ; produit = `examples[0]`) ; test consommateur `VirementDecideContratEvenementTest` dans `notifications` sur le modèle `adapter/in/TemoinEcritContratEvenementTest`.
- `services/notifications/` -- existant : `adapter/in/RecepteurTemoin`, `application/RecevoirTemoinCasUsage`, port `EvenementsTraites`, `adapter/out/persistance/{EvenementsTraitesJdbc,TransactionSpring}`, `infrastructure/AssemblageReceptionTemoin`, `Conteneurs`, `ReceptionTemoinTest`, Flyway `V1`. À créer : `domain/Notification`, port `Notifications` (dépôt, plafond), `application/RecevoirVirementDecideCasUsage`, `adapter/in/RecepteurVirementDecide` (`@KafkaListener` sur le nouveau topic), `adapter/out/persistance/NotificationsJdbc`, `infrastructure/` assemblage + `ConfigurationNotifications` (plafond validé), `db/migration/V2__notifications.sql`, `application.properties` (`nb.notifications.plafond=20`) ; `HexagonalArchitectureTest` et `HorlogeArchitectureTest` doivent rester verts (pas de `now()` hors adaptateurs d'horloge).
- `compose.yaml` -- le service `notifications` dépend déjà de `coeur-bancaire` healthy ; aucun changement attendu.
- Sources de vérité : PRD FR-002-09/10, FR-003-10/14/26, FR-023-03/05/06, FR-025-01/02/04 ; `ARCHITECTURE-SPINE.md` AD-12, AD-18, AD-22, AD-25 ; `docs/retro/00-global/{MESSAGES,LEGACY-BUGS}.md` (BUG-006, BUG-007).

## Tasks & Acceptance

**Execution:**
- `services/coeur-bancaire/src/main/java/**/domain/**`, `application/**` -- `VirementDecide`, type et rendu de notification, ports, 2e ligne d'outbox dans la transaction de décision -- décision de contenu côté producteur
- `services/coeur-bancaire/src/main/java/**/adapter/out/evenements/**`, `infrastructure/ConfigurationEvenements.java` -- enveloppe, `consigner(VirementDecide)`, topic -- publication par l'outbox
- `contrats/evenements/coeur-bancaire.virement-decide.v1.schema.json` -- schéma et exemple -- contrat d'événement
- `services/notifications/**` -- récepteur, cas d'usage idempotent, dépôt JDBC avec plafond, `V2`, configuration validée -- stockage persistant
- Tests coeur-bancaire (`@Tag` `FR-002-10`, `FR-003-10`, `FR-003-14`, `FR-003-26`, `FR-023-03`, `FR-025-01`, `FR-025-02`) : `ExecuterVirementCasUsageTest` (messages avec/sans/espaces, 5 000,00 vs 4 999,99, une seule ligne `virement-decide`), `*ContratEvenement*Test`, `FR-023-03`
- Tests notifications (`@Tag` `FR-002-09`, `FR-023-05`, `FR-023-06`, `FR-025-04`, `BUG-006`, `BUG-007`) : contrat consommateur, stockage tel que rendu en tête et non lu, double livraison (Testcontainers), plafond 20/21, redémarrage sur la même base, `nb.notifications.plafond` 0 ou négatif refusé ; couverture 100 % lignes et branches sur `domain` et `application`

**Acceptance Criteria:**
- Given la story 3.3 d'`epics.md`, when chacun de ses critères est exécuté, then il est satisfait tel que rédigé
- Given `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-002-10 | FR-003-10 | FR-003-14 | FR-003-26 | FR-023-03 | FR-025-01 | FR-025-02'`, `mvn -B -q -f services/notifications/pom.xml verify -Dgroups='FR-002-09 | FR-023-05 | FR-023-06 | FR-025-04'` et `... -Dgroups='BUG-006 | BUG-007'`, when lancés, then code de sortie 0 chacun
- Given `! grep -rn 'nb\.fraude\.' services/notifications/src/main`, when lancé, then code de sortie 0
- Given `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` et `trace-smoke.sh`, when lancés, then code de sortie 0

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 5 findings — high 0, medium 0, low 2, false 3, maybe-false 0
- findings:
  - `[low]` `[reject]` aucun test ne traverse le topic entre un événement réel du producteur et le consommateur — les deux côtés sont vérifiés contre le même schéma (`examples[0]`) ; les critères d'epics.md n'exigent pas de test inter-services, et un test inter-services ajouterait un montage lourd.
  - `[low]` `[reject]` pas de contrainte d'unicité sur `virement_id` dans `notifications` — un seul événement est produit par virement et l'idempotence repose sur `evenements_traites` (BUG-007) ; ajouter la contrainte compliquerait le futur (rejeu 3.4) sans cas atteignable.
  - `[false]` `[reject]` « redémarrage » testé dans le même processus — le critère demande « sur la même base » ; le test relance un contexte sur la même base Postgres, ce qui prouve la persistance (BUG-006).
  - `[false]` `[reject]` notification émise à l'exécution et non à une « décision » plus large — seul l'état EXÉCUTÉ existe jusqu'à la story 3.4 (epic-3-context), qui ajoutera les autres issues.
  - `[false]` `[reject]` sorties des contrôles absentes du diff — `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `grep nb.fraude.` et le grep des spans relancés à la vérification : code 0.

## Design Notes

- Le `titre` n'a pas de texte source (legacy sans titre pour les virements, aucune entrée dans `textes-nouveaux.md`) : il est `null` dans l'événement et stocké nullable ; point ouvert consigné dans `deferred` pour le métier.
- Seul l'état EXÉCUTÉ existe jusqu'à la story 3.4 ; le type FRAUDE et les autres états s'ajoutent alors, la forme de l'événement ne change pas.
- La notification est dans la transaction de décision du cœur bancaire, jamais dans la remise asynchrone : c'est ce qui garantit une seule notification par virement.

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml verify` et `mvn -B -q -f services/notifications/pom.xml verify` -- expected: code 0
- les trois commandes `-Dgroups` de la section Acceptance -- expected: code 0
- `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` -- expected: code 0
- `! grep -rnE 'Observation\.(createNotStarted|start)\(|@Observed|@NewSpan|spanBuilder\(|nextSpan\(' services/*/src/main --include=*.java` -- expected: aucune ligne

## Auto Run Result

Status: done

**Résumé :** un virement exécuté écrit, dans la transaction du débit, une ligne d'outbox `coeur-bancaire.virement-decide.v1` portant la notification rendue par le cœur bancaire (CONFIRMATION avec ou sans motif, ALERTE dès `seuilAlerte`). Le service `notifications` la consomme de façon idempotente, la stocke non lue en tête de liste, applique le plafond `nb.notifications.plafond` (20, validé au démarrage) et la conserve en base (`V2`). La remise asynchrone ne notifie rien.

**Fichiers :** `contrats/evenements/coeur-bancaire.virement-decide.v1.schema.json` (contrat) ; `services/coeur-bancaire/**` (domaine `VirementDecide`, `NotificationVirement`, `TypeNotification`, textes, cas d'usage, enveloppe et outbox Kafka, topic, tests) ; `services/notifications/**` (récepteur, cas d'usage, `NotificationsJdbc`, `PlafondNotifications`, migration `V2`, propriété, tests).

**Revue :** 0 patch ; 1 élément différé (titre des notifications de virement sans texte défini, laissé à `null`) ; 5 rejetés (2 low sans cas atteignable, 3 faux), motifs au journal de triage. Recommandation de nouvelle revue : `false`.

**Vérification :** les trois `mvn verify -Dgroups` (coeur-bancaire, notifications, BUG-006|BUG-007) et les `mvn verify` complets code 0 par le sous-agent ; relancés ici : `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` code 0, `grep nb.fraude.` et grep des spans manuels vides. `trace-smoke.sh` code 0 par le sous-agent.

**Risques résiduels :** `titre` nul tant que le métier n'a pas fixé un texte (avant l'affichage 3.9) ; le schéma limite `etat` à `EXECUTE` et `type` à CONFIRMATION/ALERTE : la story 3.4 devra les élargir ; des `@Tag` ont été ajoutés à des tests existants pour que le contrôle JaCoCo passe sous `-Dgroups`.
