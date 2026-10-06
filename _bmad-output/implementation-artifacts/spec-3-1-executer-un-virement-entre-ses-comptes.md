---
title: 'Exécuter un virement entre ses comptes'
type: 'feature'
created: '2026-10-06'
baseline_revision: 'df78eaeaf10d9dc27a1132a8e24e1817de6f94e2'
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
      Données de référence incomplètes : seuls les comptes 1 et 2 de user1 sont documentés dans docs/retro.
    evidence: |-
      Les 5 autres comptes du legacy et les 45 opérations (dont les 5 dernières du COURANT de user1, AR-80) ne figurent pas dans docs/retro ; le solde de l'EPARGNE (15 200,00) est déduit du total de FEAT-002. À compléter par la story « Socle du rejeu final ».
    location: >-
      services/coeur-bancaire/reference-data/reference-data.sql
    severity: medium
  - summary: >-
      Texte « Issue » de l'audit VIREMENT quand la remise est impossible (BUG-073) : ECHEC_REMISE est un choix provisoire.
    evidence: |-
      FR-026-03 ne fixe que EXECUTE, SUSPENDU et REFUSE_ERREUR_CONTROLE ; un texte manquant est un point ouvert à confirmer.
    severity: low
  - summary: >-
      Format de HORLOGE_DECALAGE (durée ISO-8601) à confirmer au premier rejeu.
    evidence: |-
      Retenu sans vérification en compose (make rejeu-up) ; à confirmer par la story « Socle du rejeu final ».
    severity: low

---

<intent-contract>

## Intent

**Problem:** le cœur bancaire n'est qu'un squelette « témoin » : ni comptes, ni opérations, ni virement, ni écran `/virement`. Un client ne peut pas déplacer de l'argent entre ses comptes.

**Approach:** livrer en une fois le virement immédiat entre comptes d'un même client : API `POST /api/coeur-bancaire/virements` (contrôles, débit/crédit atomiques, outbox, audit, idempotence), données de référence du legacy, écran `/virement` du front, horloge de rejeu, contrat d'événement d'audit. Le texte exact de chaque critère est la story 3.1 de `_bmad-output/planning-artifacts/epics.md` (lignes 2204-2315) : elle fait foi, la lire en entier.

## Boundaries & Constraints

**Always:** reprendre les 22 critères d'acceptation de la story 3.1 d'`epics.md` (source de vérité de la matrice ci-dessous) ; architecture hexagonale (`domain`/`application` Java pur, 100 % lignes et branches) ; montants `BigDecimal` 2 décimales, `numeric(19,2)`, chaîne `"100.00"` en JSON, front en centimes entiers ; temps uniquement par le port `Horloge` ; écritures liées à une publication par `outbox` ; consommateurs idempotents (`evenements_traites`) ; textes du virement dans un seul fichier du cœur bancaire (`Textes.java`), textes legacy de `docs/retro/00-global/MESSAGES.md` et `docs/retro/features/FEAT-003/FIELDS.md`, nouveaux de `textes-nouveaux.md` (aucun texte inventé : un texte manquant est consigné dans `deferred`) ; contrôle de propriété du compte source dans le service ; IBAN masqué dans les journaux ; `data-testid` `virement-*` ; couleurs par tokens ; tests tagués `FR-003-xx`, `FR-023-0x`, `FR-026-03`, `BUG-008`, `BUG-011`, `BUG-073` pour que les `-Dgroups` / `vitest -t` de la story les trouvent ; seuils `nb.fraude.*` validés au démarrage (`ConfigurationFraude*Test`) ; versions de `versions-verifiees.md`.

