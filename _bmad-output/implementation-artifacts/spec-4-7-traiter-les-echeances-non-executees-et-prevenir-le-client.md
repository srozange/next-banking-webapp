---
title: 'Traiter les échéances non exécutées et prévenir le client'
type: 'feature'
created: '2026-10-08'
status: 'done'
baseline_revision: 'e44a4eae4392c4098fd136013ab0ce05962f4ae7'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-4-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-6-executer-automatiquement-les-echeances-dues-des-virements-programmes.md'
  - '{project-root}/docs/quality-gates.md'
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** Depuis la story 4.6, une échéance dont le solde est insuffisant ou dont le compte destinataire n'existe plus est ignorée en silence : le client n'est pas prévenu, et le virement programmé vers un compte introuvable est retenté à chaque passage.

**Approach:** Le cœur bancaire publie, par l'outbox, un événement `coeur-bancaire.echeance-non-executee.v1` portant une notification ALERTE rendue (une seule par échéance, comptée dans sa base) ; le refus « compte introuvable » (évalué avant le solde) désactive en plus le virement programmé. Le service `notifications` le consomme de façon idempotente et le centre de notifications affiche désormais le titre.

## Boundaries & Constraints

**Always:** Hexagone (domain/application Java pur), temps par `Horloge`, entre services uniquement des identifiants numériques, aucun appel HTTP entre services.
- **Contrat** : `contrats/evenements/coeur-bancaire.echeance-non-executee.v1.schema.json` sur le modèle de `coeur-bancaire.virement-decide.v1.schema.json` (enveloppe `idEvenement`, `type` = `echeance-non-executee`, `version` 1, `horodatage`, `producteur`, `donnees` = `{virementProgrammeId, notification{clientId, type, titre, message}}`, clé Kafka = `virementProgrammeId`, `type` de notification = `ALERTE`, `titre` obligatoire non nul), avec un exemple. Topic `coeur-bancaire.echeance-non-executee.v1` déclaré par un bean `NewTopic` dans `ConfigurationEvenements`, publié par l'outbox (`EvenementsSortants.consigner(EcheanceNonExecutee)` dans la transaction, `publier` après validation).
- **Textes** (repris à l'identique de `_bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/textes-nouveaux.md`, lot « Stories », clés `vp.notif.solde.*` et `vp.notif.introuvable.*`, apostrophes et accents compris ; `{beneficiaire}` = `libelleBeneficiaire` du virement programmé) : solde insuffisant — titre « Virement programmé non exécuté », message « Virement programmé vers {beneficiaire} non exécuté : solde insuffisant, rien n'a été débité » ; compte introuvable — titre « Virement programmé refusé », message « Virement programmé vers {beneficiaire} refusé : compte destinataire introuvable, rien n'a été débité ». Les deux sont de type ALERTE. Rendus dans `TextesVirement` / `NotificationVirement` (producteur) ; `notifications` ne compose aucun texte.
- **Cœur bancaire, ordre d'une échéance** (`ExecuterEcheancesCasUsage`) : numéro du bénéficiaire → compte absent : **refus « compte introuvable » avant tout contrôle du solde** : dans la transaction, désactivation conditionnelle du virement programmé (`actif` passe à faux si `id`, `actif` et `prochaine_execution = échéance prévue`), marque de notification, événement consigné dans l'outbox ; aucune écriture de compte, d'opération ni de virement ; publication après validation ; log WARN « Echec execution virement programme id={id} » conservé, non compté en échec du passage. → compte présent : comme en 4.6. **Solde insuffisant** : la transaction d'exécution est annulée comme en 4.6 (solde, opérations, état, compteur et date de prochaine exécution inchangés, contrôle de fraude non appelé, log WARN « Solde insuffisant pour virement programme id={id} », non compté), puis, dans une **seconde transaction**, marque de notification et événement consigné ; l'échéance reste due et est retentée au passage suivant.
- **Une seule notification par échéance** : nouvelle table du cœur bancaire (migration Flyway `V8`) `echeances_notifiees (virement_programme_id bigint, echeance timestamptz, notifiee_le timestamptz, clé primaire (virement_programme_id, echeance))` ; port `VirementsProgrammes` : `boolean marquerNotifiee(long id, Instant echeance, Instant le)` (insert `on conflict do nothing`, vrai si une ligne insérée) et `boolean desactiver(long id, Instant echeancePrevue)` ; l'événement n'est consigné que si `marquerNotifiee` rend vrai. Un second passage sur la même échéance n'en publie donc pas d'autre ; au passage où le solde suffit, l'échéance est exécutée normalement (sans notification).
- **Isolation (FR-021-09, FR-004-15, BUG-070)** : un échec simulé sur une échéance annule complètement celle-ci (ni débit, crédit, opération, avance), les autres sont exécutées, et le log WARN « Echec execution virement programme id={id} » est écrit (déjà en place en 4.6 : le prouver par un test 3 virements dont le 2ᵉ échoue).
- **Notifications** : nouveau récepteur Kafka `@KafkaListener(topics = "coeur-bancaire.echeance-non-executee.v1")` (message = chaîne JSON sans en-tête de type, modèle `RecepteurVirementDecide`), port entrant et cas d'usage idempotent (`evenements_traites`, plafond 20 par utilisateur, comme `RecevoirVirementDecideCasUsage`), notification stockée pour le `clientId` du bloc `notification`, titre et message tels que rendus. Migration `V3` de `notifications` : `virement_id` devient nullable et une colonne `virement_programme_id bigint` nullable est ajoutée ; `domain.Notification` porte `Long virementId` ou `Long virementProgrammeId` (exactement un des deux non nul). Le titre est relu et servi : `NotificationDuClient` et le JSON `NotificationJson` gagnent `titre` (nul pour les notifications de virement).
- **Front** : `NotificationBrute`/`NotificationAffichee` gagnent `titre: string | null` (validation du client HTTP : `string` ou `null`) ; `SectionCentreNotifications` affiche le titre quand il est non nul, dans un élément `data-testid="notifications-titre-{id}"` placé avant le message ; aucun changement visible quand il est nul (golden master intact). Tests Vitest à jour, couverture inchangée.

**Never:** Pas de notification pour une échéance exécutée ni pour un refus par erreur du contrôle (nouvelle tentative au passage suivant, comptée en échec) ; pas de traitement de la suspension par la fraude (story 4.8 : l'échéance suspendue garde son comportement de 4.6) ; pas de texte inventé ; pas de journal de nuit hors `RECURRING_TRANSFERS` ; pas d'appel HTTP entre services ; pas de rejeu du golden master ; ne pas modifier scripts/gates/**, golden master, `replay-mapping.json`, seuils ni exclusions de couverture ; pas de `now()`/`Clock` hors des adaptateurs d'horloge ; pas de span créé à la main ; pas de mode `reel`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Erreur |
|----------|--------------|----------------------|--------|
| Solde insuffisant | actif dû, 500.00, source à 499.99 | rien d'écrit sur les comptes ; échéance inchangée ; log WARN ; contrôle de fraude non appelé ; 1 événement ALERTE « non exécuté » | — |
| Second passage | même échéance, solde toujours insuffisant | aucun nouvel événement | — |
| Solde rétabli | même échéance, solde suffisant | échéance exécutée, aucune notification | — |
| Compte introuvable | numéro absent, solde source insuffisant | solde inchangé, aucune opération, 1 événement ALERTE « refusé », virement programmé inactif | — |
| Échec isolé | 3 dus, le 2ᵉ en erreur | 1ᵉʳ et 3ᵉ exécutés, 2ᵉ intact, log « Echec execution… » | échec isolé |
| Doublon Kafka | même enveloppe livrée deux fois | une seule notification stockée pour `clientId` | 2ᵉ livraison ignorée |

</intent-contract>

## Code Map

Cœur bancaire sous `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/` (`M/`), tests sous `…/src/test/java/fr/nextbanking/coeurbancaire/` (`T/`) ; notifications sous `services/notifications/src/main/java/fr/nextbanking/notifications/` (`N/`) et `…/src/test/java/…` (`NT/`).

- `M/application/ExecuterEcheancesCasUsage.java` -- `ecrire` rend null pour « destinataire introuvable » et lève `ErreurVirement(SOLDE_INSUFFISANT)` : y brancher refus + désactivation + notification (même transaction) et notification après annulation (seconde `transaction.executer`), puis publication après validation ; ajouter les ports `EvenementsSortants` au constructeur.
- `M/application/port/out/EvenementsSortants.java`, `M/adapter/out/evenements/EvenementsSortantsKafka.java` (modèle `consigner(VirementDecide)`), nouveau `M/adapter/out/evenements/EnveloppeEcheanceNonExecutee.java` (modèle `EnveloppeVirementDecide`), `M/infrastructure/ConfigurationEvenements.java` (bean `NewTopic`), `M/infrastructure/AssemblageVirementsProgrammes.java` (injecter `EvenementsSortants`).
- `M/domain/` : nouveau `EcheanceNonExecutee` (modèle `VirementDecide` : `idEvenement`, `virementProgrammeId`, `NotificationVirement`, `horodatage`) ; `NotificationVirement` (fabriques `pourEcheanceSoldeInsuffisant`, `pourEcheanceCompteIntrouvable`, titre non nul) ; `TextesVirement` (quatre textes ci-dessus).
- `M/application/port/out/VirementsProgrammes.java` + `M/adapter/out/persistance/VirementsProgrammesJdbc.java` -- `marquerNotifiee`, `desactiver` ; `M/resources/db/migration/V8__echeances_notifiees.sql`.
- `contrats/evenements/coeur-bancaire.echeance-non-executee.v1.schema.json` (avec exemple).
- `N/adapter/in/RecepteurEcheanceNonExecutee.java` (modèle `RecepteurVirementDecide`), `N/application/port/in/RecevoirEcheanceNonExecutee.java` + `N/application/RecevoirEcheanceNonExecuteeCasUsage.java` (modèle `RecevoirVirementDecideCasUsage`, éventuellement factorisé), `N/infrastructure/AssemblageReceptionVirementDecide.java` (ou un assemblage voisin), `N/domain/Notification.java`, `N/domain/NotificationDuClient.java`, `N/adapter/out/persistance/NotificationsJdbc.java`, `N/adapter/in/http/PointEntreeNotifications.java` (`NotificationJson.titre`), `services/notifications/src/main/resources/db/migration/V3__notifications_d_echeance.sql` (V1–V2 existent).
- Front : `frontend/src/domain/notifications.ts`, `frontend/src/adapters/http/ClientNotificationsHttp.ts`, `frontend/src/adapters/ecrans/notifications/SectionCentreNotifications.tsx` (+ `.css` si besoin, tokens uniquement) et leurs tests ; `frontend/src/adapters/test/rendreApplication.tsx` si des données de test s'y trouvent.
- Tests existants à mettre à jour (comportement de 4.6 remplacé) : `T/application/ExecuterEcheancesCasUsageTest`, `T/application/MemoireCoeurBancaire` (implémenter `marquerNotifiee`, `desactiver`, le nouvel événement ; rollback), `T/adapter/EcheancesIT`, `T/adapter/EcheanceTransitionConcurrenteTest` ; côté notifications `NotificationTest`, `NotificationsHttpIT` et tout test qui construit `Notification`/`NotificationDuClient`/`NotificationJson`.
- Nouveaux tests : `T/adapter/out/evenements/EcheanceNonExecuteeContratEvenementTest` (producteur ; modèle `VirementDecideContratEvenementTest`) ; `NT/adapter/in/EcheanceNonExecuteeContratEvenementTest` (consommateur : enveloppe brute) et `NT/…/ReceptionEcheanceNonExecuteeTest` (Testcontainers, livraison double, modèle `ReceptionVirementDecideTest`) ; `T/domain/NotificationVirementTest`/`TextesVirement` ; `T/adapter/out/persistance/VirementsProgrammesJdbcIT` (`marquerNotifiee`, `desactiver`).
- Lecture seule : `docs/retro/features/FEAT-021/{RG,MESSAGES}.md`, golden master, `scripts/gates/**`.

## Tasks & Acceptance

**Execution:**
- `contrats/evenements/…echeance-non-executee.v1.schema.json`, `ConfigurationEvenements`, enveloppe, `EvenementsSortants*`, `EcheanceNonExecutee` -- contrat, topic, outbox -- FR-004-13, AR-29/31
- `NotificationVirement`, `TextesVirement` -- notifications ALERTE rendues -- FR-004-12, FR-004-13
- `VirementsProgrammes` + Jdbc + `V8` -- `marquerNotifiee`, `desactiver` -- A-004-2, A-021-3
- `ExecuterEcheancesCasUsage`, `AssemblageVirementsProgrammes` -- refus avant solde, notification unique, isolation -- FR-004-12, FR-021-04, FR-021-09, FR-004-15, BUG-014/015/068/070
- notifications (récepteur, cas d'usage, domaine, persistance, HTTP, `V4`) -- consommation idempotente et titre servi -- FR-021-04, FR-004-12
- Front (domaine, client HTTP, section) -- affichage du titre -- UX-DR55
- Tests listés dans la Code Map -- 100 % lignes et branches sur domain/application, 80 % adapter/infrastructure ; étiquettes `@Tag("FR-004-12")`, `FR-004-13`, `FR-004-15`, `FR-021-04`, `FR-021-09`, `BUG-014`, `BUG-015`, `BUG-068`, `BUG-070` selon ce qu'ils prouvent (notifications : `FR-004-12`, `FR-021-04`)

**Acceptance Criteria:**
- Given le schéma et son exemple, when `test -f contrats/evenements/coeur-bancaire.echeance-non-executee.v1.schema.json` et les tests `*ContratEvenement*Test` des deux services, then code 0, topic déclaré par `NewTopic` et publié par l'outbox
- Given un virement programmé actif et dû de 500 € sur une source à 499,99 €, when un passage s'exécute, then solde, opérations, état, compteur et date inchangés, journal « Solde insuffisant pour virement programme id={id} », contrôle de fraude non appelé, exactement un événement ALERTE ; un second passage n'en publie pas ; le passage où le solde suffit exécute l'échéance
- Given un virement actif dû vers un numéro absent et un solde insuffisant, when un passage s'exécute, then le refus « compte introuvable » est appliqué avant le solde : solde inchangé, aucune opération, une notification de refus, virement inactif
- Given trois virements dus dont le deuxième échoue, when le passage s'exécute, then le premier et le troisième sont exécutés, le deuxième intact, avertissement « Echec execution virement programme id={id} »
- Given une enveloppe brute livrée deux fois à `notifications`, when elle est consommée, then une seule notification est stockée pour le `clientId` du bloc `notification` et le centre de notifications l'affiche avec son titre et son texte
- Given la story terminée, when `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-004-12|FR-004-13|FR-004-15|FR-021-04|FR-021-09|BUG-014|BUG-015|BUG-068|BUG-070'` puis `mvn -B -q -f services/notifications/pom.xml test -Dgroups='FR-004-12|FR-021-04'`, then code 0
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` (et `trace-smoke.sh` si la pile compose est disponible), then code 0

## Spec Change Log

## Review Triage Log

### 2026-10-08 — Review pass
- verdicts: 2 findings — high 0, medium 0, low 2, false 0, maybe-false 0
- findings:
  - `[low]` `[reject]` aucun test ne parcourt la chaîne complète producteur, Kafka, récepteur, API et écran — chaque couche est prouvée séparément contre le même schéma et le même exemple (contrats des deux côtés, `ReceptionEcheanceNonExecuteeTest` Kafka et base réels, `NotificationsHttpIT`, tests Vitest) ; un test bout en bout exigerait la pile compose complète pour une preuve que `trace-smoke.sh` apporte déjà pour le chemin Kafka.
  - `[low]` `[reject]` « fraude non appelée » et le message sur Kafka réel ne sont prouvés qu'indirectement (journal du double en mémoire, lecture de la colonne `charge` de l'outbox) — la publication de l'outbox vers Kafka est la mécanique commune éprouvée aux stories précédentes ; un test de plus n'apporterait pas de preuve nouvelle.

## Design Notes

La notification du solde insuffisant est écrite dans une seconde transaction parce que la première est annulée pour restituer l'échéance intacte (prise conditionnelle comprise). La marque `echeances_notifiees` (échéance = `prochaine_execution` non avancée) est la clé de l'unicité : la clé primaire absorbe deux passages concurrents. Le cas « compte introuvable » n'annule rien : désactivation, marque et événement partent dans la même transaction. Le rejet pour erreur du contrôle et la suspension par la fraude (4.8) ne passent pas par cette notification. Le plafond de 20 notifications par utilisateur reste appliqué par `notifications`.

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-004-12|FR-004-13|FR-004-15|FR-021-04|FR-021-09|BUG-014|BUG-015|BUG-068|BUG-070'` -- expected: code 0
- `mvn -B -q -f services/notifications/pom.xml test -Dgroups='FR-004-12|FR-021-04'` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** une échéance de virement programmé non exécutée prévient désormais le client. Compte destinataire introuvable : refus évalué avant le solde, virement programmé désactivé, une notification ALERTE « Virement programmé refusé ». Solde insuffisant : échéance intacte et retentée, une notification ALERTE « Virement programmé non exécuté » (seconde transaction après annulation). Une seule notification par échéance (table `echeances_notifiees`). L'événement `coeur-bancaire.echeance-non-executee.v1` passe par l'outbox ; `notifications` le consomme de façon idempotente ; le titre est servi par l'API et affiché dans le centre de notifications.

**Fichiers :** schéma `contrats/evenements/coeur-bancaire.echeance-non-executee.v1.schema.json` ; cœur bancaire (`ExecuterEcheancesCasUsage`, `EcheanceNonExecutee`, `NotificationVirement`, `TextesVirement`, `EnveloppeEcheanceNonExecutee`, `EvenementsSortantsKafka`, `ConfigurationEvenements`, `VirementsProgrammes`/`VirementsProgrammesJdbc`, migration `V8`) ; notifications (`RecepteurEcheanceNonExecutee`, `RecevoirEcheanceNonExecuteeCasUsage`, `Notification`, `NotificationDuClient`, `NotificationsJdbc`, `PointEntreeNotifications`, migration `V3`) ; front (`notifications.ts`, `ClientNotificationsHttp.ts`, `SectionCentreNotifications`) ; tests associés.

**Revue :** 2 constats — 0 patch, 0 reporté, 2 rejetés (négligeables). Relance recommandée : non.

**Vérification :** `mvn … -Dgroups='FR-004-12|…|BUG-070'` (cœur bancaire) et `mvn … -Dgroups='FR-004-12|FR-021-04'` (notifications) code 0 ; `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` code 0.

**Risques résiduels :** `trace-smoke.sh` non lancé (pile compose non démarrée) ; la suspension par la fraude (story 4.8), le motif de plus de 100 caractères et l'attribut `virement.controle.issue` restent ouverts (reportés de la story 4.6) ; si la désactivation conditionnelle est perdue, le refus « compte introuvable » est sans effet et sans notification.
