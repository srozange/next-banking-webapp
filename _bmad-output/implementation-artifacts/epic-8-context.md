# Epic 8 Context: Agréger ses comptes d'autres banques (Powens)

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Permettre à tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) d'agréger ses comptes d'autres banques : depuis l'écran « Comptes Tiers » il connecte une banque par la page web de Powens, revient dans l'application par le retour public, voit son patrimoine total, ses comptes externes et les transactions du compte sélectionné, puis rafraîchit ou déconnecte. Le tableau de bord affiche le patrimoine total Demo Bank + comptes externes. Les retours en erreur, le solde absent et l'indisponibilité de Powens sont signalés à l'utilisateur (BUG-040, BUG-042, BUG-043, BUG-044, BUG-089 corrigés).

## Stories

- Story 8.1: Raccorder Powens derrière un port et lancer la connexion à une banque
- Story 8.2: Traiter le retour public de Powens
- Story 8.3: Consulter, sélectionner, rafraîchir et déconnecter ses comptes externes côté service
- Story 8.4: Afficher l'écran « Comptes Tiers »
- Story 8.5: Afficher le patrimoine total avec les comptes externes sur le tableau de bord

## Requirements & Constraints

- Aucun test ni contrôle ne dépend du réseau ni d'une clé. Jamais de `mode=reel` committé (`! grep -rnE 'mode *[:=] *reel|_MODE *[:=] *reel' services/*/src/main/resources compose*.yaml` sort en 0). Le mode réel sans secret client fait échouer le démarrage.
- Le secret client et le jeton Powens n'apparaissent jamais : ni dans une réponse d'API, ni dans les journaux, ni dans le front (ni `localStorage` ni `sessionStorage`), ni dans un fichier versionné.
- Défauts Powens : domaine `demi-sandbox.biapi.pro`, identifiant client `99961298`, banques proposées `59`. Délais de l'adaptateur réel : 15 s de connexion, 15 s de lecture.
- Une panne de Powens (5xx, 404 aux transactions, coupe-circuit ouvert) n'est jamais présentée comme une liste vide : 503 `{"message": "Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants."}` (BUG-043, BUG-089).
- Un solde absent est distinct de zéro : « Solde indisponible » et non « 0,00 », et exclu du patrimoine total (BUG-042).
- Au plus 20 transactions demandées et enregistrées ; libellé simplifié, sinon libellé d'origine. Un paramètre vide du retour compte comme absent.
- Messages d'état repris à l'identique (casse et absence d'accents comprises) : retour en erreur « La connexion a votre banque n a pas abouti ({motif}). Veuillez recommencer la connexion. », « Session expiree. Veuillez recommencer la connexion. », « Callback Powens incomplet. Parametres recus: [...] » (liste des seuls paramètres reçus autres que `state`), « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. », « Impossible d initialiser la connexion Powens. » (503). Le bandeau technique « Open Banking — … EJB WildFly 14 » est supprimé.
- Sans jeton JWT : 401 partout sauf le retour public. Un compte absent des comptes externes du client (dont celui d'un autre client) : 404 `{"message": "Ressource introuvable"}`, sans appel à Powens.
- Chaque FR et bug corrigé est couvert par des tests tagués (`-Dgroups='<tag>'` côté Maven, motif `[<tag>]` dans le titre Vitest). Contrôle de fin d'epic par l'humain : `scripts/golden-master/verifier-scenario.sh SC-012-01` ; SC-014-01 (HTTP) est couvert par le test d'intégration de la story 8.2 ; SC-001-04, SC-001-05, SC-002-01 et SC-002-02 doivent rester verts après la story 8.5. Ne jamais rejouer le golden master avant l'epic 10.
- Chaque story se clôt par `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` et `trace-smoke.sh` verts.

## Technical Decisions

- Service `open-banking-conseiller` (créé par l'epic 7) : Powens est appelé derrière un port sortant de `application.port.out` (utilisateur anonyme, code temporaire, adresse de la page web, comptes, transactions), avec deux adaptateurs dans `adapter.out.fournisseur` : `PowensSimule` (actif par défaut, sans réseau, connecteurs « BNP Paribas » et « Credit Agricole », comptes et transactions fixes, modes d'erreur sélectionnables par les tests) et un adaptateur réel. Seules ces classes dépendent d'un client HTTP (`ClientsHttpArchitectureTest`). Réglages sous `nb.fournisseurs.powens.*`, validés au démarrage.
- Coupe-circuit (`resilience4j-circuitbreaker`) et cache (Caffeine) choisis en epic 7 : à réutiliser sans nouveau choix, uniquement les versions de `versions-verifiees.md`. Un coupe-circuit ouvert produit une erreur d'indisponibilité.
- État Powens conservé côté serveur, en base `open-banking-conseiller`, par client (clé `sub` du JWT) : jeton permanent, comptes, compte sélectionné, transactions, indicateur de connexion, message. Pas de session serveur. La déconnexion et le retour en erreur remettent tout à zéro (le message est posé après la remise à zéro).
- Connexion : création d'un utilisateur Powens anonyme, jeton permanent enregistré avant de construire l'adresse, code temporaire, `state` signé à usage unique lié au client. Les autres appels s'authentifient avec le jeton permanent. Avec `PowensSimule`, l'adresse renvoyée est celle du retour avec un `connection_id` fixe et le `state`.
- Points d'API (tous authentifiés sauf le retour) : `POST /api/open-banking-conseiller/powens/connexion`, `GET …/powens` (état lu en base, sans appel à Powens, patrimoine total externe en chaîne `"120.50"`), `GET …/powens/retour` (public, chemin exact seulement, répond 302 vers `/comptes-tiers`, sans corps, état et `state` invalides sans effet), `POST …/powens/comptes/{compteId}/selection`, `POST …/powens/rafraichissement`, `DELETE …/powens/connexion` (204).
- Retour : seul le cas `error` est traité s'il est présent ; un `connection_id` lance la lecture immédiate des comptes (premier compte sélectionné, transactions chargées) ; `code` seul = retour incomplet.
- Conventions : montants en chaîne décimale à 2 décimales ; entre services, `compteId` numérique ; le patrimoine total est calculé par le front en centimes entiers, par un formateur de `src/domain`, à partir des totaux du cœur bancaire et d'`open-banking-conseiller` ; temps uniquement par le port `Horloge` ; pas d'appel HTTP entre services.
- Chaque story de service lance `SecuriteIT` étendu aux nouveaux chemins.

## UX & Interaction Patterns

- Route `/comptes-tiers` : visiteur non authentifié renvoyé vers `/login` ; même contenu pour CLIENT, CONSEILLER et ADMIN ; préfixe `data-testid` `tiers-` (`tiers-connecter-banque`, `tiers-deconnecter`, `tiers-rafraichir`, `tiers-message-erreur`, `tiers-compte-{compteId}`).
- Non connecté : bloc « Agregez vos comptes bancaires » avec la liste des banques et le bouton « Connecter une banque » (envoie le navigateur à l'adresse renvoyée ; sur 503, message d'erreur sans changer d'adresse). Connecté : bloc de connexion masqué, « Connected via Powens », « Deconnecter » et « Rafraichir » côte à côte, carte « Patrimoine total (tous comptes) », « Mes comptes externes (N) », transactions du compte sélectionné.
- Cartes de comptes : « Banque externe », nom, type traduit (« Compte courant », « Epargne », « Carte », « Credit », « Bourse », « Compte », sinon valeur brute), solde au format legacy, IBAN seulement s'il est non vide. Un clic sur un compte ne met à jour que la section des transactions (20 lignes au plus) ; montant > 0 en vert préfixé « + », sinon rouge ; vide : « Aucune transaction disponible. », sauf en cas de panne (message d'indisponibilité à la place).
- Tableau de bord (8.5) : avec Powens, section « Mes comptes externes (Open Banking) », carte « Patrimoine total » au sous-titre « Demo Bank + comptes externes », bouton « Voir le detail et les transactions » ; sans Powens, invitation « Connecter mes autres banques via Powens ». Ces sections se placent après le contenu comparé par le golden master. L'appel à Powens part en parallèle de ceux du cœur bancaire ; s'il échoue, le reste du tableau s'affiche et aucune somme fausse n'est montrée.

## Cross-Story Dependencies

- 8.1 → 8.2 → 8.3 (service) ; 8.4 dépend de 8.1 à 8.3 ; 8.5 dépend de 8.3 (patrimoine total externe) et 8.4 (route `/comptes-tiers`).
- Avec les autres epics : service `open-banking-conseiller`, vérification JWT (`SecuriteIT`) et bibliothèque de coupe-circuit/cache livrés par l'epic 7 ; gabarit, navigation, routeur du front et `scripts/golden-master/verifier-scenario.sh` livrés par l'epic 2 ; tableau de bord et « Solde total Demo Bank » livrés par l'epic 3 ; liste fermée des chemins publics (seul le chemin exact du retour est public).
- Arbitrages confirmés le 2026-10-05 : retour avec seul `code` = incomplet (A-014-1), texte du retour en erreur (A-012-1), « Solde indisponible » (A-012-2), message d'indisponibilité (A-012-3, A-012-4), bouton « Rafraichir » exposé (A-012-5). La valeur de production de `POWENS_CONNECTOR_ID` reste hors démonstrateur (défaut 59).
