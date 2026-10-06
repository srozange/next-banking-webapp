---
title: 'Publier l''état des clients pour les autres services'
type: 'feature'
created: '2026-10-06'
status: 'done'
baseline_revision: '553f94b0ca4c76946de30fea98e71912d9ce851c'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-2-context.md'
warnings: []
deferred: []
---

<intent-contract>

## Intent

**Problem:** `identite-clients` change l'état d'un client (désactivation, activation) sans en avertir `coeur-bancaire`, `credits` ni `documents`, qui ne peuvent ni tenir de projection locale ni lire sa base.

**Approach:** à chaque changement, une ligne de la table `outbox` est écrite dans la même transaction que `customers`, puis publiée par `KafkaTemplate` après validation (relais chaque seconde, ordre des identifiants) sur `identite-clients.client-modifie.v1`, avec l'état complet du client et un `versionAgregat` croissant ; le contrat est un schéma JSON versionné.

## Boundaries & Constraints

**Always:** clé Kafka = `clientId` (texte), `StringSerializer`, sans en-tête `__TypeId__` ; enveloppe `idEvenement`, `type` (`client-modifie`), `version` (1), `horodatage` (UTC, port `Horloge`), `producteur` (`identite-clients`), `donnees` ; `application` en Java pur, transactions par le port `Transaction` ; topic déclaré par un bean `NewTopic` (3 partitions, réplication 1) dans `infrastructure` ; tests tagués FR-016-11.

**Never:** modifier `scripts/gates/**`, seuils ou exclusions ; rejouer le golden master ; publier hors outbox ; lire la base d'un autre service ; consommateur côté autres services (stories à venir) ; texte inventé.

## I/O & Edge-Case Matrix

| Scénario | Entrée / état | Comportement attendu | Erreur |
|----------|--------------|---------------------|--------|
| Désactivation | ADMIN désactive `user1` | ligne `outbox` puis événement `actif=false`, `versionAgregat` > précédent | — |
| Activation qui suit | ADMIN réactive `user1` | événement `actif=true`, `versionAgregat` encore supérieur | — |
| Transaction annulée | écriture de `active` en échec | aucune ligne `outbox`, aucun événement | l'erreur remonte |
| Kafka indisponible | ligne non publiée | le relais la publie puis la marque publiée, dans l'ordre | publication journalisée, ligne laissée |
| Champ retiré | `conseillerId` absent de l'événement produit | le test de contrat échoue en nommant le champ | — |

</intent-contract>

## Code Map

