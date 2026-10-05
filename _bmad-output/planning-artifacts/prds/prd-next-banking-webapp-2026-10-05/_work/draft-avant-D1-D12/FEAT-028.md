### FEAT-028 — Intégration Powens (Open Banking — agrégation)

**Description :** Fonction transverse, sans écran propre, qui raccorde l'application au fournisseur d'open banking Powens pour agréger les comptes détenus dans d'autres banques. Elle sert l'écran « Comptes Tiers » (FEAT-012) et le point de retour de Powens (FEAT-014), donc tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN). Elle couvre l'initiation de la connexion (utilisateur Powens anonyme, jeton permanent, code temporaire, page web de connexion), le traitement du retour, le chargement des comptes et des transactions, le rafraîchissement, la déconnexion et la configuration. Les cas limites sont l'échec d'initialisation, le jeton absent de la session, la liste de comptes vide, le solde absent et les réponses en erreur de Powens. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-028-01 : Initiation de la connexion Powens

Quand l'utilisateur demande à connecter une banque externe, le système : (1) crée un utilisateur Powens anonyme en s'authentifiant avec l'identifiant et le secret client de l'application, et obtient un jeton permanent et un identifiant numérique d'utilisateur ; (2) conserve le jeton permanent dans la session de l'utilisateur avant toute redirection ; (3) obtient un code temporaire en s'authentifiant avec ce jeton ; (4) construit l'adresse de la page web de connexion Powens avec le domaine, l'identifiant client, l'adresse de retour (encodée), le code temporaire (encodé) et les banques proposées. Tous les appels à Powens autres que la création de l'utilisateur s'authentifient avec le jeton permanent.

Source : RG-028-001, RG-028-002, RG-028-003, RG-028-005, RG-028-026

#### FR-028-02 : Échec de l'initiation

Si la création de l'utilisateur Powens échoue ou ne renvoie pas de jeton, le système affiche « Impossible d initialiser la connexion Powens. » et abandonne la connexion sans rediriger l'utilisateur (même comportement que FR-012-05).

Source : RG-028-004

#### FR-028-03 : Traitement du retour de Powens

Le point de retour de Powens est accessible sans authentification (FR-014-01). Un retour portant un paramètre d'erreur remet à zéro l'état Powens de la session (FR-028-10) et informe l'utilisateur du motif (FR-012-07, correction BUG-040) ; un `connection_id` déclenche le chargement des comptes externes ; sans `connection_id` ni `code`, le système affiche un message d'erreur qui liste les noms des paramètres réellement reçus : « Callback Powens incomplet. Parametres recus: [<liste>] ». Dans tous les cas, l'utilisateur est ensuite redirigé vers l'écran « Comptes Tiers ». L'ordre de priorité est celui de FR-014-02.

Source : RG-028-006, RG-028-007, RG-028-008, RG-028-010, RG-028-011

#### FR-028-04 : Retour portant seulement un code

Le corpus prévoit qu'en l'absence de `connection_id`, un paramètre `code` soit traité comme identifiant de connexion pour lancer le chargement des comptes. Cette branche suit la décision de FR-014-04 : non reprise tant que Powens n'a pas confirmé son usage.

Source : RG-028-009

#### FR-028-05 : Jeton absent au retour

Si le jeton Powens de la session est absent au traitement du retour, le système affiche « Session expiree. Veuillez recommencer la connexion. » et abandonne le chargement des comptes.

Source : RG-028-013

#### FR-028-06 : Résultat du chargement des comptes

Quand la lecture des comptes réussit, la connexion Powens est marquée établie, qu'il y ait des comptes ou non. S'il y a au moins un compte, le premier est sélectionné automatiquement et ses transactions sont chargées. S'il n'y en a aucun, le système affiche « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. », sans traiter la connexion comme un échec. Une lecture en erreur n'est pas une réussite : elle suit FR-028-11.

Source : RG-028-014, RG-028-015, RG-028-016

#### FR-028-07 : Transactions d'un compte sélectionné

Quand l'utilisateur sélectionne un compte externe, ses transactions sont chargées (au plus 20, FR-014-08). Le libellé d'une transaction est son libellé simplifié ; s'il est absent ou vide, le libellé d'origine est utilisé.

