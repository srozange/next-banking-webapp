### FEAT-012 — Comptes Tiers — Open Banking Powens

**Description :** Tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) peut, depuis l'écran « Comptes Tiers », agréger ses comptes détenus dans d'autres banques grâce au fournisseur d'open banking Powens. L'écran a deux états : non connecté (bloc de connexion avec les banques partenaires et le bouton « Connecter une banque ») et connecté (patrimoine total, comptes externes, transactions du compte sélectionné, bouton « Deconnecter »). La connexion passe par la page web de Powens, qui renvoie l'utilisateur vers l'application (retour traité par FEAT-014). Les cas limites marquants sont l'échec d'initialisation, le retour incomplet ou en erreur, la session Powens expirée, la connexion réussie sans compte encore disponible, le solde absent et l'indisponibilité du service. Aucun champ de saisie : seules des actions et des sélections. Scénarios golden master : SC-012-01.

**Exigences fonctionnelles :**

#### FR-012-01 : Accès à l'écran Comptes Tiers

L'écran « Comptes Tiers » est réservé aux utilisateurs authentifiés : un utilisateur non authentifié qui le demande est redirigé vers la page de connexion. Tous les rôles authentifiés (CLIENT, CONSEILLER, ADMIN) y accèdent, avec le même contenu.

Source : RG-012-001, RG-012-002

#### FR-012-02 : Retour de Powens accessible sans authentification

Le point de retour de Powens (page de retour de connexion) est accessible sans session authentifiée ; son traitement est décrit par FR-014-01 et suivantes.

Source : RG-012-003

#### FR-012-03 : État non connecté

Quand aucune connexion Powens n'est établie dans la session de l'utilisateur, l'écran affiche le bloc de connexion : le titre « Agregez vos comptes bancaires », le texte « Connectez vos comptes d autres banques pour avoir une vue complete de votre patrimoine financier. Connexion securisee via le protocole OAuth2 PSD2. », les banques partenaires « BNP Paribas », « Credit Agricole », « La Banque Postale », « CIC », « LCL », « + 1800 banques », et le bouton « Connecter une banque ».

Source : RG-012-004

#### FR-012-04 : État connecté

Quand une connexion Powens est établie, le bloc de connexion est masqué et l'écran affiche : le badge « Connected via Powens » et le bouton « Deconnecter » ; la carte « Patrimoine total (tous comptes) » avec la mention « via Powens Open Banking API » ; la section « Mes comptes externes ({N}) », {N} étant le nombre de comptes chargés ; la section des transactions.

Source : RG-012-005

#### FR-012-05 : Lancement de la connexion à une banque

Au clic sur « Connecter une banque », le système crée auprès de Powens un utilisateur anonyme et obtient un jeton d'accès permanent, puis un code temporaire ; il conserve le jeton dans la session de l'utilisateur, construit l'adresse de la page web de connexion Powens (domaine, identifiant client, adresse de retour, code temporaire, banques proposées) et y redirige le navigateur (détail de l'échange : FR-028-01). Si l'initialisation échoue (pas de réponse, réponse en erreur, ou jeton absent), le système affiche « Impossible d initialiser la connexion Powens. » et reste à l'état non connecté, sans redirection.

Source : RG-012-006, RG-012-007

#### FR-012-06 : Aiguillage du retour de Powens

Au retour de Powens : si un identifiant de connexion (`connection_id`) est reçu, le chargement des comptes externes est lancé avec cet identifiant ; si seul un code est reçu, le chargement des comptes n'est pas lancé : le retour est traité comme incomplet (FR-014-04) [ASSUMPTION A-014-1 : écart à RG-012-011 (chargement des comptes avec le seul code), non reprise, et à RG-012-008, dont la condition est élargie au retour qui ne porte qu'un code, tant que Powens n'a pas confirmé l'usage du code (résolution de RG-014-004)] ; si ni l'un ni l'autre n'est reçu (et pas de paramètre d'erreur), le système affiche « Callback Powens incomplet. Parametres recus: [<liste des paramètres reçus>] » et reste à l'état non connecté. L'ordre de priorité entre les paramètres est celui de FR-014-02.