- `services/identite-clients/src/main/resources/db/migration/V2__outbox_audit.sql` -- modèle de la table ; ajouter `V3__client_modifie.sql` : table `outbox` et colonnes `customers.date_creation` (défaut `current_date`), `supprime` (faux), `version_agregat` (1)
- `.../application/ChangerEtatClientCasUsage.java` -- ajoute la consignation de l'événement (même transaction que `changerActif`) et sa publication après validation ; `.../infrastructure/AssemblageConnexion.java` -- assemblage
- `.../application/port/out/JournalAudit.java` -- modèle du nouveau port `PublicationEtatClient` (`consigner(clientId, idEvenement, horodatage)`, `publier(idLigne)`)
- `.../adapter/out/persistance/DepotClientsJdbc.java` -- `changerActif` incrémente `version_agregat`
- `.../adapter/out/evenements/{JournalAuditKafka,RelaisOutboxAudit,EnveloppeAudit,LigneOutboxAudit}.java` -- modèles de `PublicationEtatClientKafka`, `RelaisOutboxClients`, `EnveloppeClientModifie`, `LigneOutbox`
- `.../infrastructure/ConfigurationEvenements.java` -- bean `NewTopic` du nouveau topic
- `contrats/evenements/identite-clients.audit.v1.schema.json` -- modèle du nouveau schéma (avec exemple)
- Tests : `ChangerEtatClientCasUsageTest`, `ActivationClientIT` (construisent le cas d'usage), `AuditContratEvenementTest` et `AuditKafkaIT` (modèles du contrat et du relais), `LecteurTopic`, `LignesAudit`

## Tasks & Acceptance

**Execution:**
- `contrats/evenements/identite-clients.client-modifie.v1.schema.json` -- schéma et exemple -- contrat d'AC 1
- `services/identite-clients/**` (migration, port, adapter, relais, cas d'usage, topic, assemblage) -- publication par outbox -- AC 2 à 4
- `ChangerEtatClientCasUsageTest`, `ClientModifieContratEvenementTest`, `PublicationEtatClientKafkaTest`, `ClientModifieKafkaIT` -- tests tagués FR-016-11 couvrant la matrice (100 % lignes et branches sur `application`)

**Acceptance Criteria:**
- Given le schéma, when je le lis, then il décrit l'enveloppe et `clientId`, `login`, `nomComplet`, `role`, `actif`, `conseillerId`, `dateCreation`, `supprime`, `versionAgregat`, avec un exemple ; le topic est un bean `NewTopic` (3 partitions, réplication 1)
- Given la désactivation puis l'activation de `user1` par `admin`, when la transaction est validée, then l'événement est publié sur la clé `clientId`, sans `__TypeId__`, état complet, `versionAgregat` strictement croissant
- Given une transaction annulée, then ni ligne `outbox` ni événement
- Given une ligne non publiée, when le relais s'exécute, then elle est publiée puis marquée, dans l'ordre
- Given la story terminée, then `build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh` en code 0

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 2, false 2, maybe-false 0
- findings:
  - `[low]` `[reject]` le test de contrat sérialise un `Donnees` construit dans le test, pas la requête SQL de `consigner` — la requête est couverte de bout en bout par `ClientModifieKafkaIT` (lecture de l'événement réel sur le topic) ; garde supplémentaire disproportionnée.
  - `[false]` `[reject]` « aucun événement » à l'annulation testé seulement par un faux — `ActivationClientIT` vérifie l'absence de ligne `outbox` en base réelle, et `publier` n'est appelé qu'après validation.
  - `[low]` `[reject]` ordre du relais testé sur mocks, période d'une seconde non exercée — l'ordre vient de `order by id`, la période de `@Scheduled(fixedDelay = 1000)` ; la publication par le relais réel est testée en IT.
  - `[false]` `[reject]` portée « à chaque changement » limitée à l'activation/désactivation — ce sont les seuls changements d'état existants dans le service ; la création et la modification viendront avec leurs stories.

## Design Notes

`dateCreation` n'existe pas dans le legacy : la colonne reçoit `current_date` à la migration pour les clients existants (valeur de référence, pas une date métier). `supprime` reste faux : aucune suppression n'existe. Le cas d'usage ne transporte que `clientId`, l'identifiant et l'horodatage ; l'adaptateur relit l'état après l'écriture, dans la même transaction.

## Verification

**Commands:**
- `mvn -B -q -f services/identite-clients/pom.xml test -Dtest='*ContratEvenement*Test'` -- expected: code 0
- `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-016-11'` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** l'état complet d'un client est publié sur `identite-clients.client-modifie.v1` à chaque désactivation ou activation : ligne `outbox` écrite dans la transaction de `customers`, publiée après validation par `KafkaTemplate` (clé `clientId`, sans `__TypeId__`), relais chaque seconde dans l'ordre des identifiants, `versionAgregat` incrémenté à chaque changement.

**Fichiers :**
- `contrats/evenements/identite-clients.client-modifie.v1.schema.json` : schéma et exemple.
- `services/identite-clients/src/main/resources/db/migration/V3__client_modifie.sql` : table `outbox`, colonnes `date_creation`, `supprime`, `version_agregat`.
- `.../application/port/out/PublicationEtatClient.java`, `.../application/ChangerEtatClientCasUsage.java`, `.../infrastructure/AssemblageConnexion.java` : port et cas d'usage.
- `.../adapter/out/evenements/{PublicationEtatClientKafka,RelaisOutboxClients,EnveloppeClientModifie,LigneOutbox}.java`, `.../adapter/out/persistance/DepotClientsJdbc.java`, `.../infrastructure/ConfigurationEvenements.java` : adaptateurs, relais, topic.
- Tests tagués FR-016-11 : `ClientModifieContratEvenementTest`, `PublicationEtatClientKafkaTest`, `ClientModifieKafkaIT`, `ChangerEtatClientCasUsageTest`, `ActivationClientIT`.

**Revue :** 0 patch, 0 report ; 4 constats rejetés (2 faux, 2 négligeables), motifs dans le journal de triage. Relecture complémentaire non recommandée.

**Vérification :** `mvn test -Dtest='*ContratEvenement*Test'` et `-Dgroups='FR-016-11'` en code 0 ; `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` en code 0.

**Risques résiduels :** `dateCreation` vaut la date de migration pour les clients existants (le legacy n'en a pas) ; le premier événement d'un client porte `versionAgregat` = 2.
