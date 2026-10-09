# Epic 9 Context: Espace conseiller et administration

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Livrer l'Espace Conseiller (portefeuille limité aux clients du conseiller connecté et décidé côté serveur, trois indicateurs, une carte par client, détail inline) et compléter la page `/administration` pour l'ADMIN : indicateurs globaux, gestion des clients (recherche, détail avec « Client depuis », activation et désactivation auditées), liste de tous les utilisateurs, revue de toutes les alertes de fraude sans effet sur les virements suspendus, journal des traitements et catalogue des produits. Les sections déjà livrées sur `/administration` (virements suspendus, epic 3 ; file et historique des prêts, epic 5) restent en place et vertes.

## Stories

- Story 9.1: Exposer le portefeuille du conseiller décidé côté serveur
- Story 9.2: Afficher l'Espace Conseiller (portefeuille, cartes et détail d'un client)
- Story 9.3: Afficher les indicateurs, la gestion des clients et tous les utilisateurs sur l'administration
- Story 9.4: Activer et désactiver un client depuis la gestion des clients
- Story 9.5: Revoir les alertes de fraude sans toucher au virement suspendu
- Story 9.6: Afficher le journal des traitements et le catalogue des produits sur l'administration

## Requirements & Constraints

- Le périmètre d'un conseiller est décidé par chaque service d'après le `sub` du jeton et sa projection locale (`conseillerId = sub`) ; tout paramètre `conseillerId` ou `clientId` de la requête est ignoré. Un ADMIN sans client reçoit des listes vides. Le front redirige aussi, mais ne fait jamais foi.
- Contrôle d'accès : sans jeton 401 ; mauvais rôle 403 avec le corps `{"message": …}`, sans pile ni code interne. Les routes `/espace-conseiller` et `/administration` redirigent les rôles non autorisés vers `/tableau-de-bord`, sans message ni appel aux points protégés.
- Le portefeuille est chargé une fois par session (trois appels en parallèle, jamais en cascade) et rechargé après déconnexion puis reconnexion.
- Textes legacy repris à l'identique (casse et absence d'accents comprises) ; libellés des indicateurs en capitales par CSS seulement. Aucun texte inventé : un texte manquant est un point ouvert.
- Bugs legacy, chacun couvert par un test tagué : BUG-055 corrigé (« Client depuis » = date de création de la fiche, pas la date d'affectation), BUG-056 corrigé (toutes les alertes listées, pas seulement `OPEN`), BUG-053 conservé (statut `DEFAUT` sans badge).
- Alertes de fraude : confirmer ou classer en faux positif ne change jamais le virement suspendu (ni débit, ni libération, ni rejet, ni audit de virement, ni notification). Transition depuis `OPEN` uniquement (`UPDATE … WHERE statut = 'OPEN'`), une seule issue en cas de concurrence ; demande sans identifiant ignorée sans erreur. Auteur enregistré par identifiant, affiché par nom complet ; horodatage par le port Horloge. À la libération d'un virement, seules les alertes encore `OPEN` passent à `REVIEWED`.
- Journal des traitements : 50 entrées les plus récentes, tri par début décroissant, heures en Europe/Paris ; section « Traitements nocturnes » sans « (EJB Timer @Schedule) ».
- Une section en erreur affiche le `message` reçu (`conseiller-message-erreur`, `admin-message-erreur`) sans détail technique et n'empêche pas les autres de s'afficher.
- Tests JUnit tagués `@Tag("FR-…")` / `@Tag("BUG-…")` (lancés par `-Dgroups=`), tests Vitest avec `[FR-…]` / `[BUG-…]` dans le titre ; chaque story prévoit une violation volontaire qui doit faire échouer son test.
- Chaque story se clôt par `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh` et `golden-master.sh` verts. Scénarios vérifiés par `scripts/golden-master/verifier-scenario.sh` : SC-015-01 et SC-015-02 (9.2), SC-016-01 et SC-016-02 (9.3), SC-016-01 (9.6). Ne jamais rejouer le golden master avant l'epic 10.

## Technical Decisions

- Points d'API : `GET /api/{identite-clients|coeur-bancaire|credits}/conseiller/portefeuille` ; `GET /api/identite-clients/administration/{indicateurs|clients|utilisateurs}` ; `GET /api/credits/administration/indicateurs` ; `GET /api/coeur-bancaire/administration/alertes-fraude` et `POST …/alertes-fraude/{alertId}/{confirmation|faux-positif}` ; journal `GET /api/coeur-bancaire/traitements?limite=50` (déjà ouvert à l'ADMIN) ; catalogue lu sur `open-banking-conseiller`.
- Aucun appel HTTP entre services : coeur-bancaire et credits tiennent une projection clients alimentée par `identite-clients.client-modifie.v1` (porte `conseillerId`, `actif`, `dateCreation`, `versionAgregat`) ; un changement d'affectation se reflète avec la latence de projection. Consommateurs idempotents.
- Codes métier renvoyés tels quels (types de compte `COURANT`/`EPARGNE`, types et statuts de prêt, statuts d'alerte) ; montants en chaîne décimale (`"2130.00"`), calculés en centimes entiers côté front par des formateurs de `src/domain`.
- Chaque service vérifie lui-même jeton et rôles ; le contrôle du rôle `ADMIN` passe par le point unique déjà en place dans chaque service, sans bibliothèque partagée entre services (décision maintenue le 2026-10-08). Les `*SecuriteIT` de chaque service sont complétés (401 / 403 / périmètre).
- Front : le jeton n'est posé que par le décorateur `fetchAuthentifie` ; aucun port ni cas d'usage de `src/application` ne reçoit le jeton, tout nouveau client HTTP est câblé avec ce décorateur dans la composition.
- Désactivation/activation : l'écran réutilise l'API ADMIN existante d'`identite-clients` (audits `ADMIN_DEACTIVATE` / `ADMIN_ACTIVATE`, publication `client-modifie`) sans la modifier ni émettre d'audit côté front ; la connexion d'un client désactivé est refusée, un jeton déjà émis reste valable jusqu'à son expiration (risque accepté).
- `data-testid` au format `{écran}-{élément}[-{id}]` (ex. `conseiller-voir-detail-{clientId}`, `admin-detail-{customerId}`, `admin-desactiver-{customerId}`, `admin-alerte-confirmer-{alertId}`, `admin-voir-produits`).

## UX & Interaction Patterns

- Détail client (conseiller et admin) : panneau inline sur la même page, sans modale ni changement de route ; focus au titre à l'ouverture ; « Fermer » ou `Échap` referme et rend le focus au bouton déclencheur. Le détail conseiller n'affiche pas « Client depuis » ; le détail admin l'affiche au format `jj/MM/aaaa`.
- Cartes client : avatar à l'initiale en `aria-hidden`, badge « Actif »/« Inactif » ; un client inactif est distingué par le texte, jamais par la couleur seule. « ⚠ n pret(s) en attente » prioritaire sur « ✓ n pret(s) en cours ».
- Activation/désactivation sans modale de confirmation ; bouton `aria-disabled` pendant la requête ; succès en `alert-success` (`role="status"`) seulement après réponse de l'API.
- Ordre figé des sections de `/administration` : indicateurs, Pipeline Prets, Gestion des clients, Historique de tous les prets, Tous les utilisateurs du systeme, Virements suspendus, Alertes Fraude, Traitements nocturnes, Catalogue produits bancaires. Les libellés clés (« TOTAL CLIENTS », « Gestion des clients », « Jean Dupont », « user1 »…) doivent rester dans les 5 000 premiers caractères du texte rendu (fenêtre du golden master).
- États vides : « Aucun client dans votre portefeuille. », « Aucun client trouve », « Aucune alerte fraude enregistrée », « Aucun traitement nocturne enregistre », « Aucun produit », « Aucun compte », « Aucun pret ».

## Cross-Story Dependencies

- 9.2 dépend de 9.1 ; 9.3 à 9.6 enrichissent la même page `/administration` et se suivent (9.4 et 9.5 après 9.3 ; 9.6 après 9.3 à 9.5, pour le test d'ordre complet des sections).
- Avec les autres epics : topic `client-modifie` et API ADMIN d'activation/désactivation (epic 2) ; routeur et redirections par rôle (epic 2) ; file des virements suspendus et alertes `FRAUD_ALERTS` avec leur machine d'états (epic 3) ; décorateur `fetchAuthentifie` et contrôle de rôle centralisé par service (epic 0) ; journal des traitements (epic 4) ; file et historique des prêts, projection clients des crédits (epic 5) ; catalogue des produits d'`open-banking-conseiller` (epic 7).
- Décisions préalables confirmées le 2026-10-05 : « Client depuis » = date de création de la fiche, auteur de la revue par identifiant, pagination/filtre du journal (limite de 50).
