---
title: 'Exposer le portefeuille du conseiller décidé côté serveur'
type: 'feature'
created: '2026-10-09'
status: 'done'
baseline_revision: 'fca625fa20b7453b74d4ba540b5395c4bbb9ff9e'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-9-context.md'
warnings: ['oversized']
deferred:
  - summary: >-
      Dans une application lancée (compose), aucune étape ne charge reference-data/projection-clients.sql du cœur bancaire ni de credits : le portefeuille de ces deux services reste vide tant qu'aucun événement client-modifie de version supérieure n'est consommé.
    evidence: |-
      Aucun script, Makefile ni compose.yaml ne charge projection-clients.sql (seuls les tests le font) ; identite-clients ne republie pas ses clients au démarrage (RelaisOutboxClients relaie seulement l'outbox). Écart préexistant pour credits ; le chargement des projections en rejeu relève d'ADV-08 et de la story « Socle du rejeu final ». Les lignes de projection antérieures à V11 gardent conseiller_id nul (accepté par la spec).
    location: >-
      services/coeur-bancaire/reference-data/projection-clients.sql
    severity: medium
---

<intent-contract>

## Intent

**Problem:** aucun service n'expose le portefeuille d'un conseiller : l'Espace Conseiller (story 9.2) n'a ni la liste de ses clients, ni leurs comptes, ni leurs prêts, et rien ne garantit qu'un conseiller ne lise que ses propres clients (BUG-093 / FR-030-07 : le legacy croyait le front).

**Approach:** trois points `GET /api/{identite-clients|coeur-bancaire|credits}/conseiller/portefeuille`, réservés à CONSEILLER et ADMIN, où chaque service déduit le portefeuille du `sub` du jeton : `identite-clients` depuis `customers.advisor_id`, `coeur-bancaire` et `credits` depuis leur projection `clients` alimentée par `identite-clients.client-modifie.v1` (le cœur bancaire doit d'abord y stocker `conseillerId`).

## Boundaries & Constraints

**Always:**
- Portefeuille = clients dont `conseillerId = sub` (identifiant numérique du jeton), non supprimés, actifs **et** inactifs (la story 9.2 affiche le badge « Inactif »). Tout paramètre de requête (`conseillerId`, `clientId`, …) est ignoré : le contrôleur ne lit aucun paramètre.
- Rôles : CONSEILLER ou ADMIN → 200 ; CLIENT seul → 403 `{"message":"Accès refusé"}` ; sans jeton ou `sub` non numérique → 401 `{"message":"Authentification requise"}`. ADMIN sans client affecté → 200 `[]`.
- Le contrôle de rôle passe par le point unique déjà en place dans chaque service (règle `RoleAdminArchitectureTest` maintenue : une seule classe de production lit `Role.ADMIN`).
- Formes de réponse (JSON camelCase, montants en chaîne `toPlainString()` à 2 décimales, codes renvoyés tels quels, y compris `AUTO`, `ETUDIANT` ou un type `null` pour un prêt) :
  - identite-clients : `[{clientId, nomComplet, login, email, telephone, actif}]`, trié par `nomComplet` puis `clientId` ; `telephone` peut être `null`.
  - coeur-bancaire : `[{clientId, comptes:[{compteId, numero, type, solde}]}]`, un élément par client du portefeuille (liste `comptes` vide s'il n'en a pas), trié par `clientId`, comptes triés par `compteId`.
  - credits : `[{clientId, prets:[{pretId, type, montant, mensualite, statut}]}]`, un élément par client du portefeuille, trié par `clientId`, prêts dans l'ordre de `PretRepository` (`date_debut desc, id desc`).
- Le cœur bancaire stocke `conseillerId` (nullable) et `supprime` dans sa projection ; un événement de `versionAgregat` strictement supérieure écrase l'affectation (y compris vers `null`), une version égale ou inférieure ne change rien ; consommateur idempotent via `evenements_traites` (inchangé).
- Hexagone : `domain`/`application` en Java pur, couverture 100 % lignes et branches ; adaptateurs ≥ 80 %. Tests JUnit tagués `@Tag("FR-015-01")` (accès par rôle), `@Tag("FR-015-02")` (contenu, tri, paramètre ignoré), `@Tag("FR-015-09")` (codes tels quels, coeur-bancaire et credits), et `@Tag("FR-030-07")` sur les contrôles de rôle.

**Never:**
- Aucun appel HTTP entre services, aucune liste d'identifiants reçue du front, aucune bibliothèque partagée entre services.
- Ne pas affaiblir un contrôle : pas de seuil abaissé, pas d'exclusion, pas de règle ArchUnit élargie à plusieurs classes. Ne pas toucher `scripts/gates/**`, `docs/quality-gates.md`, `sprint-status.yaml`, le golden master (`e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json`) ; ne jamais le rejouer.
- Pas de front dans cette story (story 9.2), pas de modification du contrat d'événement `contrats/evenements/identite-clients.client-modifie.v1.schema.json` (il porte déjà `conseillerId`).
- Ne pas modifier `DonneesDeReference` du cœur bancaire pour charger la projection (les IT existantes de la file de revue dépendent de noms absents).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Portefeuille de sophie | jeton `sub=11`, CONSEILLER ; clients 1 (Jean Dupont) et 2 (Marie Lebrun) affectés à 11, clients 3 et 4 à 12 | identité : [Jean Dupont, Marie Lebrun] ; cœur : clients 1 et 2 avec leurs comptes (`"2130.00"`, `COURANT`) ; crédits : clients 1 et 2 avec leurs prêts | — |
| Paramètre forgé | `?conseillerId=12` ou `?clientId=3` | même réponse que sans paramètre | ignoré |
| ADMIN sans client | jeton `sub=10`, ADMIN | 200 `[]` sur les trois points | — |
| CLIENT | jeton `sub=1`, CLIENT | 403 `{"message":"Accès refusé"}` | sans pile ni code interne |
| Sans jeton / `sub` illisible | pas d'`Authorization` / `sub="abc"` | 401 `{"message":"Authentification requise"}` | — |
| Réaffectation | événement `client-modifie` du client 1, `conseillerId=12`, `versionAgregat=2` | client 1 quitte le portefeuille de 11 et entre dans celui de 12 (cœur et crédits) | version ≤ actuelle : ignorée |
| Client sans compte / sans prêt | client 2 n'a ni compte ni prêt | `{clientId:2, comptes:[]}` / `{clientId:2, prets:[]}` | — |
| Client supprimé | projection `supprime=true` | absent du portefeuille | — |

</intent-contract>

## Code Map

**identite-clients** (`services/identite-clients/src/main/java/fr/nextbanking/identiteclients/`)
- `adapter/in/http/ConfigurationSecurite.java` -- `.anyRequest().authenticated()` ; 401/403 JSON déjà écrits par le filtre. Aucune règle par chemin : rôles vérifiés en application.
- `adapter/in/http/GestionErreurs.java` -- `AuthentificationRequise`→401, `domain/AccesRefuse`→403 `record Message`.
- `adapter/in/http/PointEntreeClients.java:55-77` -- `acteurId(Jwt)` (`Long.parseLong(getSubject())`, sinon `AuthentificationRequise`) et `roles(Jwt)` privés : modèle à reproduire.
- `application/ChangerEtatClientCasUsage.java:54-56` -- seul lecteur actuel de `Role.ADMIN` (`exigerAdmin` en ligne).
- `src/test/.../RoleAdminArchitectureTest.java` -- n'autorise que `ChangerEtatClientCasUsage` (et `Role`) à lire `Role.ADMIN`.
- `application/port/out/DepotClients.java` / `adapter/out/persistance/DepotClientsJdbc.java` (colonnes L18 sans email/phone/advisor_id) -- port de lecture à étendre, ou nouveau port dédié.
- `domain/Client.java` -- sans email/téléphone : prévoir un record de lecture dédié (ex. `ClientDuPortefeuille`).
- Table `customers` (V1, V3) : `id, username, full_name, email, phone (nullable), role, active, advisor_id, date_creation, supprime, version_agregat`.
- `infrastructure/AssemblageConnexion.java:45-50` -- câblage `@Bean` des cas d'usage.
- Tests : `src/test/.../adapter/IdentiteClientsSecuriteIT.java` (L107-135 refus par rôle, `assertRefus`), `adapter/OutilsHttp.java` (`jetonValide(sub, role)`, `chargerDonnees` : 7 utilisateurs, user1/user2 → advisor 11, user3/user4 → 12), `application/ChangerEtatClientCasUsageTest.java` (faux ports en classes anonymes : à compléter si `DepotClients` gagne une méthode).

**coeur-bancaire** (`services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/`)
- `adapter/in/evenements/RecepteurClientModifie.java:33,57` -- `Donnees(clientId, nomComplet, versionAgregat)` : ajouter `Long conseillerId`, `boolean supprime`.
- `application/port/in/RecevoirClientModifie.java`, `application/RecevoirClientModifieCasUsage.java:30-41`, `application/port/out/ProjectionClients.java`, `adapter/out/persistance/ProjectionClientsJdbc.java:21-31` -- chaîne d'écriture de la projection à étendre ; ajouter une lecture `clientsDuConseiller(long)` (non supprimés, triés par `client_id`).
- `db/migration/V4__suspension_revue_clients_batch.sql:22-26` -- table `clients(client_id, nom_complet, version_agregat)` ; nouvelle migration **V11** (`conseiller_id bigint null`, `supprime boolean not null default false`, index sur `conseiller_id`).
- `application/port/out/Comptes.java:16` / `adapter/out/persistance/ComptesJdbc.java:31-32` -- `duClient(long)` ; ajouter une lecture par lot `desClients(Collection<Long>)` (index `comptes_par_client`).
- `domain/Compte.java`, `domain/Montant.texte()` (`toPlainString`), `domain/TypeCompte`.
- `application/DecisionAdministrateur.java:16-20` -- point unique (`exigerAdmin`, lève `ErreurVirement(Cause.ROLE_INSUFFISANT)`) : y ajouter `exigerConseillerOuAdmin`.
- `adapter/in/GestionErreursVirements.java:30-31,138` -- `ROLE_INSUFFISANT`→403 seulement pour les contrôleurs listés dans `assignableTypes` : ajouter le nouveau contrôleur.
- `adapter/in/PointEntreeRevue.java:49-51` + `application/ConsulterRevueCasUsage.java` -- modèle contrôleur → cas d'usage ; `adapter/in/JetonRoles.depuis(Jwt)`.
- `infrastructure/AssemblageVirement.java:107-121` -- câblage.
- Tests : `src/test/.../adapter/CoeurBancaireSecuriteIT.java` (`CHEMINS` L38-42), `adapter/Jetons.java` (`pour(id, role)`), `adapter/DonneesDeReference.java` (comptes 1 et 2 du client 1, compte 3 du client 8), `adapter/in/evenements/RecepteurClientModifieTest.java` (exemple du schéma, stub 4 arguments à adapter), `application/RecevoirClientModifieCasUsageTest.java`, `application/MemoireCoeurBancaire.java:64,105,464` (double en mémoire de `ProjectionClients`), `VirementIT.java:411`.
- Modèle Kafka IT : `services/credits/src/test/.../adapter/ClientModifieKafkaIT.java` (crée le topic par `AdminClient`, attend `evenements_traites`).

**credits** (`services/credits/src/main/java/fr/nextbanking/credits/`)
- `domain/ClientProjete.java`, `application/port/out/ProjectionClients.java`, `adapter/out/persistance/ProjectionClientsJdbc.java:26-56` -- projection complète (avec `conseiller_id`, `supprime`) : ajouter une lecture `clientsDuConseiller(long)`.
- `application/port/out/PretRepository.java:18` / `adapter/out/persistance/PretRepositoryJdbc.java:26,41,170-179` -- `parClient` ; ajouter `desClients(Collection<Long>)` avec le même `COLONNES` et le même tri.
- `application/DroitsAdministrateur.java:13` -- point unique package-private (`exigerAdmin` → `AccesAdministrationRefuse`) : y ajouter `exigerConseillerOuAdmin`.
- `adapter/in/http/GestionErreurs.java:103-111` -- 401/403 ; `adapter/in/http/PointEntreeAdministrationPrets.java:42-81` -- modèle contrôleur (`PretAdministrationJson`, `toPlainString`, type `null` conservé).
- `infrastructure/AssemblageCredits.java:84-87` -- câblage.
- Tests : `src/test/.../adapter/CreditsSecuriteIT.java` (L57-63 chemins sans jeton), `adapter/AdministrationPretsHttpIT.java:39-81` (seed SQL de `clients` et prêts, `spring.kafka.listener.auto-startup=false`), `adapter/OutilsHttp.java`.

**Données de référence**
- `scripts/generer-projections-reference.sh` -- génère `services/credits/reference-data/projection-clients.sql` et le bloc de documents depuis `services/identite-clients/reference-data/reference-data.sql`.
- `services/credits/src/test/.../referencedata/ProjectionClientsReferenceIT.java` -- modèle d'IT (chargement dans un schéma Flyway neuf + régénération à l'identique).

## Tasks & Acceptance

**Execution:**
- `services/identite-clients/.../application/` -- créer le point unique `DroitsRoles` (ou nom équivalent) portant `exigerAdmin` et `exigerConseillerOuAdmin` (lève `AccesRefuse`) ; `ChangerEtatClientCasUsage` l'appelle ; `RoleAdminArchitectureTest` nomme cette nouvelle classe au lieu de `ChangerEtatClientCasUsage` -- un seul lecteur de `Role.ADMIN`, règle conservée.
- `services/identite-clients/.../domain/ClientDuPortefeuille.java` + port in `ConsulterPortefeuille` + `application/ConsulterPortefeuilleCasUsage.java` + port out (méthode `duConseiller(long)` sur `DepotClients` ou port dédié) + JDBC (`where advisor_id = :id and not supprime order by full_name, id`) + `adapter/in/http/PointEntreePortefeuille.java` (`GET /api/identite-clients/conseiller/portefeuille`) + bean dans `AssemblageConnexion` -- portefeuille d'identité.
- `services/coeur-bancaire/src/main/resources/db/migration/V11__conseiller_projection_clients.sql` -- colonnes `conseiller_id`, `supprime`, index.
- `services/coeur-bancaire/...` -- étendre récepteur, port in, cas d'usage, port out, JDBC (upsert avec `conseiller_id`, `supprime`, binding `Types.BIGINT` du nul) et `MemoireCoeurBancaire` ; ajouter `clientsDuConseiller` et `Comptes.desClients` ; cas d'usage `ConsulterPortefeuilleCasUsage` (via `DecisionAdministrateur.exigerConseillerOuAdmin`) ; contrôleur `PointEntreePortefeuille` ajouté à `GestionErreursVirements.assignableTypes` ; bean.
- `services/credits/...` -- `DroitsAdministrateur.exigerConseillerOuAdmin`, `ProjectionClients.clientsDuConseiller`, `PretRepository.desClients`, cas d'usage, contrôleur `GET /api/credits/conseiller/portefeuille`, bean.
- `scripts/generer-projections-reference.sh` -- produire aussi `services/coeur-bancaire/reference-data/projection-clients.sql` (`insert into clients (client_id, nom_complet, conseiller_id, supprime, version_agregat)`, version 1) ; générer le fichier par le script, jamais à la main ; `services/coeur-bancaire/src/test/.../referencedata/ProjectionClientsReferenceIT.java` sur le modèle de credits -- le rejeu chargera la projection par les données de référence (ADV-08).
- Tests unitaires des trois cas d'usage (tri, ADMIN vide, CLIENT refusé, client sans compte/prêt, supprimé exclu) et des deux points uniques ; tests de contrat du récepteur du cœur bancaire (`conseillerId` lu, nul accepté) ; `ClientModifieKafkaIT` du cœur bancaire (réaffectation par version supérieure, version inférieure ignorée) ; IT HTTP de chaque point (portefeuille de sophie, paramètre ignoré, ADMIN `[]`, codes et montants tels quels) ; `*SecuriteIT` des trois services complétées (sans jeton → 401, CLIENT → 403 corps exact).

**Acceptance Criteria:**
- Given les données de référence d'identité (user1/user2 → 11, user3/user4 → 12), when `sophie` (`sub=11`, CONSEILLER) appelle `GET /api/identite-clients/conseiller/portefeuille`, then 200 et exactement `[Jean Dupont (1), Marie Lebrun (2)]` avec `clientId, nomComplet, login, email, telephone, actif`.
- Given la projection du cœur bancaire avec clients 1 et 2 → 11, client 3 → 12, et les comptes de référence, when `sophie` appelle `GET /api/coeur-bancaire/conseiller/portefeuille`, then 200, seuls les clients 1 et 2, client 1 avec `{compteId:1, numero:"FR76-0001-0001", type:"COURANT", solde:"2130.00"}` et le compte EPARGNE, client 2 avec `comptes: []`.
- Given la projection de credits et des prêts des clients 1 et 3, when `sophie` appelle `GET /api/credits/conseiller/portefeuille`, then 200, seuls les clients 1 et 2, avec `pretId, type, montant, mensualite, statut` en codes et chaînes décimales tels que stockés.
- Given un événement `client-modifie` réaffectant le client 1 au conseiller 12 en version supérieure, when il est consommé par le cœur bancaire et par credits, then le client 1 sort du portefeuille de 11 et entre dans celui de 12.
- Given une violation volontaire (le cœur bancaire filtre d'après `?conseillerId=` au lieu du `sub`), when `mvn -B -q -f services/coeur-bancaire/pom.xml verify`, then code non nul (test « paramètre ignoré » et `CoeurBancaireSecuriteIT` rouges) — à vérifier puis annuler.

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- verdicts: 6 findings — high 0, medium 1, low 2, false 3, maybe-false 0
- findings:
  - `[false]` `[reject]` Types de prêt renvoyés tels que stockés (`AUTO`, `ETUDIANT`, `null`) au lieu des trois codes énumérés par l'epic — l'epic exige aussi « codes renvoyés tels quels » ; la base de référence contient un prêt `AUTO` (prêt 2 de user1) : filtrer ou recoder cacherait un prêt réel. Aucun mauvais résultat.
  - `[low]` `[patch]` La réaffectation n'était vérifiée qu'au niveau de la table : `ClientModifieKafkaIT` du cœur bancaire recopiait en SQL la requête du portefeuille — corrigé : le test passe désormais par `ProjectionClients.clientsDuConseiller` (adaptateur de production) ; la lecture HTTP de ce même port est couverte par `PortefeuilleIT`.
  - `[medium]` `[defer]` Projection du cœur bancaire et de credits non chargée hors tests (application lancée) ; lignes antérieures à V11 sans conseiller — écart préexistant (credits identique avant la story), chargement en rejeu renvoyé à ADV-08 ; inscrit dans `deferred`.
  - `[low]` `[patch]` Violation volontaire sans trace, et `CoeurBancaireSecuriteIT` restait vert sous violation (les `*SecuriteIT` ne couvraient pas « conseiller : seulement son portefeuille ») — corrigé : un test `le_conseiller_ne_voit_que_son_portefeuille_meme_avec_un_parametre_force` dans les trois `*SecuriteIT` ; violation rejouée par `mvn verify` du cœur bancaire : code 1, ce test et `PortefeuilleIT.un_parametre_force_est_ignore` rouges, puis contrôleur restauré.
  - `[false]` `[reject]` Couverture 401/403 — l'auditeur confirme qu'elle est testée au niveau HTTP dans les trois services ; aucun défaut.
  - `[false]` `[reject]` Réécriture de `epic-9-context.md` hors périmètre — c'est le cache de contexte d'epic recompilé par l'étape 1 du workflow (périmé par rapport aux artefacts de planification) ; aucun code produit.

## Design Notes

- Le point unique du rôle ADMIN reste unique dans chaque service : on y ajoute la variante « CONSEILLER ou ADMIN » plutôt que de lire `Role.ADMIN` ailleurs. Dans identite-clients, ce point est extrait de `ChangerEtatClientCasUsage` vers une classe dédiée, et `RoleAdminArchitectureTest` pointe vers elle (même force de règle).
- Une ligne de projection déjà en base avant V11 garde `conseiller_id` nul jusqu'à un événement de version supérieure : acceptable (aucune donnée de production ; le rejeu charge `projection-clients.sql`).
- Trois appels indépendants côté front (9.2) : chaque réponse porte `clientId`, ce qui suffit à la jointure côté client ; aucune agrégation serveur.

## Verification

**Commands:**
- `mvn -B -q -f services/identite-clients/pom.xml verify` -- code 0
- `mvn -B -q -f services/coeur-bancaire/pom.xml verify` -- code 0
- `mvn -B -q -f services/credits/pom.xml verify` -- code 0
- `mvn -B -q -f services/<service>/pom.xml test -Dgroups='FR-015-02'` sur les trois services, puis `FR-015-01` (trois services) et `FR-015-09` (coeur-bancaire, credits) -- code 0 et au moins un test exécuté
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh`, `scripts/gates/golden-master.sh` -- code 0

## Auto Run Result

Status: done

**Résumé :** trois points `GET /api/{identite-clients|coeur-bancaire|credits}/conseiller/portefeuille`, réservés à CONSEILLER et ADMIN, déduisent le portefeuille du `sub` du jeton (identité : `customers.advisor_id` ; cœur bancaire et credits : projection `clients` alimentée par `identite-clients.client-modifie.v1`). Aucun paramètre de requête n'est lu ; CLIENT → 403, sans jeton ou sujet illisible → 401, ADMIN sans client → `[]`.

**Fichiers modifiés (principaux) :**
- `services/identite-clients/.../application/DroitsRoles.java` — nouveau point unique du contrôle de rôle (ADMIN, CONSEILLER ou ADMIN) ; `ChangerEtatClientCasUsage` l'appelle ; `RoleAdminArchitectureTest` le désigne.
- `services/identite-clients/.../{domain/ClientDuPortefeuille, application/ConsulterPortefeuilleCasUsage, application/port/in/ConsulterPortefeuille, application/port/out/PortefeuilleClients, adapter/out/persistance/PortefeuilleClientsJdbc, adapter/in/http/PointEntreePortefeuille, adapter/in/http/JetonActeur}.java` — portefeuille d'identité ; lecture du jeton factorisée.
- `services/coeur-bancaire/src/main/resources/db/migration/V11__conseiller_projection_clients.sql` — `conseiller_id`, `supprime`, index.
- `services/coeur-bancaire/.../{domain/ClientProjete, domain/ClientEtSesComptes, RecepteurClientModifie, RecevoirClientModifie*, ProjectionClients*, Comptes*, DecisionAdministrateur, ConsulterPortefeuille*, PointEntreePortefeuille, SujetIllisible, GestionErreursVirements, AssemblageVirement}` — projection enrichie, lecture par lot des comptes, point d'API.
- `services/credits/.../{DroitsAdministrateur, ProjectionClients*, PretRepository*, ConsulterPortefeuille*, domain/ClientEtSesPrets, PointEntreePortefeuille, AssemblageCredits}` — point d'API des prêts.
- `scripts/generer-projections-reference.sh` et `services/coeur-bancaire/reference-data/projection-clients.sql` (généré) — projection de référence du cœur bancaire.
- Tests : cas d'usage, points uniques de rôle, `PortefeuilleIT`/`PortefeuilleHttpIT`, `*SecuriteIT` (401, 403, périmètre du conseiller), `ClientModifieKafkaIT` (cœur bancaire, credits), `ProjectionClientsReferenceIT` du cœur bancaire, doublures de test complétées.

**Revue :** 6 constats — 2 correctifs appliqués (low, low), 1 reporté (medium : chargement des projections hors tests), 3 rejetés (faux : codes de prêt tels que stockés exigés par « tels quels » ; couverture 401/403 confirmée ; `epic-9-context.md` est le cache de contexte du workflow).

**Revue complémentaire recommandée :** non (aucun correctif high, aucun medium corrigé ; correctifs : high 0, medium 0, low 2).

**Vérification :**
- `scripts/gates/build.sh` (build propre, `mvn clean verify` de tous les services, couverture, ArchUnit, front) : code 0.
- `mvn -B -q -f services/<s>/pom.xml test -Dgroups=FR-015-02` et `-Dgroups=FR-015-01` sur les trois services, `-Dgroups=FR-015-09` sur coeur-bancaire et credits : code 0.
- `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh`, `scripts/gates/golden-master.sh` : code 0. Golden master non rejoué.
- Violation volontaire (filtre sur `?conseillerId=`) : `mvn -B -q -f services/coeur-bancaire/pom.xml verify` → code 1 (`CoeurBancaireSecuriteIT.le_conseiller_ne_voit_que_son_portefeuille_meme_avec_un_parametre_force` et `PortefeuilleIT.un_parametre_force_est_ignore` en échec), puis restauration.

**Risques résiduels :** projections du cœur bancaire et de credits vides dans une application lancée sans chargement des données de référence (reporté, ADV-08) ; latence de projection acceptée lors d'une réaffectation.