Source : RG-012-008, RG-012-010, RG-012-011

#### FR-012-07 : Retour de Powens en erreur (correction BUG-040, BUG-044)

Quand le retour de Powens porte un paramètre d'erreur (par exemple une annulation par l'utilisateur), le système déconnecte la session Powens (FR-012-15), revient à l'état non connecté et affiche un message qui explique le motif du retour ; cela remplace le comportement legacy RG-012-009 (retour silencieux). [ASSUMPTION A-012-1 : le message est « La connexion a votre banque n a pas abouti ({motif}). Veuillez recommencer la connexion. », {motif} étant la valeur du paramètre d'erreur reçu ; il est posé après la remise à zéro de la session, pour ne pas être effacé par elle]

Critère d'acceptation (test) : un utilisateur authentifié qui arrive sur le retour Powens avec `error=user_cancelled` est redirigé vers l'écran « Comptes Tiers », voit le bloc « Agregez vos comptes bancaires » et un message d'erreur non vide qui contient « user_cancelled ».

Source : BUG-040, BUG-044, RG-012-009

#### FR-012-08 : Session Powens expirée

Si, au traitement du retour de Powens, la session ne contient plus le jeton Powens, le système affiche « Session expiree. Veuillez recommencer la connexion. » et reste à l'état non connecté.

Source : RG-012-012

#### FR-012-09 : Résultat du chargement des comptes après le retour

Si le chargement réussit et renvoie au moins un compte, l'écran passe à l'état connecté, le premier compte de la liste est sélectionné automatiquement et ses transactions sont chargées. Si le chargement réussit mais ne renvoie aucun compte, l'écran passe à l'état connecté et affiche « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. ». Un chargement en erreur suit FR-012-14, et non ce message. Aucune attente n'est imposée avant la lecture des comptes : la pause bloquante du legacy décrite par RG-012-014 n'est pas reproduite (A-012-6, voir les notes de FEAT-012).

Source : RG-012-013, RG-012-014

#### FR-012-10 : Patrimoine total

Le patrimoine total est la somme des soldes connus de tous les comptes externes ; un compte dont le solde est absent n'y contribue pas. Le montant est affiché en euros, avec séparateur de milliers et deux décimales.

Source : RG-012-015

#### FR-012-11 : Carte d'un compte externe

Chaque compte externe est présenté par une carte avec le libellé fixe « Banque externe », le nom du compte, son type, son solde (deux décimales, séparateur de milliers) et son IBAN. Le type est traduit : checking → « Compte courant », savings → « Epargne », card → « Carte », loan → « Credit », market → « Bourse », type absent → « Compte » ; tout autre type est affiché tel que renvoyé par Powens. L'IBAN n'est affiché que si Powens renvoie une valeur non vide.

Source : RG-012-016, RG-012-017

#### FR-012-12 : Solde absent (correction BUG-042)

Quand Powens ne renvoie pas le solde d'un compte (champ absent ou vide), la carte du compte l'indique comme indisponible au lieu d'afficher « 0,00 EUR » ; un solde réellement nul reste affiché « 0,00 EUR ». Cette exigence remplace le comportement legacy RG-012-018. Ce remplacement vaut aussi pour la règle legacy RG-028-020 (voir FR-028-09). [ASSUMPTION A-012-2 : le libellé affiché est « Solde indisponible »]

Critère d'acceptation (test) : avec un Powens simulé qui renvoie un compte A sans solde et un compte B au solde 0, la carte A affiche « Solde indisponible », la carte B affiche « 0,00 », et le patrimoine total vaut la somme des seuls soldes renvoyés.

Source : BUG-042, RG-012-018

#### FR-012-13 : Transactions d'un compte externe