**Never:** modifier `scripts/gates/**`, `docs/quality-gates.md`, `.claude/**`, `.bmad-loop/**`, `frontend/.dependency-cruiser.cjs`, seuils, exclusions de couverture ou de mutation ; désactiver un test ; toucher `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json` ; rejouer le golden master ; évaluer les signaux de fraude (story 3.2), notifier (3.3), suspendre/refuser/libérer (3.4), relevé (3.7), tableau de bord réel (3.8) ; appel HTTP entre services ; écrire `sprint-status.yaml` ; reprendre telle quelle la tentative préservée (`refs/attempt-preserve-dirty/...`, non vérifiée, majoritairement hors périmètre : `nb.reseau.*` ne relève pas de la 3.1) — on peut en réutiliser des morceaux après les avoir jugés contre cette spec.

## I/O & Edge-Case Matrix

| Scénario | Entrée / état | Comportement attendu | Erreur |
|----------|--------------|---------------------|--------|
| Écran | `user1` ouvre `/virement` | titre, onglet, deux `<select>` (« -- Choisir un compte -- » + comptes de `user1`), montant avec indice, motif, bouton, lien SEPA, pas de bandeau EJB/ActiveMQ | — |
| Sans compte propre | CONSEILLER/ADMIN | listes réduites à « -- Choisir un compte -- » | — |
| Comptes manquants / montant manquant | soumission vide | « Veuillez sélectionner les deux comptes » puis « Veuillez saisir le montant », `aria-invalid`, `aria-describedby`, focus ; API : 400 `{"message":"Veuillez sélectionner les deux comptes"}` | aucune écriture |
| Comptes identiques | `1` → `1`, 100 | « Le compte source et destination doivent être différents » dans `virement-message-erreur`, valeurs conservées | aucune écriture |
| Montant invalide | 0, -5, « abc » | 400 `{"message":"Erreur lors du virement : Le montant doit etre positif"}` | aucun solde change |
| Motif long | motif > 100 caractères | 400 « Le motif ne doit pas dépasser 100 caractères » (`virement.validation.motif.longueur`) | aucune écriture |
| Compte inexistant | id 999 | 404 `{"message":"Erreur lors du virement : Compte introuvable"}` | sans débit ni crédit |
| Compte d'un autre client | source d'autrui | 403 | sans débit ni crédit ; sans jeton : 401 |
| Solde insuffisant | 9 999 € sur 2 130,00 € | 4xx `{"message":"Solde insuffisant sur le compte source"}` | aucun solde ni opération ; pas de nom d'erreur interne |
| Nominal | 100 € du `1` au `2`, motif « loyer » | 201, `etat` EXECUTE, écho, DEBIT « Virement vers {compte destinataire} - loyer » / CREDIT « Virement depuis {compte source} - loyer » avec soldes après, SUCCESS, même transaction, `UPDATE … WHERE solde >= montant` ; redirection `/tableau-de-bord`, bouton `aria-busy` | — |
| Seuil 5 000 € | 5 000,00 puis 4 999,99 | premier : exécuté, PENDING, opérations « [EN ATTENTE] » ; second : SUCCESS sans préfixe ; textes dans un seul fichier | — |
| Remise asynchrone | ligne `outbox` `coeur-bancaire.virement-a-traiter.v1` dans la transaction du débit ; consommateur interne idempotent | ne débite pas un EXÉCUTÉ, n'émet aucune notification ; message livré deux fois = un effet | — |
| Outbox refusée | écriture `outbox` impossible | rollback total ; 5xx `{"message":"Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard"}` ; front reste sur `/virement` ; entrée VIREMENT dans `outbox_audit` (BUG-073) | — |
| Audit | virement exécuté 100 €, « loyer » | `AUDIT_LOGS` : VIREMENT « Montant: {amount} - Motif: loyer - Issue: EXECUTE » puis TRANSFER_EXECUTED « Montant: {amount} - Motif: loyer », utilisateur et IP d'origine, via `outbox_audit` indépendante sur `coeur-bancaire.audit.v1`, clé = id du virement ; schéma `contrats/evenements/coeur-bancaire.audit.v1.schema.json` | — |
| Idempotence | deux soumissions, même `Idempotency-Key` | un virement, un débit, réponse 2 identique à la 1re | — |
| Concurrence | deux virements, solde pour un seul | un exécuté, l'autre « Solde insuffisant sur le compte source », jamais de solde négatif | — |
| Horloge de rejeu | profil `rejeu`, `HORLOGE_DECALAGE` | `instant()` = `2026-11-17T10:00:00` Europe/Paris à la seconde, avance comme l'horloge système injectée, `aujourdhui()` = `2026-11-17` ; sans profil : horloge système ; décalage illisible : contexte ne démarre pas ; aucun traitement planifié métier en `rejeu` (relais outbox actif) | démarrage refusé |
| Seuils `nb.fraude.*` | seuil d'attente/alerte négatif ou nul | le service refuse de démarrer | — |
| Règle d'architecture | `Instant.now()` dans `application` | `*ArchitectureTest` (`HorlogeArchitectureTest`) échoue | — |

