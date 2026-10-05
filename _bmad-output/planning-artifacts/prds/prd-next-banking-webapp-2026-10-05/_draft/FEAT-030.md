### FEAT-030 — Contrôle d'accès RBAC

**Description :** Le contrôle d'accès est une fonction transverse, sans écran propre, qui s'applique à toutes les requêtes des trois rôles CLIENT, CONSEILLER et ADMIN (le rôle d'un nouvel utilisateur est CLIENT par défaut). Il laisse passer sans session la page de connexion, le retour du fournisseur d'open banking et les ressources statiques, renvoie vers la connexion tout autre accès non authentifié, réserve l'espace conseiller aux rôles CONSEILLER et ADMIN et l'administration au rôle ADMIN, et n'affiche les liens de navigation correspondants qu'aux rôles autorisés. Les refus sont des redirections silencieuses, sans message. Le contrôle des rôles est aussi appliqué dans les services métier, et non plus seulement à l'entrée des requêtes. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-030-01 : Accès public limité et redirection des accès non authentifiés

Le système sert sans session authentifiée la page de connexion, la page de retour du fournisseur d'open banking et les ressources statiques de l'interface. Toute autre requête vers un écran, sans utilisateur authentifié dans la session, est redirigée vers la page de connexion. [ASSUMPTION A-030-1 : écart à RG-030-001 — l'exemption est accordée sur le chemin exact de ces écrans, et non à toute adresse qui contient leur nom comme dans le legacy, afin qu'une adresse forgée ne contourne pas l'authentification] Voir aussi FR-001-01 et FR-001-02.

Source : RG-030-001, RG-030-002

#### FR-030-02 : Espace conseiller réservé aux rôles CONSEILLER et ADMIN

Si un utilisateur authentifié dont le rôle n'est ni CONSEILLER ni ADMIN demande l'espace conseiller, le système le redirige vers le tableau de bord, sans message. Avec les trois rôles existants, ce cas est celui du CLIENT décrit par FR-001-11.

Source : RG-030-004

#### FR-030-03 : Écran d'accueil selon le rôle après connexion

Après une authentification réussie, le système redirige un utilisateur ADMIN vers l'écran d'administration, un CONSEILLER vers l'espace conseiller et un CLIENT vers le tableau de bord. Voir aussi FR-001-05 (mêmes règles, vues de la connexion).

Source : RG-030-005, RG-030-006, RG-030-007

#### FR-030-04 : Liens de navigation conditionnés par le rôle

Dans le menu de navigation de tous les écrans authentifiés, le système affiche le lien « Espace Conseiller » seulement si le rôle de l'utilisateur est CONSEILLER ou ADMIN, et le lien « Administration » seulement si son rôle est ADMIN. Voir aussi FR-002-11 (même règle sur le tableau de bord).

Source : RG-030-008, RG-030-009

#### FR-030-05 : Page expirée

Si l'utilisateur soumet une action depuis une page dont l'état de session a expiré, le système le redirige vers la page de connexion.

Source : RG-030-011

#### FR-030-06 : Téléchargement de documents réservé aux utilisateurs authentifiés

Si une demande de téléchargement de document arrive sans utilisateur authentifié dans la session, le système la redirige vers la page de connexion. Tout utilisateur authentifié, quel que soit son rôle, peut accéder à la fonction de téléchargement de documents.

Source : RG-030-012, RG-030-013

#### FR-030-07 : Contrôle des rôles dans les services métier

Le système applique le contrôle d'accès par rôle dans les services métier eux-mêmes, indépendamment du contrôle effectué à l'entrée des requêtes : une fonction réservée à un rôle refuse l'appel d'un utilisateur d'un autre rôle, même si l'appel ne passe pas par le contrôle d'entrée. Cette exigence remplace le comportement legacy RG-030-015 (contrôle à l'entrée des requêtes uniquement).

Critère d'acceptation (test) : un appel direct à une fonction d'administration (par exemple la désactivation d'un client) avec un utilisateur CLIENT ou CONSEILLER, sans passer par le contrôle d'entrée, est refusé et ne modifie aucune donnée ; le même appel avec un utilisateur ADMIN réussit.

Source : RG-030-015, BUG-093

#### FR-030-08 : Nouvelle session après authentification

Lorsqu'un utilisateur s'authentifie avec succès, le système invalide la session existante et en crée une nouvelle avant d'y enregistrer le contexte d'authentification. Cette exigence remplace le comportement legacy RG-030-014 (session conservée). Voir aussi FR-001-09 (même correction, vue de la connexion).

Critère d'acceptation (test) : l'identifiant de session obtenu avant la connexion est différent de celui obtenu après ; l'ancien identifiant ne donne accès à aucun écran protégé.

Source : RG-030-014, BUG-092

**Notes :**
- RG-030-003 : couverte par l'exigence fusionnée de FEAT-001 (règles voisines), FR-001-11.
- RG-030-010 : couverte par l'exigence fusionnée de FEAT-001 (règles voisines), FR-001-10.
- RG-030-014 et RG-030-015 (Suspect, dette legacy) sont couvertes par les FR de correction FR-030-08 et FR-030-07 ; leur comportement legacy n'est pas reproduit.
- Les refus d'accès sont des redirections sans texte affiché. Les messages d'erreur du téléchargement de documents (« Parametre type manquant », « accountId manquant », « Type inconnu : {type} », « Erreur lors de la generation du document ») sont inventoriés dans le corpus de cette fonctionnalité mais relèvent de la fonctionnalité Documents.
