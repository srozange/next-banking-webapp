### FEAT-001 — Connexion

**Description :** La connexion est le point d'entrée unique de tous les utilisateurs (CLIENT, CONSEILLER, ADMIN) : un formulaire « Identifiant » / « Mot de passe » ouvre une session authentifiée, puis oriente chaque utilisateur vers l'écran d'accueil de son rôle (tableau de bord, espace conseiller ou administration). Tout écran protégé demandé sans session renvoie vers la page de connexion. Les cas limites marquants sont les champs vides, les identifiants incorrects (message générique identique pour un identifiant inconnu et un mauvais mot de passe), le compte désactivé par l'administrateur (désormais refusé) et la déconnexion. Chaque connexion, échec et déconnexion est journalisé. Scénarios golden master : SC-001-01, SC-001-02, SC-001-03, SC-001-04, SC-001-05.

**Exigences fonctionnelles :**

#### FR-001-01 : Écrans accessibles sans authentification

La page de connexion, la page de retour du fournisseur d'open banking et les ressources statiques de l'interface (styles, scripts, images) sont servies sans session authentifiée. La page de connexion affiche le titre d'onglet « BankApp Demo - Connexion », le logo « DB », le titre « Demo Bank », le sous-titre « Espace client — Connexion sécurisée », les libellés « Identifiant » et « Mot de passe » et le bouton « Me connecter ».

Source : RG-001-002

#### FR-001-02 : Redirection des accès non authentifiés vers la connexion

Lorsqu'un utilisateur non authentifié demande tout autre écran que ceux de FR-001-01, le système le redirige vers la page de connexion. Voir aussi FR-030-01 (même règle, vue du contrôle d'accès).

Source : RG-001-001

#### FR-001-03 : Champs obligatoires du formulaire de connexion

Si le champ « Identifiant » est vide à la soumission, le système rejette le formulaire et affiche « Identifiant requis » ; si le champ « Mot de passe » est vide, il affiche « Mot de passe requis ». Les deux messages sont visibles par l'utilisateur à côté du champ concerné, et le contrôle est également appliqué côté serveur.

Source : RG-001-008, RG-001-009

#### FR-001-04 : Authentification réussie et ouverture de session

Lorsqu'un utilisateur soumet un identifiant et un mot de passe qui correspondent à un utilisateur enregistré et actif (voir FR-001-07), le système ouvre une session authentifiée portant le contexte de cet utilisateur, puis le redirige vers l'écran d'accueil de son rôle (FR-001-05).

Source : RG-001-003

#### FR-001-05 : Écran d'accueil selon le rôle

Après une connexion réussie, un utilisateur ADMIN est redirigé vers l'écran d'administration, un CONSEILLER vers l'espace conseiller et un CLIENT vers le tableau de bord. Voir aussi FR-030-03 (mêmes règles, vues du contrôle d'accès).

Source : RG-001-004, RG-001-005, RG-001-006

#### FR-001-06 : Échec d'authentification

Lorsque l'identifiant ne correspond à aucun utilisateur enregistré, ou que le mot de passe est faux, le système reste sur la page de connexion et affiche le message « Identifiants incorrects » accompagné du détail « Vérifiez votre login et mot de passe » (forme combinée du corpus : « Identifiants incorrects — Vérifiez votre login et mot de passe »). Le message est le même dans les deux cas.

Source : RG-001-007

#### FR-001-07 : Refus de connexion d'un compte désactivé

Si un utilisateur dont le compte a été désactivé par l'administrateur soumet des identifiants valides, le système refuse la connexion : aucune session authentifiée n'est ouverte et l'utilisateur reste sur la page de connexion. Cette exigence remplace le comportement legacy RG-001-017 (connexion acceptée malgré la désactivation). [ASSUMPTION A-001-1 : le refus affiche le même message que FR-001-06 (« Identifiants incorrects » / « Vérifiez votre login et mot de passe ») et produit une entrée d'audit LOGIN_FAILED, pour ne pas révéler l'existence du compte]

Critère d'acceptation (test) : un utilisateur désactivé qui saisit son identifiant et son mot de passe corrects reste sur la page de connexion, n'accède à aucun écran protégé, et peut se connecter de nouveau après réactivation par l'administrateur.

Source : RG-001-017, BUG-001

#### FR-001-08 : Mots de passe protégés par hachage