</intent-contract>

## Code Map

- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/` -- squelette « témoin » à étendre : `domain/{Temoin,TemoinEcrit}`, `application/{EcrireTemoinCasUsage, port/in/EcrireTemoin, port/out/{Horloge,DepotTemoins,Transaction,EvenementsSortants}}`, `adapter/in/{ConfigurationSecurite,Textes,AttributsErreur,PointEntreeTemoin}`, `adapter/out/{evenements/{RelaisOutbox,LigneOutbox,EnveloppeTemoinEcrit,EvenementsSortantsKafka}, persistance/{TransactionSpring,DepotTemoinsJdbc}, horloge/HorlogeSysteme}`, `infrastructure/{ConfigurationEvenements,AssemblageTemoin,ApplicationCoeurBancaire}` ; la `Transaction` doit offrir `executerIndependamment(...)` pour l'audit.
- `services/coeur-bancaire/src/main/resources/db/migration/V1__temoins_et_outbox.sql` -- `outbox` existante ; ajouter `V2__…` (comptes, opérations, virements, `outbox_audit`, `evenements_traites`, clés d'idempotence) ; `application.properties` : `nb.fraude.*` (seuil-attente et seuil-alerte 5000, autres seuils des stories suivantes seulement si leur validation est livrée ici), `spring.kafka.consumer.group-id`.
- `services/coeur-bancaire/reference-data/` -- à créer sur le modèle de `services/identite-clients/reference-data/{reference-data.sql,generer-reference-data.sh}` et `ReferenceDataIT` : comptes et opérations du legacy avec leurs identifiants (COURANT `1` « FR76-0001-0001 », EPARGNE `2` de `user1`) ; source : `docs/retro/00-global/DATA-DICTIONARY.md` et `docs/retro/`.
- `services/coeur-bancaire/src/test/java/…` -- modèles : `HorlogeArchitectureTest`, `HexagonalArchitectureTest`, `CoeurBancaireSecuriteIT` (à compléter, déjà tagué FR-030-01), `FluxTemoinTest`, `EchecPublicationTest`, `TemoinEcritContratEvenementTest`, helpers `Conteneurs`, `ContexteDemarre`, `LecteurTopic`.
- `services/audit/src/main/java/fr/nextbanking/audit/adapter/in/evenements/RecepteurAudit.java` -- écoute aujourd'hui `identite-clients.audit.v1` en dur : ajouter `coeur-bancaire.audit.v1` (clé = id du virement, enveloppe `{idEvenement,type:"audit",version:1,horodatage,producteur,donnees:{utilisateur,action,detail,adresseIp}}`) ; `ActionAudit` contient déjà `VIREMENT` et `TRANSFER_EXECUTED` ; tests `ReceptionAuditIT`, `AuditContratEvenementTest`.
- `contrats/evenements/identite-clients.audit.v1.schema.json` -- modèle du nouveau `coeur-bancaire.audit.v1.schema.json` ; `services/identite-clients/…/ConfigurationEvenements`, `outbox_audit` (V2) -- modèle du producteur d'audit.
- `frontend/src/adapters/routes/routes.tsx` (`ROUTES_PROTEGEES`, `TITRES_ONGLET`), `adapters/textes.ts` (`TEXTES`, `titresOnglet`, `navigation.liens.virement`), `adapters/ui/{ChampTexte,BoutonPrimaire,AlerteErreur,Carte,LienNavigation}`, `adapters/session/fetchAuthentifie.ts`, `adapters/http/ClientProfilHttp.ts` (modèle de client HTTP), `infrastructure/main.tsx` (assemblage), `domain/montant.ts` (ajouter l'analyse de la saisie « 100,00 » en centimes), `adapters/test/rendreApplication.tsx`, `EcranConnexion` (modèle d'écran) ; à créer : écran de virement, `<select>` natif accessible (composant du design system), port/cas d'usage `application/`, `data-testid` `virement-compte-source`, `virement-compte-destinataire`, `virement-montant`, `virement-motif`, `virement-executer`, `virement-message-erreur`.
- `scripts/golden-master/verifier-scenario.sh SC-003-01|02|04` -- statique : `start_url` littéral dans `routes/*.tsx`, `data-testid` présents hors tests, textes (non bloquant) ; ne rien y modifier.
- `compose.yaml` -- dépendances/variables du cœur bancaire (consommateur, `HORLOGE_DECALAGE`, `HORLOGE_DEPART`) ; les services à jour dans `docker compose config`.
- Spine : `_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/ARCHITECTURE-SPINE.md` (AD-12, AD-16 à AD-19, AD-22) ; `docs/retro/features/FEAT-003/FIELDS.md`, `docs/retro/00-global/{MESSAGES,LEGACY-BUGS}.md`.

## Tasks & Acceptance

**Execution:**
- `contrats/evenements/coeur-bancaire.audit.v1.schema.json` et `coeur-bancaire.virement-a-traiter.v1` (schéma interne) -- contrats versionnés avec exemple -- AC audit et remise asynchrone
- `services/coeur-bancaire/**` (migration, domaine `Virement`/`Compte`/`Operation`, cas d'usage de virement, ports, adaptateurs JDBC, contrôleur REST, outbox et relais généralisés, `outbox_audit`, consommateur interne idempotent, idempotence, `HorlogeRejeu`, `ConfigurationFraude`, reference-data) -- virement immédiat de bout en bout -- ACs API, atomicité, audit, concurrence, horloge, seuils
- `services/audit/**` -- consommer `coeur-bancaire.audit.v1` -- AC audit
- `frontend/**` (domaine montant, application, adapters écran/route/textes/select, assemblage) -- écran `/virement` -- ACs écran
- Tests (tagués comme indiqué) : domaine et application à 100 % lignes/branches, `*ContratEvenement*Test` producteur et consommateur, `*TransitionConcurrente*Test`, `CoeurBancaireSecuriteIT` complétée (401, 403), `HorlogeArchitectureTest` vérifié, `ConfigurationFraude*Test`, Vitest du front couvrant chaque ligne de la matrice -- toute la matrice

**Acceptance Criteria:**
- Given la story 3.1 d'`epics.md`, when chacun de ses critères est exécuté, then il est satisfait tel que rédigé (messages, statuts HTTP, `data-testid`, textes, ordre d'audit)
- Given `scripts/golden-master/verifier-scenario.sh SC-003-01`, `SC-003-02` puis `SC-003-04`, when lancés, then code de sortie 0 chacun
- Given `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-003-02 | … | FR-026-03'`, puis `-Dgroups='BUG-008 | BUG-011 | BUG-073'`, puis `cd frontend && npx vitest run -t 'FR-003-0[1-8]|FR-003-12|FR-023-02'`, when lancés, then code de sortie 0 chacun
- Given `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` et `trace-smoke.sh`, when lancés, then code de sortie 0 (les `KO` de `check-gates-exist.sh` ne concernent que des stories non livrées)

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 0 findings — high 0, medium 0, low 0, false 0, maybe-false 0
- findings:
  - l'audit d'alignement (descriptif) n'a relevé aucune divergence : la lecture A (story entière) est celle implémentée ; les écarts qu'il cite sont les trois `deferred` déjà consignés.

## Design Notes

- Statut d'affichage SUCCESS/PENDING distinct de l'état du virement (EXECUTE ici seulement) ; le seuil d'attente 5 000 € (BUG-009 conservé) ne suspend pas encore : il préfixe « [EN ATTENTE] », la suspension est livrée en 3.4.
- Transaction de soumission : idempotence → contrôles → `UPDATE comptes SET solde = solde - :m WHERE id = :s AND solde >= :m` → crédit → deux opérations (avec solde après) → `virements` → ligne `outbox` ; échec de l'`outbox` = rollback total et entrée VIREMENT hors transaction (`executerIndependamment`) dans `outbox_audit`.
- Audit VIREMENT puis TRANSFER_EXECUTED dans cet ordre, publiés par le relais d'`outbox_audit`, clé = id du virement.
- Le propriétaire d'un compte se lit sur le compte (identifiant client du JWT) ; pas de projection clients nécessaire pour la 3.1.

## Verification

**Commands:**
- `scripts/gates/build.sh` -- expected: code 0
- `scripts/gates/check-gates-exist.sh` -- expected: code 0, `OK` pour les contrôles de la story
- `scripts/gates/golden-master.sh` -- expected: code 0
- `scripts/gates/trace-smoke.sh` -- expected: code 0
- `scripts/golden-master/verifier-scenario.sh SC-003-01` (puis `-02`, `-04`) -- expected: code 0
- les trois commandes `-Dgroups` / `vitest -t` de la section Acceptance -- expected: code 0
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dtest='*ArchitectureTest'` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** virement immédiat entre comptes d'un même client : `POST /api/coeur-bancaire/virements` (contrôles, débit conditionnel et crédit atomiques, opérations avec solde après, statut PENDING/SUCCESS, idempotence, outbox `virement-a-traiter`, audit par `outbox_audit` indépendante), `GET /api/coeur-bancaire/comptes`, consommateur interne idempotent, horloge réglée du profil `rejeu`, seuils `nb.fraude.*` validés, contrats `coeur-bancaire.audit.v1` et `virement-a-traiter.v1`, consommation de l'audit côté service `audit`, écran `/virement` du front.

**Fichiers :** `services/coeur-bancaire/**` (migration V2, domaine, cas d'usage, adaptateurs, reference-data, tests), `services/audit/**` (écoute de `coeur-bancaire.audit.v1`), `contrats/evenements/coeur-bancaire.*.schema.json`, `frontend/**` (écran, client HTTP, liste déroulante, montant en centimes, textes, route), `compose.yaml` (variables d'horloge et d'IP, dépendance de l'audit).

**Revue :** 0 patch, 0 rejet ; 3 éléments dans `deferred` (données de référence incomplètes, texte « Issue » de l'échec de remise, format de `HORLOGE_DECALAGE`). Recommandation de nouvelle revue : `false`.

**Vérification (relancée à neuf, code 0) :** `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` ; `verifier-scenario.sh` SC-003-01, SC-003-02, SC-003-04 ; les deux `mvn verify -Dgroups=…` du cœur bancaire ; `vitest -t 'FR-003-0[1-8]|FR-003-12|FR-023-02'`.

**Risques résiduels :** `trace-smoke.sh` a tourné sur la pile compose déjà démarrée (images non reconstruites) ; le jeu de référence est partiel ; le lien SEPA pointe vers `/beneficiaires` (route à venir) ; compte destinataire d'un autre client refusé en 403 et « solde insuffisant » rendu en 400 (choix à relire).
