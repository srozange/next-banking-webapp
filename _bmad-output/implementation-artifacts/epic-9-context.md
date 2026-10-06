# Epic 9 Context: Espace conseiller et administration

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Livrer l'Espace Conseiller (portefeuille limité aux clients du conseiller connecté, trois indicateurs, une carte par client, détail inline) et compléter la page `/administration` pour l'ADMIN : indicateurs globaux, gestion des clients (recherche, détail, activation/désactivation auditées), liste de tous les utilisateurs, revue des alertes de fraude sans effet sur les virements suspendus, journal des traitements et catalogue des produits. Les sections déjà livrées sur `/administration` (virements suspendus, epic 3 ; file et historique des prêts, epic 5) restent en place et vertes.

## Stories

- Story 9.1: Exposer le portefeuille du conseiller décidé côté serveur
- Story 9.2: Afficher l'Espace Conseiller (portefeuille, cartes et détail d'un client)
- Story 9.3: Afficher les indicateurs, la gestion des clients et tous les utilisateurs sur l'administration
- Story 9.4: Activer et désactiver un client depuis la gestion des clients
- Story 9.5: Revoir les alertes de fraude sans toucher au virement suspendu
- Story 9.6: Afficher le journal des traitements et le catalogue des produits sur l'administration

## Requirements & Constraints

- Le périmètre d'un conseiller est décidé par le service, d'après le `sub` du jeton et sa projection locale ; tout paramètre `conseillerId` ou `clientId` de la requête est ignoré. Un ADMIN sans client reçoit une liste vide.
- Contrôle d'accès : sans jeton 401 ; mauvais rôle 403 avec le corps `{"message": …}`, sans pile ni code interne. Les routes `/espace-conseiller` et `/administration` redirigent les rôles non autorisés vers `/tableau-de-bord`, sans message ni appel aux points protégés.
- Le portefeuille est chargé une fois par session (trois appels en parallèle, jamais en cascade) et rechargé après reconnexion.
- Les textes legacy sont repris à l'identique (casse, absence d'accents comprise) ; libellés des indicateurs en capitales par CSS seulement. Aucun texte inventé : un texte manquant est un point ouvert.
- Bugs legacy à corriger ou conservés, chacun couvert par un test : BUG-055 (« Client depuis » = date de création de la fiche, pas la date d'affectation), BUG-056 (toutes les alertes listées, pas seulement `OPEN`), BUG-053 conservé (statut `DEFAUT` sans badge).
- Alertes de fraude : confirmer ou classer en faux positif ne change jamais le virement suspendu (ni débit, ni libération, ni rejet, ni audit, ni notification). Transition `OPEN` uniquement (`UPDATE … WHERE statut = 'OPEN'`), une seule issue en cas de concurrence ; auteur de la revue enregistré par identifiant, affiché par nom complet. À la libération d'un virement, seules les alertes encore `OPEN` passent à `REVIEWED`.
- Journal des traitements : 50 entrées les plus récentes, tri par début décroissant, heures en Europe/Paris ; section « Traitements nocturnes » sans « (EJB Timer @Schedule) ».
- Chaque story se clôt par `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh` et `golden-master.sh` verts. Les scénarios SC-015-01, SC-015-02 (9.2) et SC-016-01, SC-016-02 (9.3), SC-016-01 (9.6) passent par `scripts/golden-master/verifier-scenario.sh` ; ne jamais rejouer le golden master avant l'epic 10.

## Technical Decisions

- Nouveaux points d'API : `GET /api/{identite-clients|coeur-bancaire|credits}/conseiller/portefeuille` ; `GET /api/identite-clients/administration/{indicateurs|clients|utilisateurs}` ; `GET /api/credits/administration/indicateurs` ; `GET /api/coeur-bancaire/administration/alertes-fraude` et `POST …/alertes-fraude/{alertId}/{confirmation|faux-positif}` ; journal `GET /api/coeur-bancaire/traitements?limite=50`.
- Services sans appel HTTP entre eux : coeur-bancaire et credits tiennent une projection clients alimentée par `identite-clients.client-modifie.v1` (porte `conseillerId`, `dateCreation`, `versionAgregat`) ; un changement d'affectation se reflète avec la latence de projection.
- Les `*SecuriteIT` de chaque service sont complétés (401 / 403 / périmètre) ; les tests JUnit portent `@Tag("FR-…")` / `@Tag("BUG-…")`, les tests Vitest ont le motif `[FR-…]` dans le titre.
- Conventions : code métier en français, hexagonal ; montants en chaîne décimale (`"2130.00"`), calculés en centimes entiers côté front par des formateurs de `src/domain` ; heure uniquement via le port `Horloge` ; `data-testid` au format `{écran}-{élément}[-{id}]` (ex. `conseiller-voir-detail-{clientId}`, `admin-detail-{customerId}`, `admin-alerte-confirmer-{alertId}`).
- Désactivation/activation : l'écran réutilise l'API ADMIN d'`identite-clients` (audits `ADMIN_DEACTIVATE` / `ADMIN_ACTIVATE`, publication `client-modifie`) sans la modifier ni émettre d'audit côté front ; un jeton déjà émis reste valable jusqu'à expiration.
- Les erreurs de service s'affichent par section (`conseiller-message-erreur`, `admin-message-erreur`) avec le `message` reçu, sans détail technique ; une section en erreur n'empêche pas les autres.

## UX & Interaction Patterns

- Détail client : panneau inline sur la même page (pas de modale, pas de changement de route) ; le focus va au titre à l'ouverture ; « Fermer » ou `Échap` referme et rend le focus au bouton déclencheur.
- Cartes client : avatar à l'initiale en `aria-hidden`, badge « Actif »/« Inactif » ; un client inactif est distingué par le texte, jamais par la couleur seule. Indicateurs de prêts : « ⚠ n pret(s) en attente » prioritaire sur « ✓ n pret(s) en cours ».
- Activation/désactivation sans modale de confirmation ; bouton `aria-disabled` pendant la requête ; succès en `alert-success` (`role="status"`).
- Ordre des sections de `/administration` figé : indicateurs, Pipeline Prets, Gestion des clients, Historique de tous les prets, Tous les utilisateurs du systeme, Virements suspendus, Alertes Fraude, Traitements nocturnes, Catalogue produits bancaires. Les sections clés (« Gestion des clients », « Jean Dupont », « user1 ») doivent rester dans les 5 000 premiers caractères du texte rendu (fenêtre de comparaison du golden master) ; toute section nouvelle se place après.
- États vides : « Aucun client dans votre portefeuille. », « Aucun client trouve », « Aucune alerte fraude enregistrée », « Aucun traitement nocturne enregistre », « Aucun produit », « Aucun compte », « Aucun pret ».

## Cross-Story Dependencies

- 9.2 dépend de 9.1 ; 9.3 à 9.6 enrichissent la même page `/administration` et se suivent (9.4 et 9.5 après 9.3 ; 9.6 après 9.3 à 9.5, notamment pour le test d'ordre complet des sections).
- Avec les autres epics : topic `client-modifie` et API ADMIN d'activation/désactivation (epic 2, stories 2.6 et 2.7) ; routeur et redirections par rôle (epic 2) ; file des virements suspendus et alertes `FRAUD_ALERTS` (epic 3) ; journal des traitements ouvert à l'ADMIN (epic 4) ; file et historique des prêts (epic 5) ; catalogue produits du service `open-banking-conseiller` (epic 7).
- Décisions préalables : « Client depuis » = date de création de la fiche (A-016-2), auteur de la revue par identifiant (A-016-4), pagination/filtre du journal (RP-01), tous confirmés le 2026-10-05.