Source : RG-028-017, RG-028-019

#### FR-028-08 : Rafraîchissement des comptes

Au rafraîchissement, le système relit la liste des comptes auprès de Powens ; si aucun compte n'était sélectionné et qu'au moins un compte est renvoyé, le premier est sélectionné automatiquement et ses transactions sont chargées. L'action est exposée à l'écran par FR-012-16.

Source : RG-028-022

#### FR-028-09 : Solde absent et patrimoine total (correction BUG-042)

Quand Powens ne renvoie pas le solde d'un compte (champ absent ou vide), le solde est représenté comme « absent », distinct d'un solde nul, et l'écran l'affiche comme indisponible (FR-012-12) ; cela remplace le comportement legacy RG-028-020, qui le représentait comme zéro. Le patrimoine total additionne les soldes de tous les comptes dont le solde est renvoyé ; un solde absent n'y contribue pas (il compte pour zéro dans la somme, sans être affiché comme 0,00).

Critère d'acceptation (test) : avec un Powens simulé qui renvoie un compte sans solde et un compte au solde 120,50, le premier compte est marqué « solde absent » (et non 0), et le patrimoine total vaut 120,50.

Source : BUG-042, RG-028-020, RG-028-021

#### FR-028-10 : Déconnexion

À la déconnexion de Powens, le système efface tout l'état Powens de la session : jeton, liste des comptes, compte sélectionné, liste des transactions, indicateur de connexion et message d'erreur.

Source : RG-028-023

#### FR-028-11 : Réponse en erreur de Powens (correction BUG-089)

Si Powens répond en erreur (statut 4xx ou 5xx) à la lecture des comptes ou des transactions, le système transmet une indication d'erreur, distincte d'une liste vide, pour que l'utilisateur soit informé de l'indisponibilité du service (message et affichage : FR-012-14) ; cela remplace le comportement legacy RG-028-025, qui renvoyait une liste vide sans rien signaler.

Critère d'acceptation (test) : avec un Powens simulé qui répond 500 à la lecture des comptes, puis 404 à la lecture des transactions, chaque lecture produit une erreur d'indisponibilité, et non une liste vide ; l'écran n'affiche ni « Mes comptes externes (0) » ni « Aucune transaction disponible. ».

Source : BUG-089, RG-028-025

#### FR-028-12 : Configuration et délais

Le domaine Powens, l'identifiant client, le secret client, l'adresse de retour et les banques proposées sont des paramètres de configuration de déploiement, avec des valeurs par défaut utilisées quand le paramètre est absent (voir FR-012-17). Chaque appel à Powens applique un délai de 15 secondes pour l'établissement de la connexion et pour la lecture de la réponse. Le port, l'adaptateur simulé actif par défaut, le coupe-circuit et le cache relèvent de NFR-INT.

Source : RG-028-024, RG-028-027

**Feature-specific NFRs :** le secret client Powens et le jeton permanent ne sont jamais affichés ni journalisés en clair ; délai de 15 secondes par appel (FR-028-12), le reste relevant de NFR-INT.

**Notes :**
- RG-028-018 : couverte par l'exigence fusionnée de FEAT-014 (règles voisines).
- RG-028-012 (Suspect, dette legacy) : non reprise — le legacy attend de façon bloquante avant de lire les comptes ; le bug associé BUG-088 (attente bloquante de deux secondes au chargement des comptes) est classé « ne pas corriger ». [ASSUMPTION A-028-1 : aucune attente n'est imposée avant la lecture des comptes ; une synchronisation non terminée se traite par le message de liste vide (FR-028-06) et le rafraîchissement (FR-028-08)]
- RG-028-003 pose une question ouverte : la valeur des banques proposées en production (59 en environnement de test). C'est une valeur de configuration de déploiement (FR-028-12), à fournir au déploiement.
- FR-028-03 cite les mêmes comportements que FR-014-02 et FR-012-06 (règles proches non fusionnées par l'utilisateur) ; FR-014-02 fait foi pour l'ordre de priorité.
