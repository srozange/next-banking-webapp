# Epic 6 Context: Obtenir ses documents bancaires

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) ouvre la page Documents et télécharge, pour chacun de ses comptes, son RIB, une attestation de solde et ses relevés mensuels des 12 derniers mois, ainsi que ses IFU des deux dernières années fiscales. Le service `documents` est le seul producteur de PDF. Toute demande sans jeton est refusée (401) et le front ramène à la connexion ; le compte d'un autre client n'est jamais servi.

## Stories

- Story 6.1: Télécharger son RIB et son attestation de solde depuis le service documents
- Story 6.2: Télécharger ses relevés mensuels et ses IFU
- Story 6.3: Consulter la page Documents et ouvrir ses PDF

## Requirements & Constraints

- Point d'entrée unique `GET /api/documents/telechargement` (paramètres `type` = RIB, ATTESTATION, RELEVE, IFU, insensible à la casse ; `compteId`, `annee`, `mois`). Types et paramètres sont validés avant tout contrôle d'accès et toute génération.
- Réponses d'erreur au format `{"message": "…"}` : 401 sans jeton, 400 pour paramètre manquant, invalide ou type inconnu (textes du legacy repris à l'identique), 403 « Accès refusé » pour un compte existant d'un autre client, 404 pour compte ou client introuvable, 500 « Erreur lors de la generation du document » sans trace ni nom de composant.
- Seul le titulaire du compte télécharge ses documents, quel que soit son rôle (CONSEILLER et ADMIN compris). Le client d'un IFU est toujours celui du jeton, jamais un paramètre de la requête.
- Succès : 200, `application/pdf`, `Content-Disposition: attachment` avec nom `rib_{compteId}.pdf`, `attestation_solde_{compteId}.pdf`, `releve_{compteId}_{aaaa}_{mm}.pdf`, `ifu_{customerId}_{annee}.pdf`, et `Cache-Control: no-cache, no-store, must-revalidate`.
- Les textes des PDF (libellés, mentions de l'établissement, pied de page) sont ceux du legacy, sans accents là où le legacy n'en a pas. Les dates viennent du port Horloge de `documents` (Europe/Paris) ; l'attestation porte le solde de la projection des comptes.
- Un compteur de téléchargements de la ligne `DOCUMENTS` est incrémenté à chaque succès, jamais sur un refus ou une erreur.
- Relevé : opérations du mois découpé en Europe/Paris, triées par date croissante ; mois vide = « Aucune operation sur cette periode. ». IFU : total des seuls crédits dont la description contient « interet », PFU à 30 % arrondi `HALF_UP` à 2 décimales.
- Scénarios golden master : SC-017-01 (HTTP, couvert par le test d'intégration de la story 6.1), SC-008-01 et SC-017-02 (écran, via `scripts/golden-master/verifier-scenario.sh`, lancés par l'humain en fin d'epic).
- Chaque story est livrée avec ses tests étiquetés par exigence (`@Tag("FR-…")`, chaque tag porté par au moins un test exécuté) et les contrôles `scripts/gates/*` verts.

## Technical Decisions

- Nouveau module `services/documents/` (hexagonal : `domain`, `application`, `adapter.in`, `adapter.out`, `infrastructure`), base `postgres-documents`, port de développement 8084, `/api/documents/**` renvoyé par le proxy Vite et le conteneur web. Tests d'architecture `HexagonalArchitectureTest`, `HorlogeArchitectureTest`, `ClientsHttpArchitectureTest`.
- La bibliothèque PDF est choisie par la story 6.1 et inscrite dans `versions-verifiees.md` (version, registre officiel, date, compatibilité Java 21 et Spring Boot 4.1.1, licence) avant le premier PDF ; elle reste confinée à `adapter.out`.
- Services sans appel HTTP entre eux : `documents` tient trois projections locales (comptes, opérations, clients) alimentées par Kafka, mises à jour seulement si `versionAgregat` est strictement supérieur, dans la même transaction que `evenements_traites` (consommateurs idempotents).
- `coeur-bancaire` produit, via l'outbox et dans la même transaction que l'écriture : `coeur-bancaire.compte-modifie.v1` (clé `compteId` ; état complet du compte, un événement par compte modifié) et `coeur-bancaire.operation-enregistree.v1` (clé `compteId` ; une opération par événement). Schémas JSON avec exemple sous `contrats/evenements/`, bean `NewTopic` dans `infrastructure`, tests de contrat producteur et consommateur. `identite-clients.client-modifie.v1` est déjà produit par l'epic 2.
- `coeur-bancaire` reste seul à modifier un solde. Entre services, l'identifiant numérique `compteId`, jamais le numéro de compte.
- Données de référence de `documents` : 5 lignes `DOCUMENTS` plus les projections générées par `scripts/generer-projections-reference.sh`, jamais écrites à la main.
- Jeton vérifié par le service ; l'appartenance se décide d'après le jeton et la projection locale, jamais d'après une liste fournie par le front.
- Code et champs en français, montants `BigDecimal` à 2 décimales, IBAN masqué dans les journaux.

## UX & Interaction Patterns

- Route `/documents` (titre d'onglet « BankApp Demo - Documents »), même contenu pour les trois rôles ; `/documents`, `/mon-conseiller`, `/espace-conseiller` et `/administration` redirigent vers `/login` sans jeton.
- Par compte : section « Compte {type} - {numéro} » avec carte RIB, carte attestation et liste « Releves mensuels — {numéro} » des 12 mois les plus récents (mois en français avec accents). Section « Documents fiscaux » avec cartes IFU des deux années fiscales précédant `aujourdhui`.
- `aujourdhui` (`yyyy-MM-dd`, Europe/Paris) est fourni par `documents` : le front ne lit jamais l'horloge du navigateur pour ces valeurs.
- Étiquettes en `tag`, séparateurs en `eyebrow` (capitales par CSS, texte source en casse d'origine). Liens « Telecharger PDF » avec `data-testid` `documents-rib-{compteId}`, `documents-attestation-{compteId}`, `documents-releve-{compteId}-{aaaa}-{mm}`, `documents-ifu-{annee}` et nom accessible suffixé « (nouvel onglet) » réservé aux lecteurs d'écran.
- Le téléchargement passe par un appel authentifié (`Authorization: Bearer`), puis le PDF s'ouvre dans un nouvel onglet ; sur 401, suppression de `nb.jeton` et retour à `/login` sans ouvrir d'onglet.

## Cross-Story Dependencies

- 6.2 dépend de 6.1 (module `documents`, projections comptes et clients, contrôle d'accès, en-têtes, compteur) et ajoute la projection des opérations et le topic `operation-enregistree`.
- 6.3 dépend de 6.1 et 6.2 (les quatre types de PDF servis) et de l'epic 2 (garde des routes, gabarit, `verifier-scenario.sh`).
- 6.1 dépend de l'epic 2 (producteur `client-modifie`, vérification du jeton, format d'erreur) et des epics 3 et 4 (chemins d'écriture des comptes dans `coeur-bancaire`, qui doivent émettre les nouveaux événements).