Au clic sur un compte externe, ses transactions sont chargées et seule la section des transactions est mise à jour, sans rechargement de l'écran. La section porte le titre « Transactions — {nom du compte} » et les colonnes « Date », « Libelle », « Montant » ; sans transaction, elle affiche « Aucune transaction disponible. ». Au plus 20 transactions par compte sont récupérées (même limite que FR-014-08). Le libellé d'une transaction est son libellé simplifié, ou, s'il est vide, son libellé d'origine. Un montant strictement positif est un crédit, affiché en vert avec le préfixe « + » ; un montant nul ou négatif est un débit, affiché en rouge sans préfixe « + ».

Source : RG-012-019, RG-012-020, RG-012-021, RG-012-022, RG-012-023

#### FR-012-14 : Service Powens indisponible (correction BUG-043)

Si Powens répond en erreur (statut 400 ou plus) au chargement des comptes ou des transactions, le système informe l'utilisateur de l'indisponibilité temporaire du service, au lieu d'afficher une liste vide comme « aucun compte » ou « Aucune transaction disponible. » ; cela remplace le comportement legacy RG-012-027. [ASSUMPTION A-012-3 : le message est « Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants. », affiché dans la zone d'erreur de l'écran] [ASSUMPTION A-012-4 : si l'erreur survient au chargement des comptes après le retour de Powens, l'écran reste à l'état non connecté et le message « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. » n'est pas affiché]

Critère d'acceptation (test) : avec un Powens simulé qui répond 503 à la lecture des comptes, l'écran affiche le message d'indisponibilité et n'affiche ni « Connexion reussie mais aucun compte disponible encore » ni « Mes comptes externes (0) » ; avec un Powens simulé qui répond 500 à la lecture des transactions, la section des transactions affiche le message d'indisponibilité et non « Aucune transaction disponible. ».

Source : BUG-043, RG-012-027

#### FR-012-15 : Déconnexion de Powens

Au clic sur « Deconnecter », le système efface le jeton Powens, la liste des comptes, le compte sélectionné, les transactions et le message d'erreur, et repasse à l'état non connecté.

Source : RG-012-024

#### FR-012-16 : Rafraîchissement des comptes externes

À l'état connecté, l'utilisateur peut rafraîchir la liste de ses comptes externes sans se déconnecter ni se reconnecter ; le comportement du rafraîchissement est celui de FR-028-08. [ASSUMPTION A-012-5 : l'action est exposée par un bouton « Rafraichir » à côté de « Deconnecter » ; elle n'existe pas à l'écran dans le legacy]

Source : RG-012-028

#### FR-012-17 : Paramètres de raccordement à Powens

Le domaine Powens, l'identifiant client, le secret client, l'adresse de retour et les banques proposées sont des paramètres de configuration de déploiement ; leurs valeurs par défaut sont celles de l'environnement de test Powens (domaine demi-sandbox.biapi.pro, identifiant client 99961298, banques 59). Chaque appel à Powens applique un délai de 15 secondes pour la connexion et pour la lecture. Le reste (port, adaptateur simulé par défaut, coupe-circuit, cache) relève de NFR-INT.

Source : RG-012-025, RG-012-026

**Feature-specific NFRs :** le jeton Powens n'est conservé que dans la session de l'utilisateur et n'est jamais affiché ; délai de 15 secondes par appel à Powens (FR-012-17), le reste relevant de NFR-INT.

**Notes :**
- RG-012-014 (dette legacy, À confirmer) : le comportement fonctionnel est repris dans FR-012-09 ; la pause bloquante de 2 secondes qui précède la lecture des comptes n'est pas reproduite. Le bug associé BUG-041 (pause artificielle avant le chargement des comptes) est classé « ne pas corriger » : il n'impose aucune correction, mais une exigence ne peut pas prescrire une pause bloquante. [ASSUMPTION A-012-6 : aucune attente imposée avant la lecture des comptes ; si la synchronisation Powens n'est pas terminée, FR-012-09 (« Connexion reussie mais aucun compte disponible encore… ») et le rafraîchissement FR-012-16 couvrent le cas]
- RG-012-009, RG-012-018, RG-012-027 (Suspect, dette legacy) : remplacées par les corrections FR-012-07, FR-012-12 et FR-012-14.
- Le bandeau technique du legacy sous la navigation n'est porté par aucune règle et n'est pas repris.