Le système stocke les mots de passe sous forme hachée par une fonction de hachage sécurisée à sens unique, et vérifie un mot de passe saisi en le comparant à cette empreinte ; aucun mot de passe n'est stocké ni comparé en clair, y compris pour les utilisateurs des données de référence rechargées. Cette exigence remplace le comportement legacy RG-001-018 (stockage et comparaison en clair).

Critère d'acceptation (test) : après création ou chargement d'un utilisateur, la valeur stockée pour son mot de passe diffère du mot de passe saisi et ne permet pas de le retrouver ; la connexion avec le bon mot de passe réussit, avec un mauvais mot de passe elle échoue.

Source : RG-001-018, BUG-002

#### FR-001-09 : Nouvelle session à la connexion

Lorsqu'un utilisateur s'authentifie avec succès, le système invalide la session existante avant d'enregistrer le contexte de l'utilisateur authentifié, de sorte que l'identifiant de session antérieur à la connexion ne donne accès à rien. Cette exigence remplace le comportement legacy RG-001-019 (session conservée). Voir aussi FR-030-08.

Critère d'acceptation (test) : l'identifiant de session obtenu avant la connexion est différent de celui obtenu après ; une requête vers un écran protégé avec l'ancien identifiant est redirigée vers la page de connexion.

Source : RG-001-019, BUG-003

#### FR-001-10 : Déconnexion

Lorsqu'un utilisateur se déconnecte, le système invalide la session courante et le redirige vers la page de connexion. Les deux formulations du corpus (connexion et contrôle d'accès) sont identiques : invalidation de la session puis redirection vers la page de connexion.

Source : RG-001-010, RG-030-010

#### FR-001-11 : Accès refusé aux écrans réservés à un autre rôle

Lorsqu'un utilisateur authentifié dont le rôle n'est pas ADMIN demande l'écran d'administration, le système le redirige vers le tableau de bord, sans message. Lorsqu'un utilisateur authentifié de rôle CLIENT demande l'espace conseiller, le système le redirige vers le tableau de bord. Les deux formulations du corpus pour l'administration (connexion et contrôle d'accès) sont identiques. Voir aussi FR-030-02 pour l'espace conseiller.

Source : RG-001-011, RG-030-003, RG-001-012

#### FR-001-12 : Journalisation des connexions et déconnexions

À chaque connexion réussie, le système enregistre une entrée d'audit d'action LOGIN, de détail « Connexion réussie », avec l'adresse IP du client. À chaque tentative échouée, une entrée d'action LOGIN_FAILED, de détail « Tentative échouée », avec l'adresse IP du client. À chaque déconnexion, une entrée d'action LOGOUT, de détail « Déconnexion ». Voir FR-026-01 pour le contenu complet des entrées et FR-026-05 pour l'adresse IP de toutes les actions.

Source : RG-001-013, RG-001-014, RG-001-015

#### FR-001-13 : Résolution de l'adresse IP du client

Pour renseigner l'adresse IP d'une entrée d'audit, le système retient le premier élément de l'en-tête de transfert « X-Forwarded-For » s'il est présent, non vide et posé par un mandataire de confiance (A-001-2), sinon l'adresse distante de la connexion. [ASSUMPTION A-001-2 : écart à RG-001-016, qui retient l'en-tête sans condition : l'en-tête n'est pris en compte que lorsqu'il est posé par un mandataire de confiance de l'infrastructure ; sinon l'adresse distante de la connexion est retenue, pour éviter qu'un client falsifie son adresse dans l'audit]

Source : RG-001-016

**Notes :**
- Les messages « Identifiant requis » et « Mot de passe requis » sont invisibles en production dans le legacy (dette constatée dans le corpus de validation) ; FR-001-03 les rend visibles, conformément à RG-001-008 et RG-001-009.
- Le badge technique du pied de formulaire (pile technique du legacy) n'est pas repris. [ASSUMPTION A-001-3 : les bandeaux techniques du legacy, sans valeur fonctionnelle, ne sont pas reproduits et sont exclus de la comparaison du golden master]
- RG-001-017, RG-001-018 et RG-001-019 (Suspect, dette legacy) sont couvertes par les FR de correction FR-001-07, FR-001-08 et FR-001-09 ; leur comportement legacy n'est pas reproduit.
- FR-001-10 et FR-001-11 sont les exigences fusionnées des règles voisines RG-001-010 + RG-030-010 et RG-001-011 + RG-030-003 (porteuse FEAT-001).
