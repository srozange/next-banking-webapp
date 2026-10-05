---
title: "PRD : next-banking-webapp (rénovation)"
status: draft
created: 2026-10-05
updated: 2026-10-05
inputDocuments:
  - "_bmad-output/planning-artifacts/briefs/brief-next-banking-webapp-2026-10-05/brief.md"
  - "_bmad-output/planning-artifacts/briefs/brief-next-banking-webapp-2026-10-05/addendum.md"
  - "docs/retro/ (corpus de rétro-ingénierie, commit c05512ecab8af8878d695ccd0f33fc3ece8811a4)"
  - "docs/quality-gates.md"
---

# PRD : next-banking-webapp (rénovation)

*Brouillon complet en mode Fast path, non finalisé. Les suppositions sont marquées `[ASSUMPTION]` et indexées en §10. Les exigences suspendues à la décision ouverte sur la règle RG-027-003 sont marquées `[EN ATTENTE RG-027-003]` (voir QO-1).*

## 0. Objet du document

Ce PRD décrit la rénovation de l'application bancaire existante (le « legacy », pile Java EE analysée au commit `c05512ecab8af8878d695ccd0f33fc3ece8811a4`) en une nouvelle application. Il s'adresse au PM, à l'architecte (`bmad-architecture`), à l'équipe qui découpera les epics et à la boucle de développement.

**Sources :**
- le brief produit (`_bmad-output/planning-artifacts/briefs/brief-next-banking-webapp-2026-10-05/`) ;
- le corpus de rétro-ingénierie `docs/retro/`, qui fait foi pour le comportement fonctionnel ;
- `docs/quality-gates.md`, la référence unique de l'architecture cible et des contrôles.

**Structure :** un glossaire, puis les 30 fonctionnalités du périmètre (§4). Chaque fonctionnalité regroupe des exigences fonctionnelles numérotées `FR-NNN-xx`, où NNN est le numéro de la fonctionnalité du corpus (FEAT-NNN). La ligne `Source :` de chaque FR cite les règles de gestion (`RG-NNN-NNN`) et les bugs à corriger (`BUG-NNN`) qu'elle couvre. Les exigences non fonctionnelles transverses (§7) reprennent intégralement les contrôles de `docs/quality-gates.md`. L'architecture cible imposée et les détails techniques sont dans `addendum.md`.

## 1. Vision

La banque dispose d'une application en ligne fonctionnelle mais sans documentation, bâtie sur une pile Java EE vieillissante, sans API, sans conteneurisation ni intégration continue, et qui porte 50 comportements défectueux que le métier a décidé de corriger. La rénovation livre une application qui rend **exactement le même service** à ses trois populations (clients, conseillers, administrateurs), prouvé par le rejeu du golden master du legacy. Elle corrige ces 50 bugs, et chaque correction est démontrée par un test.

La nouvelle application est construite pour être vérifiée par des outils et non par des relectures : architecture hexagonale contrôlée automatiquement, couverture et mutation chiffrées, CVE bloquées, traces distribuées prouvées. Chaque story est livrée avec ces contrôles au vert. C'est ce qui permet de la développer en grande partie avec des agents, sans dérive.

La portée de cette version est un **démonstrateur réaliste** : le comportement et les exigences de sécurité d'une vraie banque, sans engagement réglementaire de production ni migration de données réelles.

## 2. Utilisateurs cibles

### 2.1 Jobs To Be Done

- **Client** : consulter ses comptes et son activité, virer de l'argent en sécurité (ponctuellement ou de façon programmée), gérer ses bénéficiaires, suivre son budget, demander un prêt, récupérer ses documents, suivre la bourse, agréger ses comptes d'autres banques, poser une question à un conseiller.
- **Conseiller** : voir son portefeuille de clients et leur situation, pour les accompagner.
- **Administrateur** : piloter l'ensemble (clients, prêts à approuver, alertes de fraude, traitements planifiés) et réagir aux anomalies.
- **Système** (sans rôle utilisateur) : traitements planifiés (intérêts, fraude nocturne, virements programmés, soldes faibles, relevés) et traitements asynchrones (virements, approbation des prêts, notifications, audit).

### 2.2 Non-utilisateurs (v1)

- Les utilisateurs anonymes, en dehors de la page de connexion et du retour du fournisseur d'open banking.
- Les applications tierces : aucune API publique n'est exposée à des partenaires. [ASSUMPTION A-G-05 : l'API REST de la nouvelle application est interne au front ; aucune exposition à des tiers en v1]

### 2.3 Parcours utilisateurs clés

- **UJ-1. Claire, cliente, vire 1 200 € à son colocataire.** Elle se connecte, voit son tableau de bord (soldes, dernières opérations du compte courant), ouvre le virement, choisit le compte source et le bénéficiaire, saisit le montant et un motif, puis valide. Le contrôle de fraude s'exécute avant la validation finale. Elle reçoit une notification dans l'application. **Cas limite :** solde insuffisant → refus avec un message clair et non technique (BUG-008, BUG-011). Le sort d'un virement jugé suspect dépend de QO-1. Réalise FEAT-001, FEAT-002, FEAT-003, FEAT-025, FEAT-027.
- **UJ-2. Marc, conseiller, prépare un rendez-vous.** Il ouvre son espace conseiller, ne voit que les clients qui lui sont affectés, consulte la situation de l'un d'eux et la date à laquelle il est devenu client de la banque (BUG-055). Réalise FEAT-015, FEAT-030.
- **UJ-3. Sophie, administratrice, traite la file du matin.** Elle consulte les alertes de fraude de la nuit, toutes statuts confondus (BUG-056), approuve ou refuse les prêts en attente, désactive un client. Ce client ne peut plus se connecter (BUG-001). Chaque action est auditée avec l'adresse IP d'origine (BUG-083). Réalise FEAT-016, FEAT-019, FEAT-026.
- **UJ-4. Claire agrège son compte d'une autre banque.** Depuis « Comptes tiers », elle est redirigée vers le fournisseur d'open banking. Au retour, elle voit ses comptes externes et leurs 20 dernières opérations. Un solde absent est distingué d'un solde nul (BUG-042). En cas d'indisponibilité du fournisseur, elle en est informée (BUG-043). Réalise FEAT-012, FEAT-014, FEAT-028.

[ASSUMPTION A-G-06 : les quatre parcours ci-dessus sont illustratifs, reconstitués depuis le corpus ; aucun parcours n'a été raconté par le métier]

## 3. Glossaire

- **Legacy** : l'application existante, décrite par le corpus `docs/retro/`.
- **Corpus** : le dossier `docs/retro/`, référence du comportement du legacy (règles RG, validations VAL, messages MSG, scénarios SC).
- **Règle de gestion (RG)** : règle identifiée `RG-NNN-NNN` dans `docs/retro/features/FEAT-NNN/RG.md`, de statut Confirmé, À confirmer ou Suspect.
- **Bug à corriger** : comportement défectueux du legacy identifié `BUG-NNN`, que le métier a décidé de corriger. Son comportement attendu remplace le comportement observé.
- **Bug conservé** : bug du legacy décidé « ne pas corriger ». Il n'est ni une exigence ni un test (liste dans `docs/retro/00-global/LEGACY-BUGS.md`).
- **Client** : personne titulaire de comptes, de rôle CLIENT. Un client a un ou plusieurs comptes.
- **Conseiller** : utilisateur de rôle CONSEILLER, affecté à un portefeuille de clients.
- **Administrateur** : utilisateur de rôle ADMIN. Les écrans « client » (tableau de bord, virement…) ne lui montrent que ses propres données, jamais celles des autres clients ; dans les données de référence, il n'a pas de compte personnel.
- **Compte** : compte bancaire interne d'un client, de type COURANT, EPARGNE, CREDIT, etc.
- **Virement** : transfert d'un montant d'un compte source vers un compte cible ou un bénéficiaire externe.
- **Virement programmé** : virement récurrent avec une fréquence et une prochaine date d'exécution, exécuté par un traitement planifié.
- **Bénéficiaire externe** : destinataire hors banque, identifié par un IBAN, enregistré par un client.
- **Contrôle de fraude** : évaluation d'un virement selon des signaux (LARGE_AMOUNT, ROUND_AMOUNT, RAPID_SUCCESSION, HIGH_FREQUENCY, heure inhabituelle…), chacun avec un score de risque.
- **Alerte de fraude** : enregistrement consultable par l'administrateur, produit par le contrôle de fraude en temps réel. Le traitement nocturne de détection de fraude (FEAT-019) ne crée pas d'alerte, il journalise son résultat (RG-019-007).
- **Notification** : message interne affiché au client dans l'application (CONFIRMATION, ALERTE, FRAUDE, solde faible…).
- **Prêt** : demande de crédit d'un client, avec un état (en attente, approuvé, refusé…) et un échéancier.
- **Compte tiers** : compte d'une autre banque, agrégé par le fournisseur d'open banking.
- **Fournisseur externe** : service tiers appelé par l'application : Azure OpenAI (chat conseiller), Powens (open banking), AlphaVantage (bourse).
- **Adaptateur simulé** : implémentation locale d'un fournisseur externe, sans réseau. Elle est active par défaut en local, dans les tests et dans les contrôles.
- **Traitement planifié** : traitement exécuté par le système à heure fixe ou à intervalle régulier.
- **Golden master** : ensemble des 33 scénarios enregistrés sur le legacy (`docs/retro/golden-master/`), dérivés pour la nouvelle application et rejoués une seule fois, développement terminé.
- **Contrôle** : vérification exécutée par un outil (test d'architecture, couverture, mutation, CVE, trace), jamais évaluée par un agent. La liste fait foi dans `docs/quality-gates.md`.

## 4. Fonctionnalités

Les 30 fonctionnalités du périmètre sont présentées dans l'ordre du corpus. Les règles marquées « À confirmer » dans le corpus sont reprises telles quelles (voir A-G-01).

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

Pour renseigner l'adresse IP d'une entrée d'audit, le système retient le premier élément de l'en-tête de transfert « X-Forwarded-For » s'il est présent et non vide, sinon l'adresse distante de la connexion. [ASSUMPTION A-001-2 : l'en-tête n'est pris en compte que lorsqu'il est posé par un mandataire de confiance de l'infrastructure ; sinon l'adresse distante de la connexion est retenue, pour éviter qu'un client falsifie son adresse dans l'audit]

Source : RG-001-016

**Notes :**
- Les messages « Identifiant requis » et « Mot de passe requis » sont invisibles en production dans le legacy (dette constatée dans le corpus de validation) ; FR-001-03 les rend visibles, conformément à RG-001-008 et RG-001-009.
- Le badge technique du pied de formulaire (pile technique du legacy) n'est pas repris. [ASSUMPTION A-001-3 : les bandeaux techniques du legacy, sans valeur fonctionnelle, ne sont pas reproduits et sont exclus de la comparaison du golden master]
- RG-001-017, RG-001-018 et RG-001-019 (Suspect, dette legacy) sont couvertes par les FR de correction FR-001-07, FR-001-08 et FR-001-09 ; leur comportement legacy n'est pas reproduit.
- FR-001-10 et FR-001-11 sont les exigences fusionnées des règles voisines RG-001-010 + RG-030-010 et RG-001-011 + RG-030-003 (porteuse FEAT-001).

### FEAT-002 — Tableau de bord

**Description :** Le tableau de bord est l'écran d'accueil du CLIENT après connexion ; il est aussi accessible aux rôles CONSEILLER et ADMIN, qui le voient le plus souvent sans compte personnel (solde total à zéro, aucune opération). Il affiche le solde total et la liste des comptes de l'utilisateur, les 5 dernières opérations de son compte courant, le centre de notifications (badge de non-lus, « Tout marquer lu », « Effacer tout ») et, si l'utilisateur a agrégé des comptes d'autres banques, son patrimoine total. Les notifications sont produites par le traitement des virements, une seule par virement, et sont conservées après un redémarrage. Les cas limites marquants sont l'utilisateur sans compte, sans compte courant, sans notification, et l'utilisateur supprimé pendant sa session. Scénarios golden master : SC-002-01, SC-002-02.

**Exigences fonctionnelles :**

#### FR-002-01 : Données de l'utilisateur relues à chaque ouverture

À chaque ouverture du tableau de bord, le système relit les données de l'utilisateur connecté à partir de son identifiant de session et met à jour le contexte de la session avec ces données à jour. L'en-tête affiche « Bonjour, {fullName} », où {fullName} est le nom complet de l'utilisateur, et le lien « Deconnexion ».

Source : RG-002-001

#### FR-002-02 : Utilisateur supprimé pendant sa session

Si l'utilisateur dont l'identifiant est en session n'existe plus au moment de l'ouverture du tableau de bord, le système traite cette absence sans exposer d'erreur technique à l'utilisateur. Cette exigence remplace le comportement legacy RG-002-002 (comportement imprévisible). [ASSUMPTION A-002-1 : le système met fin à la session et redirige vers la page de connexion, sans message technique]

Critère d'acceptation (test) : après suppression de l'utilisateur pendant que sa session est ouverte, l'ouverture du tableau de bord n'affiche ni erreur technique ni trace, et mène à la page de connexion sans session authentifiée.

Source : RG-002-002, BUG-004

#### FR-002-03 : Solde total

Sous l'intitulé « Mes comptes Demo Bank », la carte « Solde total Demo Bank » (sous-titre « Tous comptes Demo Bank confondus ») affiche la somme arithmétique des soldes de tous les comptes de l'utilisateur connecté. Si l'utilisateur n'a aucun compte, le solde total affiché est EUR 0.00.

Source : RG-002-003, RG-002-004

#### FR-002-04 : Liste des comptes

Pour chaque compte de l'utilisateur connecté, le tableau de bord affiche le libellé du type de compte (par exemple COURANT, EPARGNE), son solde courant en EUR avec 2 décimales et un séparateur de milliers, et son numéro sous la forme « N {numéro de compte} ». [ASSUMPTION A-002-2 : le rendu exact des montants (préfixe « EUR », séparateurs décimal et de milliers) reprend celui du legacy capturé par le golden master ; le corpus en donne deux rendus différents, « EUR17 330,00 » et « EURx,xxx.xx »]

Source : RG-002-005

#### FR-002-05 : Dernières opérations du compte courant

Lorsque l'utilisateur a au moins un compte, le tableau de bord affiche les 5 opérations les plus récentes de son compte COURANT, de la plus récente à la plus ancienne. Cette exigence remplace le comportement legacy de RG-002-006, qui prenait le premier compte renvoyé, quel que soit son type. Si l'utilisateur n'a aucun compte, le tableau est vide. [ASSUMPTION A-002-3 : un utilisateur qui a des comptes mais aucun compte COURANT voit un tableau vide]

Critère d'acceptation (test) : pour un client qui possède un compte EPARGNE et un compte COURANT, quel que soit leur ordre de création, les opérations affichées appartiennent toutes au compte COURANT, au nombre de 5 au plus, triées par date décroissante.

Source : RG-002-006, RG-002-007, RG-002-008, BUG-005

#### FR-002-06 : Présentation des opérations

La section « Activite recente » / « Dernieres operations » présente les colonnes « Date », « Description », « Type », « Montant » et « Solde apres ». La date de chaque opération est au format dd/MM/yyyy HH:mm, et son type est affiché DEBIT ou CREDIT. Le bouton « Voir le releve complet » est toujours affiché.

Source : RG-002-009, RG-002-010

#### FR-002-07 : Centre de notifications de l'utilisateur

La section « Centre de notifications » affiche uniquement les notifications enregistrées pour l'utilisateur connecté, de la plus récente à la plus ancienne ; une notification non lue est signalée par l'indication « Non lu ». Si l'utilisateur n'a aucune notification, le système affiche « Aucune notification - effectuez un virement pour voir les messages publies via ActiveMQ Artemis en temps reel ».

Source : RG-002-011, RG-002-012

#### FR-002-08 : Badge et actions sur les notifications

Lorsque l'utilisateur a au moins une notification non lue, le système affiche un badge « {N} nouvelle(s) », où N est le nombre de notifications non lues, et le bouton « Tout marquer lu ». Lorsqu'il a au moins une notification, lue ou non, le système affiche le bouton « Effacer tout ». « Tout marquer lu » marque comme lues toutes les notifications de l'utilisateur connecté ; « Effacer tout » supprime toutes ses notifications.

Source : RG-002-013, RG-002-014, RG-002-015, RG-002-016, RG-002-017

#### FR-002-09 : Conservation des notifications

Une nouvelle notification est placée en tête de la liste de son destinataire, pour apparaître en premier. Le nombre de notifications conservées par utilisateur est limité à une valeur de configuration, 20 par défaut : quand la limite est atteinte, l'ajout d'une nouvelle notification supprime la plus ancienne. Les notifications sont conservées après un redémarrage du serveur. La partie « conservées après un redémarrage » remplace le comportement legacy RG-002-020 (notifications en mémoire, perdues au redémarrage).

Critère d'acceptation (test) : un utilisateur qui a des notifications, lues et non lues, retrouve après un redémarrage du serveur les mêmes notifications, dans le même ordre et avec le même état de lecture.

Source : RG-002-018, RG-002-019, RG-002-020, BUG-006

#### FR-002-10 : Une seule notification par virement traité [EN ATTENTE RG-027-003 (a)]

Acquis : lorsqu'un virement est traité, le système produit exactement une notification pour l'émetteur, de type CONFIRMATION, ALERTE ou FRAUDE, jamais une combinaison de ces types. Le legacy choisit le type selon le montant, avec un seuil d'alerte (5 000 EUR par défaut) et un seuil de fraude (10 000 EUR par défaut) de configuration :
- montant strictement inférieur au seuil d'alerte : CONFIRMATION, « Virement de {montant} EUR vers le compte {numCompte} confirme », ou « Virement de {montant} EUR vers le compte {numCompte} confirme (motif : {motif}) » si un motif est saisi ;
- montant supérieur ou égal au seuil d'alerte et strictement inférieur au seuil de fraude : ALERTE, « Virement de {montant} EUR en attente de validation - transaction importante, verification en cours. » ;
- montant supérieur ou égal au seuil de fraude : FRAUDE, « Virement de {montant} EUR bloque pour verification (seuil anti-fraude : {seuilFraudeEur} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation. ».

Le fait qu'une notification ALERTE ou FRAUDE remplace la CONFIRMATION et annonce un virement en attente ou bloqué dépend du sort d'un virement suspect, qui reste ouvert : voir la question QO-1 du PRD. Cette exigence remplace le comportement legacy RG-002-024 (plusieurs notifications possibles pour un même virement).

Critère d'acceptation (test) : pour des virements de montant inférieur au seuil d'alerte, compris entre les deux seuils et supérieur au seuil de fraude, chaque virement traité ajoute exactement une notification à l'émetteur.

Source : RG-002-021, RG-002-022, RG-002-023, RG-002-024, BUG-007

#### FR-002-11 : Navigation du tableau de bord selon le rôle

La barre de navigation du tableau de bord affiche à tous les utilisateurs les liens « Tableau de bord », « Virement », « Virements programmes », « Releve », « Budget », « Prets », « Documents », « Bourse », « Mon Conseiller », « Produits », « Comptes Tiers » et « Beneficiaires ». Elle affiche le lien « Espace Conseiller » seulement aux rôles CONSEILLER et ADMIN, et le lien « Administration » seulement au rôle ADMIN. Voir aussi FR-030-04 (même règle pour tous les écrans).

Source : RG-002-025, RG-002-026

#### FR-002-12 : Patrimoine total avec les comptes d'autres banques

Lorsque l'utilisateur a une connexion active d'agrégation de comptes d'autres banques, le tableau de bord affiche la section « Mes comptes externes (Open Banking) » avec la carte « Patrimoine total » (sous-titre « Demo Bank + comptes externes »), dont la valeur est le solde total Demo Bank (FR-002-03) plus le solde total de tous les comptes externes agrégés. Il affiche aussi « Comptes externes via Powens Open Banking ({nombre} comptes) » et le bouton « Voir le detail et les transactions ». Sans connexion active, il affiche la section « Open Banking », le texte « Agregez vos comptes d autres banques pour une vue complete de votre patrimoine » et le bouton « Connecter mes autres banques via Powens ». L'appel au fournisseur d'open banking respecte NFR-INT.

Source : RG-002-027, RG-002-028

**Notes :**
- RG-002-002, RG-002-007, RG-002-020 et RG-002-024 (Suspect, dette legacy) sont couvertes par les FR de correction FR-002-02, FR-002-05, FR-002-09 et FR-002-10 ; leur comportement legacy n'est pas reproduit.
- Libellés : le corpus donne les textes sans accents de l'écran legacy (« Dernieres operations », « publies », « Releve »…), que le golden master capture tels quels ; RG-002-012 cite le message vide avec accents (« publiés », « réel »). Les FR reprennent le texte affiché par le legacy.
- Le texte du message vide de FR-002-07 cite le nom d'un produit technique ; il est repris à l'identique parce que le golden master compare les textes affichés. Les autres bandeaux et étiquettes techniques du legacy (bandeau de pile technique en en-tête, séparateur et étiquette techniques du centre de notifications, suffixe technique de l'horodatage des notifications) ne sont pas repris. [ASSUMPTION A-002-4 : ces bandeaux techniques, sans valeur fonctionnelle, sont exclus du golden master et non reproduits]
- Le texte explicatif affiché par le legacy sur le tableau de bord annonce des combinaisons (CONFIRMATION + ALERTE entre 5 000 et 10 000 EUR, CONFIRMATION + FRAUDE au-delà), en contradiction avec RG-002-021 à RG-002-023 ; il n'est pas repris (BUG-007, FR-002-10).
- Le motif dans la notification de confirmation est aussi porté par l'exigence fusionnée de FEAT-003 (RG-003-020 + RG-023-007).

### FEAT-003 — Effectuer un virement

**Description :** Le formulaire de virement permet à un utilisateur authentifié de virer un montant d'un de ses comptes vers un autre de ses comptes. L'écran est ouvert à tous les rôles, mais seuls les comptes de l'utilisateur connecté sont proposés : un CONSEILLER ou un ADMIN, sans compte propre, voit des listes vides et ne peut rien virer. Le virement est exécuté sans étape de confirmation : débit et crédit simultanés, deux opérations au relevé, statut SUCCESS ou PENDING selon le seuil de mise en attente, journal d'audit, notification dans l'application, puis retour au tableau de bord. Chaque virement passe par le contrôle de fraude en temps réel (FEAT-027), dont le sort d'un virement suspect ou d'un contrôle en erreur reste ouvert (QO-1). Cas limites marquants : comptes non sélectionnés ou identiques, montant nul ou négatif, solde insuffisant (désormais un message clair), virement de 5 000 € et plus (PENDING), de 10 000 € et plus (notification FRAUDE). Scénarios golden master : SC-003-01, SC-003-02, SC-003-03, SC-003-04.

**Exigences fonctionnelles :**

#### FR-003-01 : Accès au formulaire et comptes proposés

Tout utilisateur authentifié, quel que soit son rôle (CLIENT, CONSEILLER, ADMIN), accède à l'écran de virement. Les listes « Compte source » et « Compte destinataire » ne proposent que les comptes de l'utilisateur connecté, précédés de l'option « -- Choisir un compte -- » ; pour un utilisateur sans compte (CONSEILLER, ADMIN), les deux listes ne contiennent que cette option. L'écran a pour titre « Effectuer un virement » et présente les champs « Compte source », « Compte destinataire », « Montant (€) » (indice « Ex : 100,00 — Saisir plus de 5 000 € pour tester l'alerte fraude »), « Motif » (facultatif, 100 caractères au plus, limite appliquée aussi côté serveur), le bouton « Exécuter le virement » et le lien « Virement SEPA externe — Gérer mes bénéficiaires ».

Source : RG-003-001, RG-003-002

#### FR-003-02 : Les deux comptes sont obligatoires

Si le compte source ou le compte destinataire n'est pas sélectionné, le système rejette le virement et affiche « Veuillez sélectionner les deux comptes ».

Source : RG-003-003, RG-003-004

#### FR-003-03 : Comptes source et destinataire différents

Si le compte source et le compte destinataire sont le même compte, le système rejette le virement et affiche « Le compte source et destination doivent être différents », en conservant les valeurs saisies dans le formulaire.

Source : RG-003-005

#### FR-003-04 : Montant strictement positif

Si le montant est absent, non numérique, nul ou négatif, le système rejette le virement, contrôle compris côté serveur. Le message affiché ne contient aucun détail technique (NFR-SEC-04). [ASSUMPTION A-003-1 : pour un montant nul ou négatif, le message est « Erreur lors du virement : Le montant doit etre positif », soit le message legacy sans le nom d'erreur interne qu'il expose]

Source : RG-003-006

#### FR-003-05 : Compte introuvable

Si le compte source ou le compte destinataire référencé n'existe pas, le système rejette le virement, sans débit ni crédit, avec un message sans détail technique (NFR-SEC-04). [ASSUMPTION A-003-1 (suite) : le message est « Erreur lors du virement : Compte introuvable »]

Source : RG-003-008

#### FR-003-06 : Solde insuffisant : refus avec un message clair

Si le solde du compte source est inférieur au montant demandé, le système rejette le virement, ne modifie aucun solde et affiche un message clair et non technique indiquant que le solde est insuffisant. [ASSUMPTION A-003-2 : le texte est « Solde insuffisant sur le compte source », message prévu par le legacy mais jamais affiché] Cette exigence remplace le comportement legacy RG-003-007, qui affichait « Erreur lors du virement : » suivi du nom d'une erreur interne et de « Solde insuffisant ».

Critère d'acceptation (test) : un client dont le compte COURANT a un solde de 2 130,00 € demande un virement de 9 999,00 € vers son compte EPARGNE ; la page affiche « Solde insuffisant sur le compte source », n'affiche aucun nom d'erreur interne ni texte technique, et les soldes des deux comptes sont inchangés.

Source : RG-003-007, BUG-008, BUG-011

#### FR-003-07 : Ordre des contrôles

Les contrôles de saisie et de solde (FR-003-02 à FR-003-06) s'appliquent avant le contrôle de fraude (FR-003-15) ; un virement rejeté par l'un d'eux n'est ni exécuté ni soumis au contrôle de fraude. [ASSUMPTION A-003-3 : l'ordre entre contrôles de saisie et contrôle de fraude n'est pas écrit dans le corpus ; le legacy ne contrôlait la fraude que pour un virement exécuté]

Source : RG-003-003, RG-003-004, RG-003-005, RG-003-006, RG-003-008

#### FR-003-08 : Exécution sans étape de confirmation

Le système exécute le virement dès la soumission du formulaire, sans demander de confirmation à l'utilisateur.

Source : RG-003-009

#### FR-003-09 : Débit, crédit et opérations au relevé [EN ATTENTE RG-027-003 (a)(b)]

Acquis : un virement exécuté débite le compte source et crédite le compte destinataire du montant, de façon atomique (les deux ou aucun). Il enregistre une opération DEBIT sur le compte source (« Virement vers {compte destinataire} - {motif} ») et une opération CREDIT sur le compte destinataire (« Virement depuis {compte source} - {motif} »), chacune avec le solde après opération, ainsi que le virement lui-même avec son horodatage ; un virement au statut PENDING porte en plus la mention « [EN ATTENTE] » dans ces descriptions. Le débit et le crédit n'interviennent qu'après le contrôle de fraude (FR-003-15). Ouvert : l'application ou non du débit et du crédit quand le contrôle détecte une suspicion (a) ou tombe en erreur (b) dépend de la question QO-1 du PRD.

Source : RG-003-010, RG-003-011, RG-003-012, RG-003-013

#### FR-003-10 : Statut du virement selon le seuil de mise en attente [EN ATTENTE RG-027-003 (a)(b)]

Acquis : un virement d'un montant supérieur ou égal au seuil de mise en attente (configuration de déploiement, 5 000 € par défaut) prend le statut PENDING ; un virement d'un montant strictement inférieur prend le statut SUCCESS. Le statut affiché ne doit plus contredire l'état réel du solde : le legacy débitait immédiatement un virement PENDING, sans aucune sortie possible de cet état. Ouvert : le sens exact de PENDING (débit différé ou non), la façon d'en sortir et le statut d'un virement jugé suspect ou dont le contrôle est en erreur dépendent de la question QO-1 du PRD. Cette exigence remplace le comportement legacy RG-003-014.

Critère d'acceptation (test) : un virement de 5 000,00 € prend le statut PENDING et un virement de 4 999,99 € le statut SUCCESS ; pour le virement PENDING, l'état du solde source est celui que fixe la réponse à QO-1, et aucune donnée affichée n'annonce une attente alors que le débit est définitif, ni l'inverse.

Source : RG-003-014, RG-003-015, BUG-009

#### FR-003-11 : Journal d'audit du virement

Pour chaque virement exécuté, le système enregistre une entrée d'audit avec l'identifiant de l'utilisateur, le montant et le motif.

Source : RG-003-016

#### FR-003-12 : Retour au tableau de bord

Après l'exécution d'un virement, le système affiche le tableau de bord, où les soldes sont à jour.

Source : RG-003-017

#### FR-003-13 : Remise au traitement asynchrone [EN ATTENTE RG-027-003 (a)(b)]

Acquis : chaque virement enregistré est remis au traitement asynchrone des virements (FR-023-01) ; l'échec de cette remise n'est plus ignoré (FR-023-02, qui remplace la publication « au mieux » du legacy). Ouvert : la remise d'un virement jugé suspect ou dont le contrôle est en erreur dépend de la question QO-1 du PRD.

Source : RG-003-018

#### FR-003-14 : Notification selon le montant [EN ATTENTE RG-027-003 (a)]

Acquis : l'utilisateur connecté reçoit dans l'application une seule notification par virement (FR-002-10), choisie selon le montant, avec le seuil d'alerte (5 000 € par défaut) et le seuil de fraude (10 000 € par défaut) de la configuration de déploiement :
- montant strictement inférieur à 5 000 € : CONFIRMATION, « Virement de {montant} EUR vers le compte {n°_compte_destinataire} confirme » ;
- montant supérieur ou égal à 5 000 € et strictement inférieur à 10 000 € : ALERTE, « Virement de {montant} EUR en attente de validation - transaction importante, verification en cours. » ;
- montant supérieur ou égal à 10 000 € : FRAUDE, « Virement de {montant} EUR bloque pour verification (seuil anti-fraude : {seuil} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation. ».

Ouvert : le remplacement de la CONFIRMATION par une notification FRAUDE quand le contrôle détecte une suspicion, et la véracité des mentions « en attente » et « bloque » dépendent de la question QO-1 du PRD (voir aussi FR-023-04).

Source : RG-003-019, RG-003-021, RG-003-022

#### FR-003-15 : Contrôle de fraude avant la validation finale [EN ATTENTE RG-027-003 (a)(b)]

Acquis : le contrôle de fraude en temps réel (FR-027-01) s'exécute pour chaque virement avant sa validation finale, et toute erreur de ce contrôle est journalisée. Cette exigence remplace l'ordre legacy de RG-003-023 (contrôle après la validation, erreurs ignorées). Ouvert : le sort du virement quand le contrôle détecte une suspicion (a) ou tombe en erreur (b) dépend de la question QO-1 du PRD (FR-027-02).

Source : RG-003-023

#### FR-003-16 : Avertissement du nombre d'alertes [EN ATTENTE RG-027-003 (a)]

Acquis : si le contrôle de fraude détecte au moins une alerte pour le virement, le système affiche à l'utilisateur « Attention : {N} alerte(s) fraude détectée(s) sur ce virement. Votre conseiller a été notifié. », où N est le nombre d'alertes. Ouvert : le legacy affiche ce message sans bloquer le virement ; que le virement soit exécuté, bloqué ou suspendu à ce moment dépend de la question QO-1 du PRD. Voir FR-027-04.

Source : RG-003-024

#### FR-003-17 : Signal LARGE_AMOUNT (règles voisines)

Le contrôle de fraude lève un signal LARGE_AMOUNT de score 40 pour un montant supérieur ou égal à 5 000 € et de score 80 pour un montant supérieur ou égal à 15 000 €. Écart dans le corpus : RG-003-025 lève le score 40 dès 5 000 € sans borne haute, alors que RG-027-004 le borne à moins de 15 000 € ; un montant de 15 000 € ou plus pourrait donc lever 40 et 80. [ASSUMPTION A-003-4 : un seul signal LARGE_AMOUNT par virement, de score 40 entre 5 000 € inclus et 15 000 € exclus, de score 80 à partir de 15 000 €, conformément à RG-027-004 et aux deux descriptions distinctes du legacy] Description de l'alerte : « Transaction amount {amount} exceeds the large threshold of {threshold} » (score 40) ou « Transaction amount {amount} exceeds the very-large threshold of {threshold} » (score 80).

Source : RG-003-025, RG-003-026, RG-027-005

#### FR-003-18 : Signal HIGH_FREQUENCY sur les transactions du jour

Le contrôle de fraude lève un signal HIGH_FREQUENCY de score 50 si le nombre de transactions du client exécutées le jour courant dépasse 8. Les transactions des jours précédents ne sont pas comptées. Cette exigence remplace le comportement legacy RG-003-027, qui comptait toutes les transactions depuis l'ouverture du compte. Même règle que FR-027-06.

Critère d'acceptation (test) : un client qui a 100 transactions anciennes et 8 transactions le jour courant ne déclenche pas HIGH_FREQUENCY ; avec 9 transactions le jour courant, le signal est levé avec le score 50.

Source : RG-003-027, BUG-010

#### FR-003-19 : Signal NEW_BENEFICIARY

Le contrôle de fraude lève un signal NEW_BENEFICIARY de score 35 si le compte destinataire n'est pas enregistré comme bénéficiaire actif du client. Même règle que FR-027-07.

Source : RG-003-028

#### FR-003-20 : Signal UNUSUAL_HOURS

Le contrôle de fraude lève un signal UNUSUAL_HOURS de score 45 si le virement est initié avant 06:00 ou après 23:00, heure du serveur. Même règle que FR-027-08.

Source : RG-003-029

#### FR-003-21 : Signal ROUND_AMOUNT (règles voisines)

Le contrôle de fraude lève un signal ROUND_AMOUNT de score 20 si le montant est supérieur ou égal à 1 000 € et multiple exact de 1 000. Description de l'alerte : « Transaction amount {amount} is a suspicious round figure (multiple of 1000) ». Les deux règles voisines sont identiques.

Source : RG-003-030, RG-027-009

#### FR-003-22 : Signal FOREIGN_IBAN

Le contrôle de fraude lève un signal FOREIGN_IBAN de score 30 si le numéro du compte destinataire ne commence pas par « FR ». FR-027-09 précise que la comparaison est insensible à la casse.

Source : RG-003-031

#### FR-003-23 : Signal RAPID_SUCCESSION (règles voisines)

Le contrôle de fraude lève un signal RAPID_SUCCESSION de score 55 si la précédente opération du même client a eu lieu moins de 30 secondes avant le virement courant. Écart dans le corpus : RG-003-032 parle de la précédente « transaction » du client, RG-027-011 du précédent « virement ». [ASSUMPTION A-003-5 : la référence est le précédent virement du client soumis au contrôle de fraude] Description de l'alerte : « Previous transaction was only {elapsedSeconds} seconds ago (threshold: {rapidSuccessionSeconds}s) ».

Source : RG-003-032, RG-027-011

#### FR-003-24 : Signal VELOCITY_CHECK

Le contrôle de fraude lève un signal VELOCITY_CHECK de score 65 si le client a effectué 5 transactions ou plus de plus de 500 € dans les 60 dernières minutes. Voir FR-027-10, qui parle de « virements ».

Source : RG-003-033

#### FR-003-25 : Enregistrement des alertes au-delà du score 75

Si la somme des scores de tous les signaux levés pour le virement dépasse strictement 75, le système enregistre toutes les alertes correspondantes. Même règle que FR-027-03 (une alerte par signal, au statut OPEN).

Source : RG-003-034

#### FR-003-26 : Motif dans la notification de confirmation (règles voisines)

Lorsque la notification CONFIRMATION est produite et que le motif est renseigné, le système l'ajoute au message : « Virement de {montant} EUR vers le compte {n°_compte_destinataire} confirme (motif : {motif}) ». Écart dans le corpus : RG-003-020 exige un motif « non vide », RG-023-007 un motif « non nul et non vide » ; le texte du legacy écarte aussi un motif composé uniquement d'espaces (MESSAGES de FEAT-025). Un motif absent, vide ou composé uniquement d'espaces donne le message sans motif de FR-003-14.

Source : RG-003-020, RG-023-007

**Notes :**
- RG-003-007, RG-003-014 et RG-003-027 (Suspect, dette legacy) sont couvertes par les FR de correction FR-003-06 (BUG-008, BUG-011), FR-003-10 (BUG-009) et FR-003-18 (BUG-010) ; leur comportement legacy n'est pas reproduit.
- SC-003-03 capture le message technique legacy de solde insuffisant ; corrigé par FR-003-06 (BUG-011), son texte attendu change.
- Le motif dans la notification de confirmation est porté par l'exigence fusionnée FR-003-26 (RG-003-020 + RG-023-007).
- Les deux bandeaux techniques du legacy au-dessus du formulaire (pile technique de gestion du virement, publication sur une file nommée) et les messages de champ obligatoire du legacy, qui exposent des identifiants de composants internes, ne sont pas repris (NFR-SEC-04). [ASSUMPTION A-003-6 : ces textes sont exclus du golden master ; un champ obligatoire vide reçoit un message sans détail technique]
- Le corpus signale pour RAPID_SUCCESSION un risque d'accès concurrent dans le legacy (JIRA-8910), sans décision de correction ; FR-003-23 n'en fait pas une exigence.

### FEAT-004 — Virements programmés

**Description :** L'écran « Virements programmes » permet à tout utilisateur authentifié (en pratique un CLIENT titulaire de comptes ; un CONSEILLER ou un ADMIN sans compte voit un formulaire sans compte source et une liste vide) de créer des virements récurrents depuis l'un de ses comptes, avec une fréquence parmi six (hebdomadaire à annuelle), puis de les suspendre, les réactiver ou les supprimer. Un traitement automatique exécute toutes les 5 minutes les virements actifs arrivés à échéance (détaillé dans FEAT-021). Les cas limites marquants sont la date de première exécution saisie au format `yyyy-MM-dd` (différent des autres écrans, conservé), la date passée acceptée, le solde insuffisant à l'échéance (désormais notifié au client), le compte bénéficiaire qui n'est pas un compte de la banque (désormais refusé avant tout débit) et les actions sur le virement d'un autre client (désormais refusées). Scénarios golden master : SC-004-01, SC-004-02, SC-004-03.

**Exigences fonctionnelles :**

#### FR-004-01 : Accès à l'écran des virements programmés

Lorsqu'un utilisateur sans session authentifiée demande l'écran des virements programmés, le système le redirige vers la page de connexion.

Source : RG-004-001

#### FR-004-02 : Formulaire de création et comptes source proposés

L'écran affiche le titre d'onglet « BankApp Demo - Virements programmes », la section « Nouveau virement programme » avec les libellés « Compte debiteur », « Montant (EUR) », « Frequence », « IBAN / N compte beneficiaire », « Libelle beneficiaire », « Date de premiere execution », « Motif (optionnel) », les exemples de saisie « ex: 200.00 », « ex: FR76-0001-0002 », « ex: Compte epargne », « yyyy-MM-dd », « ex: Epargne mensuelle », et le bouton « Creer le virement programme ». Le sélecteur « Compte debiteur » commence par l'option « -- Choisir -- » et ne propose que les comptes appartenant au client connecté, chacun libellé « {numéro de compte} ({type de compte}) - {solde} EUR ».

Source : RG-004-002

#### FR-004-03 : Liste des virements programmés du client

La section « Mes virements programmes » affiche uniquement les virements programmés du client connecté, triés par date de prochaine exécution croissante, dans un tableau aux colonnes « Source », « Beneficiaire », « Montant », « Frequence », « Prochain », « Nb executions », « Etat », « Actions ». La date de prochaine exécution est affichée au format `dd/MM/yyyy`. La colonne « Etat » affiche « Actif » ou « Inactif » ; un virement actif propose les boutons « Suspendre » et « Supprimer », un virement inactif « Reactiver » et « Supprimer ». Si le client n'a aucun virement programmé, l'écran affiche « Aucun virement programme configure ».

Source : RG-004-003, RG-004-050, RG-004-051, RG-004-052

#### FR-004-04 : Champs obligatoires et format de date du formulaire

À la soumission, le système rejette la création et affiche :
- « Veuillez selectionner un compte source » si aucun compte débiteur n'est sélectionné ;
- « Veuillez saisir le numero de compte destinataire » si le numéro de compte bénéficiaire est vide ou blanc ;
- « Veuillez saisir le libelle du beneficiaire » si le libellé du bénéficiaire est vide ou blanc ;
- « Veuillez saisir la date de premiere execution » si la date de première exécution est vide ou blanche ;
- « Format de date invalide (attendu : yyyy-MM-dd) » si la date de première exécution ne respecte pas le format `yyyy-MM-dd` (format conservé, différent du format `dd/MM/yyyy` des autres écrans).

Le formulaire reste affiché avec les valeurs saisies.

Source : RG-004-010, RG-004-011, RG-004-012, RG-004-014, RG-004-015

#### FR-004-05 : Montant invalide (virement programmé, simulation et demande de prêt)

Exigence fusionnée (règles voisines). Le système rejette la saisie et affiche exactement « Montant invalide » lorsque le montant est absent ou inférieur ou égal à zéro, dans trois contextes :
- création d'un virement programmé (champ « Montant (EUR) ») : la création est rejetée (RG-004-013) ;
- simulation de prêt (montant de simulation) : la simulation est rejetée (RG-007-003) ;
- demande de prêt (montant de la demande, champ également obligatoire) : la demande est rejetée (RG-007-012).

Le seuil (strictement positif) et le message sont identiques dans les trois contextes ; seul l'effet diffère (création, simulation ou demande rejetée).

Source : RG-004-013, RG-007-003, RG-007-012

#### FR-004-06 : Contrôles à l'enregistrement du virement programmé

À l'enregistrement, indépendamment des contrôles du formulaire, le système rejette la création :
- si le client ou le compte source est introuvable, avec le message « Erreur : Client ou compte introuvable » ;
- si le compte source n'appartient pas au client connecté, avec le message « Erreur : Compte non autorise » ;
- si le montant est absent ou inférieur ou égal à zéro, avec le message « Erreur : Montant invalide ».

Aucun virement programmé n'est créé dans ces cas.

Source : RG-004-020, RG-004-021, RG-004-022

#### FR-004-07 : Création réussie

Lorsqu'un virement programmé est créé, le système l'enregistre à l'état actif, avec un nombre d'exécutions à 0 et l'horodatage de création, affiche « Virement programme cree avec succes », vide tous les champs du formulaire et recharge la liste des virements programmés. Une date de première exécution passée est acceptée : le virement est alors exécuté au prochain passage du traitement automatique, dans les 5 minutes (FR-004-11).

Source : RG-004-023, RG-004-024, RG-004-066

#### FR-004-08 : Fréquences proposées

Le système propose six fréquences, dans cet ordre et avec ces libellés : « Hebdomadaire » (7 jours calendaires), « Mensuel » (1 mois calendaire, valeur sélectionnée par défaut), « Bimestriel » (2 mois calendaires), « Trimestriel » (3 mois calendaires), « Semestriel » (6 mois calendaires), « Annuel » (1 an calendaire). Le calcul des échéances suivantes est décrit en FR-021-08.

Source : RG-004-030

#### FR-004-09 : Suspension, réactivation et suppression

Lorsque le client clique sur « Suspendre » pour un virement actif, le système le passe à l'état inactif ; sur « Reactiver » pour un virement inactif, il le repasse à l'état actif ; dans les deux cas, la liste est rechargée. Lorsque le client clique sur « Supprimer », le système supprime définitivement le virement programmé. Ces actions ne s'appliquent qu'aux virements du client connecté (FR-004-10).

Source : RG-004-040, RG-004-041, RG-004-042

#### FR-004-10 : Contrôle d'appartenance avant suspension, réactivation ou suppression

Avant toute suspension, réactivation ou suppression, le système vérifie que le virement programmé appartient au client connecté. Sinon, la demande est rejetée, le virement reste inchangé, et un message d'erreur générique, qui ne révèle pas l'existence du virement, est affiché [ASSUMPTION A-004-1 : le corpus ne fixe pas le message de refus ; un message générique est retenu, conformément à NFR-SEC-04]. Remplace le comportement legacy RG-004-043 (aucune vérification d'appartenance).

Critère d'acceptation (test) : connecté en tant que client A, une demande de suspension, de réactivation ou de suppression portant sur l'identifiant d'un virement programmé du client B est refusée, et le virement de B garde son état, sa date de prochaine exécution et son existence.

Source : RG-004-043, BUG-013

#### FR-004-11 : Exécution automatique des virements dus

Toutes les 5 minutes, le système recherche les virements programmés actifs dont la date de prochaine exécution est passée ou atteinte, et tente de les exécuter l'un après l'autre (détail en FR-021-01 à FR-021-03). Lorsque le compte bénéficiaire est un compte de la banque et que le solde est suffisant, le système débite le compte source, crédite le compte bénéficiaire et enregistre une opération DEBIT et une opération CREDIT (libellés en FR-021-05 et FR-021-06), sauf si le solde est insuffisant (FR-004-12) ou si le compte bénéficiaire n'est pas un compte de la banque (FR-004-13).

Source : RG-004-060, RG-004-062

#### FR-004-12 : Solde insuffisant à l'échéance

Exigence fusionnée (règles voisines). Si, au moment de l'exécution, le solde du compte source est strictement inférieur au montant du virement programmé, le système n'exécute pas le virement : aucun débit, aucun crédit, aucune opération enregistrée, ni l'état ni le nombre d'exécutions ni la date de prochaine exécution ne changent (RG-021-004 ; RG-004-061 dit « sans changer l'état du virement »), de sorte que le virement reste dû et est retenté au passage suivant, 5 minutes plus tard. Contrairement au legacy (RG-004-061 et RG-021-004 : aucune notification), le système informe le client, par une notification dans l'application, que le virement programmé n'a pas pu être exécuté faute de solde suffisant ; il journalise aussi « Solde insuffisant pour virement programme id={id} ». [ASSUMPTION A-004-2 : une seule notification par échéance non exécutée, et non une à chaque nouvelle tentative toutes les 5 minutes.] [ASSUMPTION A-004-3 : la notification est de type ALERTE, avec un texte à définir du type « Virement programme vers {libellé bénéficiaire} non execute : solde insuffisant » ; le corpus ne fixe ni le type ni le texte.] Remplace le comportement legacy RG-004-061 (exécution sautée sans information du client).

Critère d'acceptation (test) : avec un virement programmé actif et dû de 500 € sur un compte source dont le solde est de 499,99 €, après un passage du traitement automatique, le solde, les opérations, l'état et la date de prochaine exécution sont inchangés, et le client a exactement une notification indiquant que le virement n'a pas pu être exécuté faute de solde suffisant ; un second passage n'ajoute pas de notification pour la même échéance.

Source : RG-004-061, RG-021-004, BUG-014

#### FR-004-13 : Bénéficiaire obligatoirement interne à la banque

Les virements programmés ne sont possibles que vers un compte de la banque. À la création, si le numéro de compte bénéficiaire ne correspond à aucun compte de la banque, le système rejette la création et affiche un message d'erreur clair [ASSUMPTION A-004-4 : le contrôle est fait dès la création, en plus de l'exécution ; texte proposé « Compte destinataire introuvable », non fixé par le corpus]. À l'exécution, le même cas est rejeté avant tout débit (FR-021-04). Remplace le comportement legacy RG-004-063 (compte source débité sans crédit nulle part).

Critère d'acceptation (test) : la création d'un virement programmé vers un numéro de compte qui n'existe pas dans la banque est refusée avec un message d'erreur, aucun virement programmé n'est créé ; et un virement programmé dû dont le compte bénéficiaire n'existe pas ne provoque ni débit ni opération sur le compte source.

Source : RG-004-063, BUG-015

#### FR-004-14 : Mise à jour du virement après exécution

Après une exécution réussie, le système enregistre l'horodatage de cette exécution, augmente de 1 le nombre d'exécutions et calcule la date de prochaine exécution à partir de la date d'échéance prévue et de la fréquence, comme en FR-021-08, et non à partir de l'heure réelle d'exécution comme le décrit RG-004-064. [ASSUMPTION A-004-5 : la correction du BUG-069 (FEAT-021) s'applique aussi à RG-004-064, qui décrit le même calcul.]

Source : RG-004-064

#### FR-004-15 : Échec isolé d'un virement pendant un passage

Si l'exécution d'un virement programmé échoue, le système journalise un avertissement et poursuit l'exécution des autres virements dus du même passage, sans que l'échec affecte ces derniers (FR-021-09).

Source : RG-004-065

**Notes :**
- RG-004-016 (dette legacy, Suspect) : non reproduite comme exigence distincte. Le format `yyyy-MM-dd` reste néanmoins le seul accepté pour la date de première exécution, par FR-004-04 (RG-004-015). Bugs conservés liés à ce format : BUG-012 (format différent des autres formulaires) et BUG-021 (format jugé incohérent).
- RG-004-070 (dette legacy, Suspect) : non reproduite — motif de plus de 255 caractères ; bugs conservés BUG-016 et BUG-022.
- RG-004-071 (dette legacy) : non reproduite — numéro de compte bénéficiaire de plus de 34 caractères ; bugs conservés BUG-017 et BUG-019.
- RG-004-072 (dette legacy) : non reproduite — libellé bénéficiaire de plus de 100 caractères ; bugs conservés BUG-018 et BUG-020.
- Les erreurs techniques inattendues étaient affichées en legacy sous la forme « Erreur : {message} » (création, suspension, réactivation) et « Erreur suppression : {message} » (suppression), avec le message brut de l'erreur ; ce détail technique n'est plus exposé (NFR-SEC-04).
- RG-021-004 est portée ici par FR-004-12 (porteuse FEAT-004).
- Ordre des contrôles à l'exécution : voir FR-021-04 (compte bénéficiaire interne vérifié avant le solde).

### FEAT-005 — Relevé de compte

**Description :** L'écran « Relevé de compte » permet à tout utilisateur authentifié (CLIENT, et CONSEILLER ou ADMIN pour leurs propres comptes) de consulter, compte par compte, toutes les opérations d'un de ses comptes, de la plus récente à la plus ancienne. Le premier compte est sélectionné d'office ; changer de compte rafraîchit le solde et les opérations sans recharger toute la page. Les opérations en attente (libellé préfixé « [EN ATTENTE] ») sont signalées par un badge et une ligne surlignée. Les cas limites marquants sont l'utilisateur sans compte (sélecteur vide, aucune opération), le compte sans opération et l'absence de limite au nombre d'opérations affichées. Scénarios golden master : SC-005-01.

**Exigences fonctionnelles :**

#### FR-005-01 : Accès à l'écran du relevé

Lorsqu'un utilisateur sans session active demande l'écran du relevé de compte, le système le redirige vers la page de connexion.

Source : RG-005-001

#### FR-005-02 : Comptes proposés et sélection initiale

À l'ouverture, l'écran affiche le titre d'onglet « BankApp Demo - Relevé de compte », la section « Relevé de compte » et le sélecteur libellé « Compte : », qui ne propose que les comptes du client connecté. Si le client a au moins un compte, le premier compte de la liste est sélectionné d'office et ses opérations sont chargées. Si le client n'a aucun compte, le sélecteur est affiché sans option et aucune opération n'est affichée.

Source : RG-005-002, RG-005-003, RG-005-004

#### FR-005-03 : Changement de compte

Lorsque l'utilisateur change de compte dans le sélecteur, le système recharge la liste de ses comptes (soldes à jour), puis charge les opérations du compte choisi, et rafraîchit le panneau d'informations du compte et le tableau des opérations sans recharger toute la page.

Source : RG-005-005, RG-005-006

#### FR-005-04 : Informations du compte sélectionné

Lorsqu'un compte est sélectionné, le système affiche « {type de compte} — N° {numéro de compte} — Solde actuel : {solde} », le solde étant formaté en euros (symbole « € ») avec au moins 2 décimales.

Source : RG-005-007, RG-005-008

#### FR-005-05 : Chargement de toutes les opérations

Le système charge toutes les opérations du compte sélectionné, sans limite de nombre, triées par date d'opération décroissante (la plus récente en premier).

Source : RG-005-009, RG-005-010

#### FR-005-06 : Tableau des opérations

Si le compte n'a aucune opération, le système affiche « Aucune transaction pour ce compte. ». Sinon, il affiche au-dessus du tableau le compteur « {N} opération(s) », où N est le nombre total d'opérations chargées, puis un tableau aux colonnes « Date », « Description », « Type », « Montant », « Solde après ». Pour chaque opération :
- la date est au format `dd/MM/yyyy HH:mm` ;
- le type est affiché « CREDIT » en vert (#2e7d32) ou « DEBIT » en rouge (#E3000F) ;
- le montant est précédé de « + » pour un CREDIT et d'un signe moins pour un DEBIT [ASSUMPTION A-005-1 : le signe moins est le tiret ASCII « - » relevé dans l'inventaire des messages, et non le caractère « − » cité par RG-005-014], et formaté en euros avec au moins 2 décimales ;
- le solde après opération est formaté en euros avec au moins 2 décimales, sans signe.

Source : RG-005-011, RG-005-012, RG-005-013, RG-005-014, RG-005-015, RG-005-016

#### FR-005-07 : Opérations en attente

Si le libellé d'une opération commence par « [EN ATTENTE] » suivi d'une espace (13 caractères), le système affiche le badge « En attente » avant le libellé, retire ces 13 caractères du libellé affiché et affiche la ligne sur fond jaune.

Source : RG-005-017

#### FR-005-08 : Liens de navigation selon le rôle

Sur l'écran du relevé, le lien de navigation « Espace Conseiller » n'est affiché qu'aux utilisateurs de rôle CONSEILLER ou ADMIN, et le lien « Administration » qu'aux utilisateurs de rôle ADMIN.

Source : RG-005-018, RG-005-019

**Notes :**
- Le bandeau technique de l'application legacy, affiché sous la navigation, n'est pas une règle de gestion et n'est pas repris.
- Contradiction mineure dans le corpus : RG-005-014 cite le signe « − » pour les débits, l'inventaire des messages le tiret « - » ; tranché par A-005-1.

### FEAT-006 — Budget et catégories de dépenses

**Description :** La page Budget est accessible à tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN), avec la même interface pour tous ; chacun n'y voit que ses propres données. Elle ventile les dépenses (opérations au débit) d'un mois choisi par catégorie, à partir de mots-clés reconnus dans le libellé des opérations, et affiche les totaux de dépenses et de revenus du mois. L'utilisateur peut définir un plafond mensuel par catégorie, suivre sa consommation par une barre de progression colorée (vert, orange, rouge) et voir les dépassements. Cas limites marquants : utilisateur sans historique (mois courant affiché), mois sans dépense, catégorie sans plafond, dépenses égales ou supérieures au plafond, suppression d'un plafond sans confirmation. Scénarios golden master : SC-006-01.

**Exigences fonctionnelles :**

#### FR-006-01 : Données limitées à l'utilisateur connecté

Toutes les données de la page Budget (plafonds, ventilation des dépenses, totaux, liste des périodes) portent exclusivement sur le compte client de l'utilisateur authentifié. Un utilisateur ne voit jamais les opérations ni les plafonds d'un autre client.

Source : RG-006-001

#### FR-006-02 : Choix de la période

Le sélecteur « Periode : » propose, au format AAAA-MM, les 24 mois les plus récents qui comptent au moins une opération de l'utilisateur connecté. À l'ouverture de la page, la période sélectionnée est le mois le plus récent comportant des opérations ; si l'utilisateur n'a aucune opération, c'est le mois civil en cours. Dès que l'utilisateur change la valeur du sélecteur, toutes les données de budget et de dépenses sont rechargées pour la nouvelle période, sans action de validation ; le bouton « Afficher » permet aussi de recharger.

Source : RG-006-002, RG-006-003, RG-006-004, RG-006-005

#### FR-006-03 : Ventilation des dépenses par catégorie

La section « Repartition par categorie », titrée « Depenses du mois {AAAA-MM} », ventile uniquement les opérations au débit de l'utilisateur pour le mois sélectionné. Chaque opération est rattachée à la première catégorie dont un mot-clé apparaît dans son libellé, sans tenir compte de la casse ; une opération qui ne correspond à aucun mot-clé est rattachée à la catégorie « Autres ». Seules les catégories dont le montant de dépenses est non nul sont affichées. Si l'utilisateur n'a aucune dépense sur la période, la page affiche « Aucune depense enregistree sur cette periode ».

Source : RG-006-006, RG-006-007, RG-006-008, RG-006-009, RG-006-010

#### FR-006-04 : Totaux du mois

La page affiche trois indicateurs : « Total depenses » (somme des montants des opérations au débit de l'utilisateur pour le mois sélectionné), « Total revenus » (somme des montants des opérations au crédit de l'utilisateur pour le mois sélectionné), tous deux en EUR avec deux décimales, et « Plafonds definis » (nombre de plafonds définis pour le mois sélectionné).

Source : RG-006-011, RG-006-012

#### FR-006-05 : Contrôles du formulaire de plafond

Le formulaire « Definir un plafond » comporte le champ « Categorie » (option vide « -- Choisir -- ») et le champ « Plafond mensuel (EUR) » (exemple affiché « ex: 500 »), et le bouton « Enregistrer le plafond ». Si aucune catégorie n'est choisie, l'enregistrement est refusé avec le message « Veuillez selectionner une categorie ». Si le plafond est vide, nul ou négatif, l'enregistrement est refusé avec le message « Veuillez saisir un plafond valide ». Une saisie non numérique du plafond est refusée.

Source : RG-006-013, RG-006-014

#### FR-006-06 : Catégories proposées pour un plafond, sans la catégorie Revenus

Le sélecteur de catégorie du formulaire de plafond propose uniquement des catégories de dépenses : Logement, Alimentation, Transport, Sante, Loisirs, Abonnements, Epargne, Assurances, Autres. La catégorie de revenus « Revenus » n'est pas proposée, car un plafond de dépenses ne peut pas s'appliquer à des revenus (remplace le comportement legacy RG-006-028, qui proposait « Revenus » et produisait un plafond jamais atteint).

Critère d'acceptation (test) : à l'ouverture de la page Budget, la liste des options du champ « Categorie » contient « -- Choisir -- » et les neuf catégories de dépenses ci-dessus, et ne contient pas « Revenus » ; une tentative d'enregistrer un plafond pour la catégorie Revenus est refusée.

Source : RG-006-028, BUG-023

#### FR-006-07 : Enregistrement d'un plafond

Il existe au plus un plafond par combinaison (client, catégorie, mois). À l'enregistrement d'un plafond valide pour le mois sélectionné : si un plafond existe déjà pour ce client, cette catégorie et ce mois, sa valeur est remplacée par la nouvelle ; sinon, un nouveau plafond est créé. En cas de succès, la page affiche « Plafond enregistre pour {categorie} » ({categorie} = libellé de la catégorie choisie), vide les champs catégorie et plafond, et rafraîchit la liste des plafonds.

Source : RG-006-015, RG-006-016, RG-006-017, RG-006-018

#### FR-006-08 : Suppression d'un plafond

Chaque ligne du tableau des plafonds porte un bouton « Supprimer ». Son activation supprime immédiatement le plafond, sans demande de confirmation.

Source : RG-006-019

#### FR-006-09 : Barre de progression par catégorie

Pour chaque catégorie affichée qui a un plafond défini, la page affiche une barre de progression et le libellé « {pourcentage}% du plafond » ({pourcentage} = dépenses / plafond × 100, en entier). La barre est verte en dessous de 80 % du plafond, orange de 80 % à 99 % inclus, rouge à partir de 100 %. Le remplissage affiché de la barre est plafonné à 100 %, même quand les dépenses dépassent le plafond. Pour une catégorie sans plafond, la zone de progression affiche « Aucun plafond defini ».

Source : RG-006-020, RG-006-021, RG-006-022, RG-006-023, RG-006-025

#### FR-006-10 : Signalement d'un dépassement

Quand les dépenses d'une catégorie sont strictement supérieures à son plafond, la ligne affiche le badge « Depassement », est surlignée sur fond rose et le montant dépensé est affiché en rouge. Des dépenses exactement égales au plafond donnent une barre rouge (FR-006-09) sans badge « Depassement ».

Source : RG-006-024, RG-006-022

#### FR-006-11 : Tableau des plafonds du mois

Le tableau « Plafonds du mois {AAAA-MM} » n'est affiché que si au moins un plafond est défini pour le mois sélectionné. Ses colonnes sont « Categorie », « Plafond », « Depense », « Ecart » et « Action ». L'écart vaut plafond moins dépenses ; il est affiché en rouge quand il est négatif et en vert quand il est positif ou nul.

Source : RG-006-026, RG-006-027

**Feature-specific NFRs :** En cas d'erreur inattendue lors de l'enregistrement ou de la suppression d'un plafond, aucun message technique interne n'est montré à l'utilisateur (le legacy affiche « Erreur : {message technique} » et « Erreur suppression : {message technique} », non reproduits). Un plafond qui dépasse la capacité de stockage du montant (13 chiffres entiers, 2 décimales) est refusé par une validation explicite, pas par une erreur technique.

**Notes :**
- RG-006-028 (Suspect, dette legacy) : remplacée par FR-006-06 (correction BUG-023).
- Le bandeau technique du legacy (mention des technologies) n'est pas reproduit.

### FEAT-007 — Demande et suivi de prêts

**Description :** La page Prêts est accessible à tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN), avec la même interface pour tous ; chacun n'y voit que ses propres emprunts. Elle réunit un simulateur (mensualité, coût total, intérêts et échéancier), un formulaire de demande de prêt et la liste des emprunts de l'utilisateur avec leur échéancier. Une demande valide est enregistrée « En attente d approbation » puis traitée en arrière-plan par le service de scoring crédit (FEAT-024), qui l'approuve (statut « En cours », échéancier généré) ou la refuse (« Refuse par le service credit ») et notifie le client. Cas limites marquants : durée de 301 à 360 mois désormais refusée dès la demande, échéance déjà payée, dernière échéance payée (prêt « Rembourse »), suppression d'un prêt sans confirmation. Scénarios golden master : SC-007-01, SC-007-02.

**Exigences fonctionnelles :**

#### FR-007-01 : Accès à la page Prêts

Si la session n'a pas d'utilisateur authentifié, la demande de la page Prêts redirige vers la page de connexion. Tout utilisateur authentifié, quel que soit son rôle (CLIENT, CONSEILLER, ADMIN), accède à la page Prêts avec la même interface, sans restriction propre au rôle. La page présente les sections « Simulateur de pret », « Demande de pret » et « Mes emprunts ».

Source : RG-007-001, RG-007-002

#### FR-007-02 : Formulaire du simulateur et contrôles

La section « Simulateur de pret » (titre « Simuler un emprunt ») comporte les champs « Montant (EUR) », « Taux annuel (%) » et « Duree (mois) », pré-remplis avec 10 000 EUR, 3,50 % et 36 mois, et le bouton « Calculer ». Si le taux est inférieur à 0 ou supérieur à 30, la simulation est refusée avec le message « Taux invalide (0-30%) ». Si la durée est inférieure ou égale à 0 ou supérieure à 360, la simulation est refusée avec le message « Duree invalide (1-360 mois) ». Le contrôle du montant suit l'exigence fusionnée « Montant invalide » de FEAT-004.

Source : RG-007-004, RG-007-005, RG-007-006

#### FR-007-03 : Calcul de la simulation

Quand la simulation est demandée avec des valeurs valides (montant > 0, taux de 0 à 30, durée de 1 à 360 mois), le système calcule et affiche « Mensualite », « Cout total » et « Total interets ». Si le taux annuel est nul, la mensualité est le montant divisé par le nombre de mois ; sinon, elle suit la formule standard d'annuité constante. Le lien « Voir l echeancier complet ({N} echeances) » donne l'échéancier complet, avec pour chaque échéance les colonnes « # », « Capital », « Interets », « Total » et « Capital restant ». Exemple de référence : 10 000 EUR à 3,5 % sur 36 mois donnent une mensualité de 293.02 EUR, un coût total de 10,548.72 EUR et un total d'intérêts de 548.72 EUR.

Source : RG-007-007, RG-007-008, RG-007-009, RG-007-010

#### FR-007-04 : Types de prêt proposés

Le champ « Type de pret » du formulaire « Nouveau pret » propose, dans cet ordre, « Immobilier », « Automobile », « Consommation », « Travaux » et « Etudiant », avec « Consommation » sélectionné par défaut.

Source : RG-007-011

#### FR-007-05 : Contrôles de la demande de prêt

Le formulaire « Nouveau pret » comporte les champs « Type de pret », « Montant (EUR) », « Taux annuel (%) », « Duree (mois) » et « Objet du pret », et le bouton « Soumettre la demande ». Si le taux est inférieur à 0 ou supérieur à 30, la demande est refusée avec le message « Taux invalide ». Si la durée est inférieure ou égale à 0 ou supérieure à 360, la demande est refusée avec le message « Duree invalide » (voir aussi FR-007-06 pour les durées de 301 à 360 mois). Si l'objet du prêt est vide ou ne contient que des espaces, la demande est refusée avec le message « Veuillez indiquer un motif ». Le contrôle du montant suit l'exigence fusionnée « Montant invalide » de FEAT-004.

Source : RG-007-013, RG-007-014, RG-007-015

#### FR-007-06 : Refus dès la demande d'une durée de 301 à 360 mois

Si la durée saisie dans la demande de prêt est comprise entre 301 et 360 mois inclus, la demande est refusée comme invalide dès la soumission, avec le message « Duree invalide » [ASSUMPTION A-007-1 : le message est celui du contrôle de durée de la demande, « Duree invalide »], aucun prêt n'est enregistré et rien n'est transmis au service de scoring crédit. Cette règle remplace le comportement legacy RG-007-028, où la demande était acceptée puis systématiquement refusée au scoring (RG-007-027) sans message différencié. Le simulateur conserve la plage de 1 à 360 mois (FR-007-02).

Critère d'acceptation (test) : une demande de prêt valide en tout point sauf une durée de 301 mois (puis de 360 mois) est refusée à la soumission avec le message « Duree invalide », et aucune ligne n'apparaît dans « Emprunts en cours » ; une demande identique avec 300 mois est acceptée et transmise.

Source : RG-007-028, BUG-025, RG-007-014

#### FR-007-07 : Enregistrement et transmission d'une demande valide

Quand une demande valide est soumise, le système enregistre un prêt au statut EN_ATTENTE (libellé « En attente d approbation »), avec la mensualité, le coût total, le total des intérêts, une date de début égale au premier jour du mois en cours et une date de fin égale à la date de début plus la durée en mois civils, puis le transmet au service de scoring crédit pour un traitement asynchrone (FEAT-024). La page affiche « Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes) » et rafraîchit la liste des emprunts. L'échec de la transmission est traité par l'exigence FR-024-02.

Source : RG-007-017, RG-007-018, RG-007-019

#### FR-007-08 : Liste des emprunts

La section « Mes emprunts » (titre « Emprunts en cours ») affiche uniquement les prêts de l'utilisateur connecté, triés par date de début décroissante, avec les colonnes « Type », « Montant », « Taux », « Duree », « Mensualite », « Total interets », « Statut », « Echeancier » et « Action ». Les statuts sont affichés avec les libellés « En attente d approbation », « En cours », « Rembourse », « Refuse par le service credit » et « En defaut ». Pour un prêt au statut EN_ATTENTE, la colonne « Echeancier » affiche « En cours de traitement... ». Si l'utilisateur n'a aucun prêt, la page affiche « Aucun emprunt enregistre ».

Source : RG-007-020, RG-007-037

#### FR-007-09 : Règles de scoring crédit

Le service de scoring crédit approuve le prêt s'il respecte les règles suivantes, et le refuse sinon : refus si la durée dépasse 300 mois, quels que soient le type et le montant ; refus si le montant dépasse 25 000 EUR pour CONSOMMATION, 40 000 EUR pour AUTO, 80 000 EUR pour TRAVAUX, 20 000 EUR pour ETUDIANT, 600 000 EUR pour IMMOBILIER. Ces règles sont détaillées dans FR-024-03 à FR-024-05.

Source : RG-007-021, RG-007-022, RG-007-023, RG-007-024, RG-007-025, RG-007-026, RG-007-027

#### FR-007-10 : Approbation, refus et notification

Quand un prêt est approuvé, son statut passe de EN_ATTENTE à EN_COURS et un échéancier de N échéances mensuelles est généré (N = durée en mois). Quand un prêt est refusé, son statut passe de EN_ATTENTE à REFUSE. Dans les deux cas, le client est notifié dans le centre de notifications de l'application : « Votre demande de pret {type} de {montant} EUR a ete approuvee par le service credit. » (type CONFIRMATION) ou « Votre demande de pret {type} de {montant} EUR a ete refusee (plafond depasse ou duree excessive). » (type ALERTE), où {type} est le libellé du type de prêt. Une demande d'approbation ou de refus portant sur un prêt qui n'est pas au statut EN_ATTENTE est ignorée sans erreur ni changement.

Source : RG-007-029, RG-007-030, RG-007-031, RG-007-032

#### FR-007-11 : Consultation de l'échéancier d'un prêt

Pour un prêt au statut EN_COURS ou REMBOURSE, la colonne « Echeancier » affiche le bouton « Voir », qui ouvre la section « Echeancier du pret selectionne » (titre « Detail des echeances »). Pour chaque échéance, elle affiche « # », « Date echeance » (jj/MM/aaaa), « Capital », « Interets », « Total », « Capital restant » et « Statut », ce dernier valant « Paye » pour une échéance payée et « En attente » pour une échéance non payée [ASSUMPTION A-007-2 : libellés du relevé des messages du code, « Paye » / « En attente », retenus de préférence à « Paye / En cours » de la règle].

Source : RG-007-038, RG-007-039

#### FR-007-12 : Paiement d'une échéance

Pour une échéance non payée, la colonne « Action » affiche le bouton « Marquer paye ». Son activation marque l'échéance comme payée, enregistre la date de paiement et affiche « Echeance marquee comme payee ». Une échéance déjà payée n'est pas payée une seconde fois, sans message d'erreur. Quand toutes les échéances du prêt sont payées, le prêt passe au statut REMBOURSE (« Rembourse »).

Source : RG-007-033, RG-007-034, RG-007-035, RG-007-036

#### FR-007-13 : Suppression d'un prêt

Chaque ligne de la liste des emprunts porte un bouton « Supprimer ». Son activation supprime définitivement le prêt et toutes ses échéances, sans demande de confirmation.

Source : RG-007-040

**Feature-specific NFRs :** En cas d'erreur inattendue lors de la simulation, de la demande, du paiement ou de la suppression, aucun message technique interne n'est montré à l'utilisateur (le legacy affiche « Erreur simulation : {message technique} », « Erreur : {message technique} » et « Erreur suppression : {message technique} », non reproduits).

**Notes :**
- RG-007-003 : couverte par l'exigence fusionnée de FEAT-004 (règles voisines).
- RG-007-012 : couverte par l'exigence fusionnée de FEAT-004 (règles voisines).
- RG-007-016 (dette legacy) : non reproduite. Le legacy ne contrôle pas la longueur de l'objet du prêt (255 caractères au plus en stockage) ; un dépassement provoque une erreur technique. BUG-024 (objet de prêt trop long, erreur technique affichée) et BUG-026 (objet de plus de 255 caractères en contournant la saisie) sont des bugs conservés : leur correction n'est pas demandée.
- RG-007-028 (Suspect, dette legacy) : remplacée par FR-007-06 (correction BUG-025).
- Le statut DEFAUT (« En defaut ») existe dans la liste des statuts mais aucune transition n'y mène dans le legacy.
- Le corpus donne « Aucun emprunt enregistré » dans la règle RG-007-020 et « Aucun emprunt enregistre » dans le relevé des messages ; le relevé des messages fait foi.
- Le texte informatif legacy sous le bouton « Soumettre la demande » et le bandeau technique nomment des technologies : ils ne sont pas reproduits.
- L'approbation ou le refus manuel d'un prêt par un ADMIN relève de l'écran d'administration (autre fonctionnalité) ; la garde de FR-007-10 s'y applique aussi.

### FEAT-008 — Documents (relevés, attestations, IFU)

**Description :** Tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) consulte la page documents, en lecture seule, qui propose pour chacun de ses comptes un RIB, une attestation de solde et les relevés mensuels des 12 derniers mois, ainsi que l'IFU des deux dernières années fiscales. Chaque lien déclenche le téléchargement d'un PDF généré à la demande (le contenu détaillé des PDF est décrit dans FEAT-017). La page ne comporte aucun formulaire ni saisie. Les cas limites portent sur l'accès sans session, les paramètres de téléchargement manquants ou inconnus, le compte introuvable et le compte qui n'appartient pas à l'utilisateur. Scénarios golden master : SC-008-01.

**Exigences fonctionnelles :**

#### FR-008-01 : Redirection vers la connexion des écrans réservés aux utilisateurs authentifiés

Quand un utilisateur non authentifié demande la page documents, la page de chat avec le conseiller, l'espace conseiller (portefeuille clients) ou la page d'administration, le système le redirige vers la page de connexion. Les quatre règles d'origine ont le même effet (redirection vers la page de connexion), sans différence de condition ni de valeur.

Source : RG-008-001, RG-010-001, RG-015-001, RG-016-001

#### FR-008-02 : Téléchargement sans session

Quand une demande de téléchargement de document arrive sans session authentifiée, le système redirige vers la page de connexion, sans générer de document (même exigence côté téléchargement : FR-017-01).

Source : RG-008-002

#### FR-008-03 : Accès à la page documents pour tous les rôles

Le système autorise tout utilisateur authentifié, quel que soit son rôle (CLIENT, CONSEILLER, ADMIN), à accéder à la page documents et à télécharger ses propres documents.

Source : RG-008-003

#### FR-008-04 : Refus d'un document portant sur le compte d'un autre client

Si un utilisateur connecté demande un relevé, un RIB ou une attestation pour un compte qui ne lui appartient pas, le système refuse la demande et ne sert aucun document. Ce contrôle remplace le comportement legacy RG-008-004, où tout identifiant de compte existant était servi sans vérification d'appartenance. Le refus est indépendant du compte introuvable (FR-008-11) : un compte existant mais d'un autre client est refusé, un compte inexistant reste en 404. [ASSUMPTION A-008-1 : le refus est signalé comme un accès interdit (HTTP 403) ; le corpus ne donne ni code ni message.]

Critère d'acceptation (test) : connecté en tant que client A, une demande de relevé, de RIB ou d'attestation sur un compte existant du client B est refusée et aucun PDF n'est renvoyé ; la même demande sur un compte du client A renvoie le PDF.

Source : RG-008-004, BUG-027

#### FR-008-05 : Documents proposés par compte

Quand un utilisateur authentifié affiche la page documents, le système affiche, pour chacun de ses comptes, une section "Compte {type de compte} - {numéro de compte}" contenant : une carte RIB (étiquette "Bancaire", titre "RIB", sous-titre "Releve d Identite Bancaire", lien "Telecharger PDF"), une carte attestation (étiquette "Attestation", titre "Attestation de solde", sous-titre "Solde au jour de la demande", lien "Telecharger PDF"), et une liste "Releves mensuels — {numéro de compte}" avec un lien "{mois} {année}" par mois pour les 12 mois les plus récents. Les libellés de mois sont "Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre".

Source : RG-008-005

#### FR-008-06 : Documents fiscaux (IFU N-1 et N-2)

Quand un utilisateur authentifié affiche la page documents, le système affiche une section "Documents fiscaux" titrée "Imprime Fiscal Unique (IFU)" avec deux cartes, "IFU {N-1}" et "IFU {N-2}" (N étant l'année civile en cours), chacune avec l'étiquette "Fiscal", le sous-titre "Revenus de capitaux mobiliers" et le lien "Telecharger PDF".

Source : RG-008-006

#### FR-008-07 : Relevé mensuel

Quand un relevé est demandé pour un compte, une année et un mois, le système génère et sert un PDF qui contient toutes les opérations du compte sur cette période, triées par date d'opération croissante (contenu détaillé : FR-017-04).

Source : RG-008-007

#### FR-008-08 : RIB

Quand un RIB est demandé pour un compte, le système génère et sert un PDF qui contient le numéro de compte, l'IBAN, le BIC/SWIFT, le code banque, le code guichet et le nom du titulaire (contenu détaillé : FR-017-05).

Source : RG-008-008

#### FR-008-09 : Attestation de solde

Quand une attestation est demandée pour un compte, le système génère et sert un PDF qui certifie le solde courant de ce seul compte, daté du moment de la demande (contenu détaillé : FR-017-06).

Source : RG-008-009

#### FR-008-10 : IFU de l'utilisateur connecté

Quand un IFU est demandé pour une année, le système le génère pour le client de la session authentifiée, jamais pour un client désigné dans les paramètres de la demande. Le revenu d'intérêts déclaré est le total des opérations au crédit dont la description contient "interet", et le système lui applique un Prélèvement Forfaitaire Unique (PFU) de 30 % (calcul et arrondi détaillés : FR-017-07).

Source : RG-008-010, RG-008-011

#### FR-008-11 : Demandes de téléchargement invalides

Le système rejette une demande de téléchargement : avec un code 400 et le message "Parametre type manquant" si le type de document est absent ; avec un code 400 et le message "accountId manquant" si l'identifiant de compte manque pour un relevé, un RIB ou une attestation ; avec un code 400 et le message "Type inconnu : {type}" si le type n'est ni RELEVE, ni RIB, ni ATTESTATION, ni IFU ; avec un code 404 et le message "Compte introuvable" si le compte demandé n'existe pas.

Source : RG-008-012, RG-008-013, RG-008-014, RG-008-015

#### FR-008-12 : Compteur de téléchargements

Quand un document est téléchargé, le système incrémente le compteur de téléchargements de ce document.

Source : RG-008-018

**Notes :**
- RG-008-016 (dette legacy) : non reproduite. Bugs conservés associés à la validation de l'année et du mois d'un relevé ou d'un IFU : BUG-028, BUG-030, BUG-031, BUG-032 (le legacy génère un PDF vide sans erreur).
- RG-008-017 (dette legacy) : non reproduite. Bug conservé associé : BUG-029 (SIRET de la banque écrit en dur).
- RG-010-001, RG-015-001, RG-016-001 : intégrées dans l'exigence fusionnée FR-008-01, dont FEAT-008 est porteuse (règles voisines).
- Les messages de génération de documents enregistrés ("Session expirée.", "Relevé {mois} {année} généré avec succès.", "Impossible de générer le relevé.", etc.) ne sont jamais affichés dans le parcours de la page documents : non repris.
- Le bandeau technique de la page legacy (pile technique, adresse du service de téléchargement) n'est pas repris.
- Recouvrement avec FEAT-017 (même service de téléchargement, aucune fusion décidée) : les FR de FEAT-008 décrivent la page et l'effet attendu, celles de FEAT-017 le contenu des PDF et les réponses de téléchargement.
- Écart corpus : le legacy observé en exécution renvoie une erreur 500 pour tout relevé, RIB ou attestation (défaut de chargement des données) et une erreur 500 au lieu de 404 pour un compte inexistant (BUG-061, conservé, voir FEAT-017). Les FR reprennent le comportement décrit par les règles.

### FEAT-009 — Bourse et watchlist

**Description :** Tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) consulte la page bourse : il choisit une valeur de la watchlist et une période ("1 Semaine", "1 Mois", "1 An", "5 Ans"), et voit le cours, la variation, la performance, un graphique d'évolution et les dix dernières séances, issus du fournisseur de cotations AlphaVantage. La watchlist est unique et partagée par tous les utilisateurs ; on y ajoute une valeur par un formulaire intégré à la page et on la supprime sans confirmation. Cas limites : absence de données du fournisseur (message d'erreur contextuel), symbole en double, watchlist vidée. L'intégration avec le fournisseur est détaillée dans FEAT-029. Scénarios golden master : SC-009-01.

**Exigences fonctionnelles :**

#### FR-009-01 : Accès à la page bourse

Le système autorise l'accès à la page bourse à tout utilisateur authentifié, quel que soit son rôle (CLIENT, CONSEILLER, ADMIN), sans autre restriction de rôle.

Source : RG-009-019

#### FR-009-02 : Chargement initial

Quand l'utilisateur ouvre la page bourse, le système charge la watchlist, sélectionne automatiquement sa première valeur (ordre : marché, puis symbole) et récupère son historique avant d'afficher la page.

Source : RG-009-001

#### FR-009-03 : Changement de valeur ou de période

Quand l'utilisateur change la valeur sélectionnée ("Valeur") ou la période ("Période" : "1 Semaine", "1 Mois", "1 An", "5 Ans"), le système récupère et affiche l'historique mis à jour sans recharger toute la page.

Source : RG-009-002

#### FR-009-04 : Indicateurs de marché

Quand un historique est disponible, le système affiche le libellé "{nom} — {symbole}" avec le cours actuel (clôture du point le plus récent), "Variation sur la période" (dernière clôture − première clôture, préfixée "+" si positive), "Performance %" ((dernière − première) / première × 100) et "Marché".

Source : RG-009-003

#### FR-009-05 : Couleur selon la performance

Quand la performance en pourcentage est supérieure ou égale à zéro, le système affiche le graphique et les indicateurs en vert (#2e7d32) avec le préfixe "▲ +" ; quand elle est strictement négative, en rouge (#E3000F) avec le préfixe "▼ ".

Source : RG-009-004

#### FR-009-06 : Graphique d'évolution du cours

Quand un historique est disponible, le système affiche, sous le titre "📈 Évolution du cours", un graphique vectoriel généré côté serveur avec cinq dates régulièrement espacées en abscisse au format MM-JJ, cinq lignes horizontales de grille et un badge de période : "1 semaine (données journalières)", "1 mois (données journalières)", "1 an (données hebdomadaires)" ou "5 ans (données hebdomadaires)".

Source : RG-009-005

#### FR-009-07 : Dernières séances

Quand un historique est disponible, le système affiche sous "📋 Dernières séances" les 10 derniers points, du plus récent au plus ancien, avec les colonnes "Date", "Ouverture", "Haut", "Bas", "Clôture" (2 décimales), "Volume" et "Var. J" ((clôture − ouverture) / ouverture × 100, préfixée "+" si clôture ≥ ouverture) ; chaque ligne est verte si clôture ≥ ouverture, rouge sinon.

Source : RG-009-006

#### FR-009-08 : Données demandées au fournisseur selon la période

Pour les périodes "1W" et "1M", le système demande au fournisseur la série journalière compacte (100 derniers jours) et ne garde que les points datés au plus tôt d'aujourd'hui moins 1 semaine ou moins 1 mois. Pour "1Y" et "5Y", il demande la série hebdomadaire et ne garde que les points datés au plus tôt d'aujourd'hui moins 1 an ou moins 5 ans. L'accès au fournisseur suit NFR-INT.

Source : RG-009-007, RG-009-008

#### FR-009-09 : Absence de données du fournisseur

Si le fournisseur répond avec un statut autre que succès, ou par un message d'information, de limite ou d'erreur, ou par une série vide, le système masque le graphique et le tableau et affiche "⚠️ Aucune donnée reçue pour « {SYMBOLE} » sur {libellé de période}. Causes possibles : symbole invalide pour cette place boursière (vérifiez le suffixe, ex: .PA pour Euronext), ou limite de 25 requêtes/jour atteinte (plan gratuit Alpha Vantage). Réessayez dans quelques minutes ou changez de valeur.", où le libellé de période est celui de FR-009-06.

Source : RG-009-009

#### FR-009-10 : Watchlist unique et partagée

Le système gère une seule watchlist commune à tous les utilisateurs authentifiés, quel que soit leur rôle, sans filtrage par utilisateur, et la présente triée par marché croissant puis par symbole croissant. Elle s'affiche sous le titre "⭐ Ma Watchlist ({N} valeurs)" avec les colonnes "Symbole", "Nom", "Marché", "Devise", "Suppr.", et son nombre de valeurs apparaît à côté du sélecteur sous la forme "({N} valeurs)".

Source : RG-009-010, RG-009-011

#### FR-009-11 : Formulaire d'ajout et contrôles

Le bouton "+ Ajouter une valeur" ouvre dans la page le formulaire "➕ Ajouter une valeur à la watchlist" : "Symbole *" (exemple "ex: AMZN", aide "Symbole Alpha Vantage"), "Nom *" (exemple "ex: Amazon"), "Marché" ("Euronext Paris" par défaut, "NASDAQ", "NYSE", "London SE", "Autre"), "Devise" ("EUR" par défaut, "USD", "GBP"), boutons "Ajouter" et "Annuler". À la soumission, le formulaire reste ouvert et la soumission est rejetée avec le message "Le symbole est obligatoire (ex: AMZN, SG.PA)" si le symbole est vide ou blanc, "Le nom de la valeur est obligatoire" si le nom est vide ou blanc, et "Le symbole « {SYMBOLE} » existe déjà dans votre watchlist" (symbole en majuscules) si le symbole existe déjà, sans tenir compte de la casse.

Source : RG-009-012, RG-009-013, RG-009-014

#### FR-009-12 : Enregistrement d'une nouvelle valeur

Quand une valeur est ajoutée, le système enregistre le symbole en majuscules sans espaces de bord (20 caractères au plus), le nom sans espaces de bord (100 au plus), le marché sans espaces de bord (50 au plus) et la devise en majuscules sans espaces de bord (10 au plus), affiche "Valeur ajoutée !", sélectionne la nouvelle valeur, ferme le formulaire et récupère son historique. Le refus des symboles de plus de 20 caractères et des noms de plus de 100 est décrit par FR-029-13.

Source : RG-009-015, RG-009-016

#### FR-009-13 : Suppression d'une valeur

Le bouton "✕" (infobulle "Supprimer {nom} de la watchlist") supprime immédiatement la valeur, sans demande de confirmation. Si la valeur supprimée était sélectionnée, le système sélectionne la première valeur restante et récupère ses données ; si la watchlist devient vide, il efface la sélection et masque le graphique et le tableau.

Source : RG-009-017, RG-009-018

#### FR-009-14 : Clé d'accès au fournisseur de cotations hors du code source

Le système lit la clé d'accès AlphaVantage uniquement depuis une configuration sécurisée (variable d'environnement ou propriété système) et n'utilise jamais de valeur par défaut présente dans le code source. Ce comportement remplace le comportement legacy RG-009-021 (clé par défaut écrite en clair dans le code).

Critère d'acceptation (test) : aucune clé d'accès ne figure dans le code source ni dans les fichiers versionnés ; sans clé configurée, aucun appel réel n'est émis avec une clé par défaut.

Source : RG-009-021, BUG-034

**Notes :**
- RG-009-020 (dette legacy) : non reproduite. Bug conservé associé : BUG-033 (appel au fournisseur sans délai d'attente). Le délai d'attente est de toute façon imposé par NFR-INT.
- BUG-035 (nom de plus de 100 caractères : erreur technique) et BUG-036 (symbole de plus de 20 caractères tronqué sans avertissement) sont conservés ; le champ de saisie legacy limite déjà la longueur (voir FR-029-13).
- Recouvrement avec FEAT-029 (aucune fusion décidée) : FR-009-02 à FR-009-13 et FR-029-01 à FR-029-14 décrivent le même écran et doivent rester identiques.
- Le bandeau technique de la page legacy (pile technique et base de données) n'est pas repris.

### FEAT-010 — Chat avec le conseiller

**Description :** Tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) dialogue avec « Sophie », une conseillère virtuelle animée par un service d'IA externe (Azure OpenAI), depuis l'écran « Mon Conseiller ». À l'ouverture, une conversation neuve démarre avec un message de bienvenue personnalisé au prénom de l'utilisateur et quatre actions rapides. Chaque message envoyé est transmis au service d'IA avec l'historique de la conversation ; la réponse s'affiche comme un message de Sophie. Les indisponibilités du service d'IA se traduisent par un message dégradé affiché dans la conversation, jamais par une page d'erreur. L'utilisateur peut repartir d'une conversation neuve à tout moment. Scénarios golden master : SC-010-01, SC-010-02.

**Exigences fonctionnelles :**

#### FR-010-01 : Accès aux écrans ouverts à tout utilisateur authentifié

L'écran « Mon Conseiller » (chat), l'écran du catalogue des produits et l'écran des bénéficiaires sont accessibles à tout utilisateur authentifié, quel que soit son rôle (CLIENT, CONSEILLER, ADMIN), sans différence de contenu selon le rôle. Le corpus donne la même règle pour les trois écrans (même ensemble de rôles, aucune restriction) ; aucune différence de valeur n'est à arbitrer. L'utilisateur non authentifié est redirigé vers la page de connexion (exigence fusionnée de FEAT-008).

Source : RG-010-002, RG-011-001, RG-013-002

#### FR-010-02 : Message de bienvenue personnalisé

À l'ouverture de l'écran, le système affiche l'en-tête « Sophie — Conseillère Demo Bank », l'indicateur « En ligne · Répond en quelques secondes », et un premier message de Sophie : « Bonjour {prenom} ! Je suis Sophie, votre conseillere Demo Bank. Je suis la pour repondre a vos questions sur vos comptes, vos placements ou tout projet financier. Comment puis-je vous aider aujourd'hui ? », où {prenom} est le premier mot du nom complet du client connecté. Si aucun client n'est associé à la session de l'utilisateur, {prenom} est remplacé par « cher client » (« Bonjour cher client ! … »).

Source : RG-010-003, RG-010-004

#### FR-010-03 : Conversation neuve à chaque ouverture

À chaque ouverture de l'écran, la conversation est réinitialisée et ne contient que le message de bienvenue, avant toute interaction de l'utilisateur.

Source : RG-010-005

#### FR-010-04 : Actions rapides

Tant que la conversation ne contient que le message de bienvenue, quatre boutons d'action rapide sont affichés. Un clic sur un bouton envoie, comme message de l'utilisateur, le texte prédéfini associé, puis masque les quatre boutons pour le reste de la conversation :
- « Optimiser mon epargne » → « Je voudrais optimiser mon epargne, que me conseillez-vous ? »
- « Projet immobilier » → « Je souhaite acheter un appartement, comment preparer mon dossier ? »
- « Investir en bourse » → « Je veux commencer a investir en bourse, par ou commencer ? »
- « Preparer ma retraite » → « Comment preparer ma retraite efficacement ? »

Le message envoyé suit ensuite le même traitement que FR-010-05, y compris la gestion des indisponibilités de FR-010-09.

Source : RG-010-006, RG-010-007

#### FR-010-05 : Envoi d'un message

La zone de saisie porte le texte indicatif « Posez votre question a Sophie... » et le bouton « Envoyer ». À l'envoi d'un message non vide, le système ajoute le message de l'utilisateur à la conversation, interroge le service d'IA, puis ajoute la réponse comme message de Sophie. Un message vide ou composé uniquement d'espaces est ignoré, sans message d'erreur ni appel au service d'IA.

Source : RG-010-008, RG-010-009

#### FR-010-06 : Historique transmis au service d'IA

Pour le premier message de la conversation, le service d'IA est interrogé avec un historique vide. Pour les messages suivants, l'historique transmis contient tous les échanges précédents, à l'exclusion du message de bienvenue et du message en cours (envoyé séparément).

Source : RG-010-011, RG-010-012

#### FR-010-07 : Consignes de comportement de Sophie

Chaque requête au service d'IA porte une consigne fixe qui définit la persona Sophie, ses six domaines d'expertise et ses contraintes de comportement. Cette consigne impose : ne jamais donner de conseil d'investissement ferme et mentionner que les placements comportent des risques (conformité MiFID II) ; répondre exclusivement en français ; répondre de façon concise, en 3 à 5 phrases, sauf si l'utilisateur demande explicitement une réponse détaillée. Chaque réponse est limitée à 500 unités de génération (tokens), avec une température de génération de 0,7.

Source : RG-010-014, RG-010-015, RG-010-016, RG-010-017, RG-010-018

#### FR-010-08 : Raccordement au service d'IA

Le système interroge le service de complétion de conversation d'Azure OpenAI ; l'adresse du service, le nom du déploiement, la version d'API et la clé sont des paramètres de configuration de déploiement (priorité : variable d'environnement, puis paramètre système, puis valeur par défaut). Chaque appel applique un délai de connexion de 15 secondes et un délai de lecture de 30 secondes. Le port, l'adaptateur simulé actif par défaut, le coupe-circuit et le cache relèvent de la NFR transverse NFR-INT.

Source : RG-010-013, RG-010-019

#### FR-010-09 : Indisponibilité du service d'IA (correction BUG-038)

Les erreurs du service d'IA s'affichent dans une bulle de Sophie, dans la conversation, et non dans une zone d'erreur distincte :
- si le service rejette l'authentification de l'application, le système affiche « Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence. », sans code technique (remplace, pour ce cas, le comportement legacy de RG-010-020, qui exposait « (HTTP 401) ») ; [ASSUMPTION A-010-1 : « erreur d'authentification » couvre les statuts 401 et 403, et réutilise le message de l'exception technique]
- pour tout autre statut de réponse en erreur, le système affiche « Je suis momentanement indisponible (HTTP {status}). Veuillez reessayer. », {status} étant le code reçu ;
- si l'appel échoue sans réponse (délai dépassé, réseau, réponse illisible), le système affiche « Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence. ».

Le cas se produit aussi bien après un message saisi qu'après une action rapide.

Critère d'acceptation (test) : avec un service d'IA simulé qui répond par un refus d'authentification (401), l'envoi de « Quel est mon solde courant ? » affiche ce message dans la conversation, suivi d'une bulle de Sophie contenant « Je suis momentanement indisponible » et ne contenant ni « HTTP 401 » ni « 401 ».

Source : BUG-038, RG-010-007, RG-010-020, RG-010-021

#### FR-010-10 : Nouvelle conversation

Un clic sur « Nouvelle conversation » efface tous les messages et réinitialise la conversation avec un nouveau message de bienvenue personnalisé (FR-010-02) ; les actions rapides réapparaissent (FR-010-04).

Source : RG-010-022

#### FR-010-11 : Comportement de la zone de saisie

Pendant le traitement d'un envoi, le bouton « Envoyer » est désactivé et affiche « ... » ; il est rétabli à la fin du traitement. La touche Entrée sans la touche Maj envoie le message ; Maj+Entrée insère un saut de ligne. À la réception d'une réponse de Sophie, la liste des messages défile pour afficher le dernier message.

Source : RG-010-023, RG-010-024, RG-010-025

**Feature-specific NFRs :** chaque appel au service d'IA applique un délai de connexion de 15 secondes et un délai de lecture de 30 secondes (FR-010-08) ; le reste (port, simulation, coupe-circuit, cache) relève de NFR-INT.

**Notes :**
- RG-010-001 : couverte par l'exigence fusionnée de FEAT-008 (règles voisines).
- RG-010-010 (Suspect, dette legacy) : non reprise — la règle décrit un vidage du champ après envoi que le legacy ne fait pas, et le bug associé BUG-037 (champ de saisie non vidé après envoi) est classé « ne pas corriger ». [ASSUMPTION A-010-2 : le champ de saisie n'est pas vidé automatiquement après l'envoi, comme dans le legacy]
- SC-010-02 : le scénario attend « Je suis momentanement indisponible » ; il reste vrai après la correction BUG-038 (FR-010-09), à condition que le rejeu utilise un service d'IA simulé qui renvoie un refus d'authentification, et non l'adaptateur simulé nominal (NFR-INT), qui renverrait une réponse normale.
- Le bandeau technique du legacy (nom du modèle, de la plateforme et du composant serveur) n'est porté par aucune règle et n'est pas repris.

### FEAT-011 — Catalogue produits bancaires

**Description :** Tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) consulte, en lecture seule, le catalogue des produits bancaires actifs, réparti en trois sections (épargne, crédit, investissement) et trié par nom, ainsi que l'historique des traitements nocturnes. Un bouton "Rafraichir" recharge ces données. Cas marquants : valeurs absentes remplacées par "—" (ou "Illimite" pour la durée d'un produit d'épargne), sections vides, taux affichés en pourcentage, historique de traitements vide. Scénarios golden master : SC-011-01.

**Exigences fonctionnelles :**

#### FR-011-01 : Catalogue en lecture seule

Le système présente le catalogue des produits sous le titre "Catalogue des produits" comme une vue en lecture seule, sans aucun champ de saisie.

Source : RG-011-002

#### FR-011-02 : Chargement et rafraîchissement

À l'ouverture de la session d'un utilisateur authentifié, le système charge le catalogue des produits et l'historique des traitements. Quand l'utilisateur actionne le bouton "Rafraichir", le système recharge le catalogue et l'historique et reste sur la même page.

Source : RG-011-003, RG-011-004

#### FR-011-03 : Produits affichés, tri et sections

Le système n'affiche que les produits actifs, triés par nom en ordre alphabétique, répartis selon leur type en trois sections : "Produits d epargne" (colonnes "Nom", "Description", "Taux", "Depot min", "Capital garanti", "Duree max"), "Produits de credit" ("Nom", "Description", "Taux", "Duree max", "LTV max", "Assurance requise") et "Produits d investissement" ("Nom", "Description", "Secteur", "Niveau de risque", "Rendement attendu"). Le système sait récupérer les produits d'un seul type (épargne, crédit ou investissement). Les colonnes booléennes affichent "Oui" ou "Non" ; une durée renseignée s'affiche "{n} mois".

Source : RG-011-005, RG-011-006, RG-011-007, RG-011-020

#### FR-011-04 : Valeurs absentes

Si un champ facultatif d'un produit est vide (taux d'intérêt, dépôt minimal d'ouverture, LTV maximale, secteur, rendement attendu, durée maximale d'un produit de crédit), le système affiche "—" à sa place, sauf la durée maximale vide d'un produit d'épargne, qui s'affiche "Illimite".

Source : RG-011-008, RG-011-009

#### FR-011-05 : Taux et rendements en pourcentage

Le système affiche le taux d'intérêt et le rendement attendu d'un produit comme un pourcentage à deux décimales avec virgule (par exemple, une valeur stockée de 0,0300 s'affiche "3,00 %"). Ce comportement remplace le comportement legacy RG-011-010, qui affichait la fraction brute suivie de "%" (valeur cent fois trop petite).

Critère d'acceptation (test) : un produit dont le taux stocké vaut 0,0300 affiche "3,00 %" dans la colonne "Taux" ; un produit d'investissement dont le rendement attendu stocké vaut 0,0650 affiche "6,50 %" dans la colonne "Rendement attendu".

Source : RG-011-010, BUG-039

#### FR-011-06 : Niveau de risque d'un produit d'investissement

Le système affiche le niveau de risque sous la forme "{libellé} ({score}/5)", avec l'échelle 1 = "Tres faible", 2 = "Faible", 3 = "Moyen", 4 = "Eleve", 5 = "Tres eleve" ; un score hors de 1 à 5 affiche le libellé "N/A".

Source : RG-011-011

#### FR-011-07 : Valeurs par défaut des produits

À sa création, un produit d'épargne a la garantie du capital à vrai par défaut, et un produit de crédit a l'assurance requise à faux par défaut.

Source : RG-011-012, RG-011-013

#### FR-011-08 : Sections vides

Si aucun produit d'un type n'est disponible, la section correspondante affiche "Aucun produit d epargne disponible", "Aucun produit de credit disponible" ou "Aucun produit d investissement disponible". Si aucun traitement n'a été enregistré, la section d'historique affiche "Aucun traitement nocturne enregistre — les jobs s executent a 02h00, 02h30 et le 1er du mois a 03h00". [ASSUMPTION A-011-1 : le message vide d'une section s'affiche dès que ce type n'a aucun produit, comme le dit la règle ; la page legacy ne l'affiche que si tout le catalogue est vide.]

Source : RG-011-014, RG-011-015

#### FR-011-09 : Historique des traitements nocturnes

Le système affiche, sous le titre "Historique des traitements nocturnes", au plus les 50 dernières exécutions, de la plus récente à la plus ancienne (heure de début), avec les colonnes "Job", "Debut", "Fin", "Duree (ms)", "Enregistrements", "Statut", "Details". Le statut enregistré vaut "SUCCESS", "FAILED" ou "PARTIAL". La durée vaut fin moins début, en millisecondes, et vaut 0 si le début ou la fin est absent. Le séparateur de section affiche "Traitements nocturnes". [ASSUMPTION A-011-2 : les deux titres de section legacy perdent la mention technique entre parenthèses qui les suit.]

Source : RG-011-016, RG-011-017, RG-011-018, RG-011-019

**Notes :**
- RG-011-001 : couverte par l'exigence fusionnée de FEAT-010 (règles voisines).
- Le bandeau technique de la page legacy (pile technique, modèle d'héritage des produits) n'est pas repris.

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

Au retour de Powens : si un identifiant de connexion (`connection_id`) est reçu, le chargement des comptes externes est lancé avec cet identifiant ; si seul un code est reçu, la branche « code » suit la décision portée par FR-014-04 ; si ni l'un ni l'autre n'est reçu (et pas de paramètre d'erreur), le système affiche « Callback Powens incomplet. Parametres recus: [<liste des paramètres reçus>] » et reste à l'état non connecté. L'ordre de priorité entre les paramètres est celui de FR-014-02.

Source : RG-012-008, RG-012-010, RG-012-011

#### FR-012-07 : Retour de Powens en erreur (correction BUG-040, BUG-044)

Quand le retour de Powens porte un paramètre d'erreur (par exemple une annulation par l'utilisateur), le système déconnecte la session Powens (FR-012-15), revient à l'état non connecté et affiche un message qui explique le motif du retour, au lieu du retour silencieux du legacy. [ASSUMPTION A-012-1 : le message est « La connexion a votre banque n a pas abouti ({motif}). Veuillez recommencer la connexion. », {motif} étant la valeur du paramètre d'erreur reçu ; il est posé après la remise à zéro de la session, pour ne pas être effacé par elle]

Critère d'acceptation (test) : un utilisateur authentifié qui arrive sur le retour Powens avec `error=user_cancelled` est redirigé vers l'écran « Comptes Tiers », voit le bloc « Agregez vos comptes bancaires » et un message d'erreur non vide qui contient « user_cancelled ».

Source : BUG-040, BUG-044, RG-012-009

#### FR-012-08 : Session Powens expirée

Si, au traitement du retour de Powens, la session ne contient plus le jeton Powens, le système affiche « Session expiree. Veuillez recommencer la connexion. » et reste à l'état non connecté.

Source : RG-012-012

#### FR-012-09 : Résultat du chargement des comptes après le retour

Si le chargement réussit et renvoie au moins un compte, l'écran passe à l'état connecté, le premier compte de la liste est sélectionné automatiquement et ses transactions sont chargées. Si le chargement réussit mais ne renvoie aucun compte, l'écran passe à l'état connecté et affiche « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. ». Un chargement en erreur suit FR-012-14, et non ce message.

Source : RG-012-013, RG-012-014

#### FR-012-10 : Patrimoine total

Le patrimoine total est la somme des soldes connus de tous les comptes externes ; un compte dont le solde est absent n'y contribue pas. Le montant est affiché en euros, avec séparateur de milliers et deux décimales.

Source : RG-012-015

#### FR-012-11 : Carte d'un compte externe

Chaque compte externe est présenté par une carte avec le libellé fixe « Banque externe », le nom du compte, son type, son solde (deux décimales, séparateur de milliers) et son IBAN. Le type est traduit : checking → « Compte courant », savings → « Epargne », card → « Carte », loan → « Credit », market → « Bourse », type absent → « Compte » ; tout autre type est affiché tel que renvoyé par Powens. L'IBAN n'est affiché que si Powens renvoie une valeur non vide.

Source : RG-012-016, RG-012-017

#### FR-012-12 : Solde absent (correction BUG-042)

Quand Powens ne renvoie pas le solde d'un compte (champ absent ou vide), la carte du compte l'indique comme indisponible au lieu d'afficher « 0,00 EUR » ; un solde réellement nul reste affiché « 0,00 EUR ». Ce remplacement vaut aussi pour la règle legacy RG-028-020 (voir FR-028-09). [ASSUMPTION A-012-2 : le libellé affiché est « Solde indisponible »]

Critère d'acceptation (test) : avec un Powens simulé qui renvoie un compte A sans solde et un compte B au solde 0, la carte A affiche « Solde indisponible », la carte B affiche « 0,00 », et le patrimoine total vaut la somme des seuls soldes renvoyés.

Source : BUG-042, RG-012-018

#### FR-012-13 : Transactions d'un compte externe

Au clic sur un compte externe, ses transactions sont chargées et seule la section des transactions est mise à jour, sans rechargement de l'écran. La section porte le titre « Transactions — {nom du compte} » et les colonnes « Date », « Libelle », « Montant » ; sans transaction, elle affiche « Aucune transaction disponible. ». Au plus 20 transactions par compte sont récupérées (même limite que FR-014-08). Le libellé d'une transaction est son libellé simplifié, ou, s'il est vide, son libellé d'origine. Un montant strictement positif est un crédit, affiché en vert avec le préfixe « + » ; un montant nul ou négatif est un débit, affiché en rouge sans préfixe « + ».

Source : RG-012-019, RG-012-020, RG-012-021, RG-012-022, RG-012-023

#### FR-012-14 : Service Powens indisponible (correction BUG-043)

Si Powens répond en erreur (statut 400 ou plus) au chargement des comptes ou des transactions, le système informe l'utilisateur de l'indisponibilité temporaire du service, au lieu d'afficher une liste vide comme « aucun compte » ou « Aucune transaction disponible. ». [ASSUMPTION A-012-3 : le message est « Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants. », affiché dans la zone d'erreur de l'écran] [ASSUMPTION A-012-4 : si l'erreur survient au chargement des comptes après le retour de Powens, l'écran reste à l'état non connecté et le message « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. » n'est pas affiché]

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

### FEAT-013 — Gestion des bénéficiaires externes

**Description :** L'écran « Beneficiaires » permet à tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) d'enregistrer des bénéficiaires externes SEPA (libellé, titulaire, IBAN, BIC et banque facultatifs), de vérifier un IBAN avant l'ajout, de consulter la liste de ses bénéficiaires actifs avec IBAN masqué et badge de zone SEPA, et de supprimer un bénéficiaire. L'IBAN est contrôlé par l'algorithme MOD-97, le BIC par son format, et la zone SEPA est déduite du code pays de l'IBAN. La suppression est logique (le bénéficiaire est désactivé, pas effacé). Les cas limites marquants sont la liste vide, l'IBAN hors zone SEPA, la session expirée à la soumission et la suppression du bénéficiaire d'un autre client (désormais refusée). Scénarios golden master : SC-013-01, SC-013-02.

**Exigences fonctionnelles :**

#### FR-013-01 : Accès à l'écran des bénéficiaires

Lorsqu'un utilisateur non authentifié demande l'écran des bénéficiaires, le système lui refuse l'accès et le redirige vers la page de connexion.

Source : RG-013-001

#### FR-013-02 : Liste des bénéficiaires actifs du client

L'écran affiche le titre d'onglet « BankApp Demo - Beneficiaires », puis la section « Mes beneficiaires enregistres », titrée « Beneficiaires ({nombre}) », avec un tableau aux colonnes « Libelle », « Titulaire », « IBAN », « BIC », « Banque », « Zone SEPA », « Action » et un bouton « Supprimer » par ligne. La liste ne contient que les bénéficiaires du client connecté qui sont actifs, triés par libellé alphabétique croissant. Si le client n'a aucun bénéficiaire actif, l'écran affiche « Aucun beneficiaire enregistre. Utilisez le formulaire ci-dessus pour en ajouter un. ».

Source : RG-013-003, RG-013-004, RG-013-005, RG-013-006

#### FR-013-03 : Affichage masqué de l'IBAN, BIC, banque et zone SEPA

Dans le tableau, l'IBAN est affiché sous la forme de ses 4 premiers caractères, suivis de « *** », suivis de ses 4 derniers caractères (par exemple « FR35***9010 »). Si le BIC n'est pas renseigné, la colonne « BIC » affiche « — » ; si la banque n'est pas renseignée, la colonne « Banque » affiche « — ». La colonne « Zone SEPA » affiche « ✓ SEPA » si le code pays de l'IBAN appartient à la zone SEPA, « Hors SEPA » sinon.

Source : RG-013-007, RG-013-008, RG-013-009, RG-013-010

#### FR-013-04 : Vérification de l'IBAN avant l'ajout

La section « Ajouter un beneficiaire » (titre « Nouveau beneficiaire SEPA ») propose, à côté du champ « IBAN * » (exemple « ex: FR76 3000 1007 9412 3456 7890 185 »), le bouton « Verifier l'IBAN ». Au clic, le système vérifie le seul champ IBAN, sans soumettre le formulaire d'ajout ni recharger la page, et affiche :
- « Veuillez saisir un IBAN. » si l'IBAN est vide ;
- « IBAN invalide (echec MOD-97). Verifiez la saisie. » si l'IBAN échoue au contrôle MOD-97 ;
- « IBAN valide — {code pays} (Zone SEPA) » si l'IBAN est valide et que son code pays appartient à la zone SEPA, « IBAN valide — {code pays} (Hors zone SEPA) » sinon.

Source : RG-013-011, RG-013-012, RG-013-013, RG-013-014

#### FR-013-05 : Contrôles à l'ajout d'un bénéficiaire

Le formulaire d'ajout comporte les champs « Libelle * », « Nom du titulaire * », « IBAN * », « BIC (optionnel) » (exemple « ex: BNPAFRPPXXX ») et « Banque (optionnel) » (exemple « ex: BNP Paribas »), et le bouton « Ajouter le beneficiaire ». À la soumission, le système rejette l'ajout et affiche :
- « Session expirée, veuillez vous reconnecter. » si la session ne porte plus de client ;
- « Le libellé est obligatoire. » si le libellé est vide ;
- « L'IBAN est obligatoire. » si l'IBAN est vide ;
- « IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97). » si l'IBAN échoue au contrôle MOD-97 ;
- « Le nom du titulaire est obligatoire. » si le nom du titulaire est vide ;
- « BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques. » si un BIC est saisi et ne respecte pas ce format (4 lettres de banque, 2 lettres de pays, 2 caractères alphanumériques de localisation, puis 3 caractères alphanumériques facultatifs).

Source : RG-013-015, RG-013-016, RG-013-017, RG-013-018, RG-013-019, RG-013-020

#### FR-013-06 : Longueur maximale des champs texte

Le système rejette l'ajout si le libellé, le nom du titulaire ou le nom de la banque dépasse 100 caractères ; aucun bénéficiaire n'est alors enregistré. La saisie de ces champs est limitée à 100 caractères.

Source : RG-013-028, RG-013-030, RG-013-032

#### FR-013-07 : Normalisation et enregistrement du bénéficiaire

À l'enregistrement, le système :
- normalise l'IBAN en supprimant tous les espaces et en le passant en majuscules ;
- normalise le BIC en supprimant les espaces de début et de fin et en le passant en majuscules, et l'enregistre vide s'il est blanc ;
- supprime les espaces de début et de fin du nom de la banque, et l'enregistre vide s'il est blanc ;
- calcule et enregistre l'appartenance à la zone SEPA à partir du code pays de l'IBAN normalisé ;
- enregistre la date du jour comme date de création ;
- enregistre le bénéficiaire comme actif.

Source : RG-013-021, RG-013-022, RG-013-023, RG-013-024, RG-013-025, RG-013-026

#### FR-013-08 : Ajout réussi

Lorsqu'un bénéficiaire est ajouté, le système affiche « Bénéficiaire ajouté avec succès. », recharge la liste des bénéficiaires et vide le formulaire d'ajout.

Source : RG-013-027

#### FR-013-09 : Suppression logique d'un bénéficiaire

Lorsque l'utilisateur clique sur « Supprimer », le système désactive le bénéficiaire sans l'effacer, puis rafraîchit aussitôt la liste affichée, sans recharger toute la page ; le bénéficiaire n'apparaît plus dans la liste (FR-013-02). La suppression n'est possible que pour un bénéficiaire du client connecté (FR-013-10).

Source : RG-013-033, RG-013-034

#### FR-013-10 : Contrôle d'appartenance avant suppression

Avant toute suppression, le système vérifie que le bénéficiaire appartient au client connecté. Sinon, la suppression est rejetée, le bénéficiaire reste actif, et le message « Erreur lors de la suppression. » est affiché [ASSUMPTION A-013-1 : le corpus ne fixe pas de message pour ce refus ; le message d'erreur de suppression existant est réutilisé, il ne révèle pas l'existence du bénéficiaire]. Remplace le comportement legacy RG-013-035 (aucune vérification d'appartenance).

Critère d'acceptation (test) : connecté en tant que client A, une demande de suppression portant sur l'identifiant d'un bénéficiaire du client B est refusée, et ce bénéficiaire reste actif et visible dans la liste de B.

Source : RG-013-035, BUG-047

**Notes :**
- RG-013-002 : couverte par l'exigence fusionnée de FEAT-010 (règles voisines) — accès pour tout utilisateur authentifié, quel que soit son rôle.
- RG-013-029 (dette legacy, Suspect) : non reproduite — information de l'utilisateur en cas de libellé de plus de 100 caractères ; bugs conservés BUG-045 et BUG-048.
- RG-013-031 (dette legacy, Suspect) : non reproduite — information de l'utilisateur en cas de nom du titulaire de plus de 100 caractères ; bugs conservés BUG-046 et BUG-049.
- Bug conservé BUG-050 : nom de banque de plus de 100 caractères (le rejet reste exigé par FR-013-06).
- En legacy, une erreur inattendue à l'ajout affichait « Erreur lors de l'ajout du bénéficiaire : {message} » avec le message technique brut ; ce détail n'est plus exposé (NFR-SEC-04).

### FEAT-014 — Callback OAuth Powens

**Description :** Point de retour appelé par le navigateur quand l'utilisateur quitte la page web de connexion de Powens, après avoir connecté (ou non) une banque depuis l'écran « Comptes Tiers » (FEAT-012). Il n'a pas d'écran propre ni de champ : il lit les paramètres reçus (`error`, `connection_id`, `code`), déclenche le traitement adapté, puis redirige toujours vers l'écran « Comptes Tiers », où s'affiche le résultat ou le message d'erreur. Il est accessible sans authentification ; les cas limites sont le retour en erreur, le retour incomplet, la session Powens expirée et la connexion réussie sans compte encore disponible. Tous les rôles sont concernés (CLIENT, CONSEILLER, ADMIN), puisque le traitement repose sur la session de l'utilisateur qui a lancé la connexion. Scénarios golden master : SC-014-01.

**Exigences fonctionnelles :**

#### FR-014-01 : Accès sans authentification

Le point de retour de Powens est accessible sans authentification. Quel que soit le cas traité, il répond par une redirection vers l'écran « Comptes Tiers », sans afficher de contenu propre ; appelé sans aucun paramètre, il répond par une redirection (statut 302).

Source : RG-014-001

#### FR-014-02 : Aiguillage par ordre de priorité

Les paramètres sont examinés dans cet ordre, et seul le premier cas qui s'applique est traité ; un paramètre vide compte comme absent :
1. `error` présent : la session Powens est remise à zéro (FR-014-07), le motif est affiché à l'utilisateur (FR-012-07, correction BUG-040), puis redirection vers « Comptes Tiers » ;
2. sinon, `connection_id` présent : le chargement des comptes est lancé avec cet identifiant (FR-014-05, FR-014-06), puis redirection ;
3. sinon, `code` présent : voir FR-014-04 ;
4. sinon : le système pose le message « Callback Powens incomplet. Parametres recus: [<liste des noms de paramètres reçus>] » (par exemple « Callback Powens incomplet. Parametres recus: [] »), puis redirection ; l'écran reste à l'état non connecté.

Source : RG-014-002, RG-014-003, RG-014-005

#### FR-014-03 : Redirection systématique

Après traitement, succès comme erreur, l'utilisateur est toujours redirigé vers l'écran « Comptes Tiers », qui affiche l'état et les messages issus du traitement.

Source : RG-014-002, RG-014-003, RG-014-005

#### FR-014-04 : Retour portant seulement un code

Le corpus décrit un parcours alternatif où, sans `error` ni `connection_id`, un `code` non vide déclenche le chargement des comptes avec ce code. Son usage réel par Powens n'est pas confirmé (question bloquante du corpus) ; tant qu'il ne l'est pas, la cible ne traite que `connection_id` et la branche « code » n'est pas reprise. [ASSUMPTION A-014-1 : d'ici la confirmation, un retour qui ne porte qu'un `code` est traité comme un retour incomplet (cas 4 de FR-014-02), avec le message « Callback Powens incomplet. Parametres recus: [code] »]

Source : RG-014-004

#### FR-014-05 : Session Powens expirée

Si le chargement des comptes est lancé alors que la session de l'utilisateur ne contient pas le jeton Powens, le chargement est abandonné et le système affiche, sur l'écran « Comptes Tiers », « Session expiree. Veuillez recommencer la connexion. ».

Source : RG-014-006

#### FR-014-06 : Connexion établie

Avec un jeton Powens valide en session : si Powens renvoie au moins un compte, la connexion est marquée établie, le premier compte est sélectionné automatiquement et ses transactions sont chargées ; si Powens ne renvoie aucun compte, la connexion est marquée établie et le système affiche « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. ». Une réponse en erreur de Powens n'est pas une liste vide : elle suit FR-012-14.

Source : RG-014-007, RG-014-008

#### FR-014-07 : Remise à zéro de la session Powens

La déconnexion Powens remet à zéro tout l'état Powens de la session : jeton d'accès, liste des comptes externes, compte sélectionné, liste des transactions et indicateur de connexion. Le message d'erreur est aussi effacé, comme pour la déconnexion de FR-012-15 [ASSUMPTION A-014-3 : la remise à zéro au retour de Powens efface aussi le message d'erreur, comme la déconnexion manuelle ; RG-014-009 ne le mentionne pas] ; dans le cas `error` de FR-014-02, le message du motif est posé après cette remise à zéro.

Source : RG-014-009

#### FR-014-08 : Vingt transactions au plus par compte externe

Au chargement des transactions d'un compte externe, au plus 20 transactions sont récupérées auprès de Powens. Les deux règles voisines donnent la même valeur (20) ; aucune différence n'est à arbitrer. La même limite figure dans FR-012-13.

Source : RG-014-011, RG-028-018

**Notes :**
- RG-014-010 (Suspect, dette legacy) : non reprise — le legacy attend 2 secondes de façon bloquante avant de lire les comptes, ce qui n'est pas une exigence métier ; le bug associé BUG-051 (pause bloquante au retour de Powens) est classé « ne pas corriger ». [ASSUMPTION A-014-2 : aucune attente de synchronisation n'est imposée avant la lecture des comptes ; une synchronisation non terminée donne le message de FR-014-06 et l'utilisateur rafraîchit (FR-012-16)]
- Les textes de la page de transit du legacy (« Connexion bancaire en cours... », « Connexion en cours... », « Recuperation de vos comptes via Powens Open Banking ») ne sont jamais vus, la redirection ayant lieu avant tout affichage ; ils ne sont pas repris.
- RG-014-007 et RG-028-014 laissent la connexion « établie » même sans compte ; RG-014-009 n'efface pas le message d'erreur alors que RG-012-024 et RG-028-023 l'effacent : FR-014-07 retient l'effacement, cohérent avec la déconnexion de FEAT-012.

### FEAT-015 — Espace Conseiller — Portefeuille clients

**Description :** L'espace conseiller est utilisé par les rôles CONSEILLER et ADMIN ; un CLIENT qui tente d'y accéder est renvoyé sans message vers son tableau de bord. Il présente le portefeuille des clients affectés à l'utilisateur connecté : un bandeau d'accueil, trois indicateurs (nombre de clients, encours total, prêts en attente), puis une carte par client avec ses soldes, comptes et prêts. Un panneau de détail s'ouvre sur la même page pour consulter les coordonnées, les comptes et les prêts d'un client. Cas limites marquants : portefeuille vide, client sans compte ou sans prêt, client inactif ; le portefeuille est chargé une seule fois par session (pas de rafraîchissement, comportement legacy conservé). Scénarios golden master : SC-015-01, SC-015-02.

**Exigences fonctionnelles :**

#### FR-015-01 : Accès à l'espace conseiller selon le rôle

Un utilisateur authentifié de rôle CONSEILLER ou ADMIN qui demande l'espace conseiller voit la page « Espace Conseiller » (portefeuille). Un utilisateur de rôle CLIENT qui la demande est redirigé vers son tableau de bord, sans aucun message d'erreur.

Source : RG-015-002, RG-015-003

#### FR-015-02 : Chargement du portefeuille du conseiller connecté

À la première ouverture de l'espace conseiller dans une session, le système charge la liste des clients dont le conseiller affecté est l'utilisateur connecté, et seulement ceux-là, triée par ordre alphabétique du nom complet. Le bandeau d'accueil affiche « Espace Conseiller » et « Bonjour {fullName}, vous gerez {N} client(s) », où {fullName} est le nom complet du conseiller connecté et {N} le nombre de clients du portefeuille.

Source : RG-015-004, RG-015-005

#### FR-015-03 : Indicateurs du portefeuille

Une fois le portefeuille chargé, le système affiche trois indicateurs : « Mes clients » (nombre de clients du portefeuille, sous-libellé « clients dans mon portefeuille »), « Encours total » (somme des soldes de tous les comptes de tous les clients du portefeuille, sous-libellé « solde cumulé mes clients ») et « Prets en attente » (nombre total de prêts au statut EN_ATTENTE de tous les clients du portefeuille, sous-libellé « a traiter »).

Source : RG-015-006, RG-015-007, RG-015-008

#### FR-015-04 : Portefeuille vide

Si le portefeuille du conseiller ne contient aucun client, le système affiche le message « Aucun client dans votre portefeuille. » suivi de « Contactez l administrateur pour l affectation des clients. », à la place de la liste des clients.

Source : RG-015-009

#### FR-015-05 : Carte client

Quand le portefeuille n'est pas vide, le système affiche une carte par client avec : la première lettre du nom complet en avatar, le nom complet, l'identifiant précédé de « @ », un badge de statut « Actif » ou « Inactif », et quatre statistiques : « Solde total » (somme des soldes de ses comptes), « Comptes » (nombre de comptes bancaires), « Prets actifs » (nombre de prêts EN_COURS) et « Prets en attente » (nombre de prêts EN_ATTENTE). La carte d'un client dont le statut actif est faux est visuellement distinguée comme inactive. Chaque carte porte un bouton « Voir detail ».

Source : RG-015-010, RG-015-011, RG-015-014

#### FR-015-06 : Indicateur de prêts sur la carte client

Si le client a au moins un prêt EN_ATTENTE, sa carte affiche l'avertissement « ⚠ {N} pret(s) en attente », où {N} est le nombre de prêts EN_ATTENTE. Sinon, s'il a au moins un prêt EN_COURS, la carte affiche l'indicateur positif « ✓ {N} pret(s) en cours », où {N} est le nombre de prêts EN_COURS. L'avertissement des prêts en attente l'emporte : les deux indicateurs ne s'affichent jamais ensemble.

Source : RG-015-012, RG-015-013

#### FR-015-07 : Panneau de détail d'un client

Quand l'utilisateur clique sur « Voir detail » d'une carte, le système affiche le panneau de détail du client sur la même page, sans changer d'écran. Le panneau affiche le nom complet, l'identifiant précédé de « @ », l'email et le téléphone ; une section « Comptes bancaires » (colonnes « Numero », « Type », « Solde ») ; une section « Prets » (colonnes « Type », « Montant », « Mensualite », « Statut »). Le bouton « Fermer » masque le panneau et revient à la liste du portefeuille, sans changer d'écran.

Source : RG-015-015, RG-015-016, RG-015-017, RG-015-018, RG-015-021

#### FR-015-08 : Client sans compte ou sans prêt

Dans le panneau de détail, si le client sélectionné n'a aucun compte bancaire, la section des comptes affiche « Aucun compte ». S'il n'a aucun prêt, la section des prêts affiche « Aucun pret ».

Source : RG-015-019, RG-015-020

#### FR-015-09 : Valeurs de référence affichées

Les types de compte gérés sont COURANT et EPARGNE. Les types de prêt gérés sont IMMOBILIER, CONSOMMATION et TRAVAUX. Les statuts de prêt gérés sont EN_ATTENTE, EN_COURS, REMBOURSE, DEFAUT et REFUSE. Ces valeurs sont affichées telles quelles (codes, sans libellé traduit) ; les statuts EN_ATTENTE, EN_COURS, REFUSE et REMBOURSE sont affichés en badge, et le statut DEFAUT est affiché sans badge (comportement legacy conservé, voir Notes).

Source : RG-015-022, RG-015-023, RG-015-024

**Notes :**
- RG-015-001 : couverte par l'exigence fusionnée de FEAT-008 (règles voisines).
- RG-015-025 (Suspect, dette legacy) : non reproduite. Le rafraîchissement du portefeuille en cours de session n'est pas offert ; le portefeuille reste chargé une fois par session (FR-015-02). Bug conservé : BUG-052 (bouton de rafraîchissement jamais affiché, conservé tel quel lors de la rénovation).
- RG-015-026 (Suspect, dette legacy) : non reproduite comme exigence. Le statut DEFAUT reste affiché sans badge distinct (FR-015-09). Bug conservé : BUG-053 (statut de prêt « Défaut » sans mise en forme distincte).
- Le libellé de rôle « Conseiller » et les liens de menu « Mon portefeuille » et « Conseiller IA » figurent dans l'en-tête de l'écran (corpus MESSAGES.md), la déconnexion relève de l'exigence fusionnée de FEAT-001.

### FEAT-016 — Administration — Gestion globale

**Description :** La page d'administration est réservée au rôle ADMIN ; les rôles CONSEILLER et CLIENT sont renvoyés vers leur tableau de bord. Elle regroupe sur un seul écran : des indicateurs globaux, le pipeline des prêts en attente (approbation ou refus), la gestion des clients (recherche, détail, activation et désactivation), l'historique de tous les prêts, la liste de tous les utilisateurs, la revue des alertes fraude, le journal des traitements périodiques et le catalogue des produits bancaires. Chaque action administrative est tracée dans un journal d'audit qui survit à l'échec de l'action. Cas limites marquants : pipeline vide, recherche sans résultat, action sur un prêt qui n'est plus en attente, alerte déjà traitée. Scénarios golden master : SC-016-01, SC-016-02.

**Exigences fonctionnelles :**

#### FR-016-01 : Accès à l'administration réservé au rôle ADMIN

Un utilisateur de rôle CONSEILLER ou CLIENT qui demande la page d'administration est redirigé vers son tableau de bord, sans que la page d'administration soit affichée. Un utilisateur de rôle ADMIN accède à toutes les sections de la page d'administration.

Source : RG-016-002, RG-016-003

#### FR-016-02 : Indicateurs de l'administration

À l'ouverture de la page d'administration, le système affiche : « Total clients » (nombre de personnes de rôle CLIENT) avec le sous-libellé « {n} actifs » (nombre de ces clients actifs) ; « Conseillers » (nombre de personnes de rôle CONSEILLER, sous-libellé « actifs ») ; « Prets en attente » (nombre de prêts au statut EN_ATTENTE) avec le sous-libellé « {montant} EUR a traiter » (montant cumulé de ces prêts en euros) ; « Total utilisateurs » (sous-libellé « tous roles confondus »).

Source : RG-016-004, RG-016-005, RG-016-006, RG-016-007

#### FR-016-03 : Pipeline des prêts vide

La section « Pipeline Prets — En attente d approbation » liste les prêts au statut EN_ATTENTE (colonnes « ID », « Client », « Type », « Montant », « Duree », « Taux », « Mensualite », « Motif », « Actions »). Quand aucun prêt n'a le statut EN_ATTENTE, elle affiche « Aucun pret en attente ».

Source : RG-016-008

#### FR-016-04 : Approbation d'un prêt

Si un prêt a le statut EN_ATTENTE et que l'administrateur clique sur « Approuver », le système passe le prêt au statut EN_COURS, génère l'échéancier complet de remboursement et affiche « Prêt approuvé ». Sauf si le prêt n'a plus le statut EN_ATTENTE : la demande est alors ignorée, sans erreur ni changement.

Source : RG-016-009, RG-016-011

#### FR-016-05 : Refus d'un prêt

Si un prêt a le statut EN_ATTENTE et que l'administrateur clique sur « Refuser », le système passe le prêt au statut REFUSE et affiche « Prêt refusé ». Le refus est immédiat, sans demande de confirmation. Sauf si le prêt n'a plus le statut EN_ATTENTE : la demande est alors ignorée, sans erreur ni changement.

Source : RG-016-010, RG-016-011

#### FR-016-06 : Journal d'audit des actions administratives

Chaque action administrative est enregistrée dans le journal d'audit avec son action et l'identifiant concerné : « ADMIN_LOAN_APPROVE » et l'identifiant du prêt pour une approbation, « ADMIN_LOAN_REFUSE » et l'identifiant du prêt pour un refus, « ADMIN_DEACTIVATE » et l'identifiant du client pour une désactivation, « ADMIN_ACTIVATE » et l'identifiant du client pour une activation. L'entrée d'audit est enregistrée indépendamment de l'action : elle est conservée même si l'action principale échoue et est annulée.

Source : RG-016-012, RG-016-013, RG-016-023, RG-016-024, RG-016-038

#### FR-016-07 : Erreur lors de l'approbation ou du refus d'un prêt

Si une erreur survient pendant l'approbation ou le refus d'un prêt, le système affiche à l'administrateur une notification d'erreur compréhensible, sans aucun détail technique interne (message d'exception, nom de composant, trace). Ce comportement remplace le comportement legacy RG-016-014, qui affichait « Erreur : » suivi du message technique brut. [ASSUMPTION A-016-1 : le libellé exact de la notification n'est pas donné par le corpus ; il est fixé à « Erreur lors du traitement du prêt. Veuillez réessayer. »]

Critère d'acceptation (test) : en provoquant une erreur pendant l'approbation puis pendant le refus d'un prêt EN_ATTENTE, la notification affichée est le message métier défini, ne contient ni texte d'exception ni nom technique, et le prêt garde son statut EN_ATTENTE.

Source : RG-016-014, BUG-054

#### FR-016-08 : Liste et recherche des clients

La section « Gestion des clients » n'affiche que les personnes de rôle CLIENT (jamais CONSEILLER ni ADMIN), avec les colonnes « Nom », « Login », « Email », « Role », « Statut » (« Actif » ou « Inactif »), « Conseiller » et « Actions ». Quand le champ de recherche (texte indicatif « Rechercher par nom, login, email... ») est vide, tous les clients sont affichés. Quand l'administrateur saisit un terme et clique sur « Filtrer », seuls les clients dont le nom complet, l'identifiant ou l'email contient ce terme sont affichés (correspondance partielle, sans distinction de casse). Si la liste filtrée est vide, le système affiche « Aucun client trouve ».

Source : RG-016-015, RG-016-016, RG-016-017, RG-016-018

#### FR-016-09 : Panneau de détail d'un client

Quand l'administrateur clique sur « Detail » sur une ligne client, le système affiche sur la même page le panneau « Detail : {nom complet} », en lecture seule, avec : « Nom complet », « Identifiant », « Email », « Telephone », « Role », « Statut », « Conseiller assigne » (seulement si un conseiller est affecté) et « Client depuis » (date au format jj/MM/aaaa, voir FR-016-10). Le bouton « Fermer » ferme le panneau.

Source : RG-016-019, RG-016-020

#### FR-016-10 : Date « Client depuis »

Sous le libellé « Client depuis », le système affiche la date à laquelle la personne est devenue cliente de la banque, et non la date d'affectation de son conseiller ; les deux dates peuvent différer. Ce comportement remplace le comportement legacy RG-016-025, qui affichait la date d'affectation du conseiller et seulement si un conseiller était affecté. [ASSUMPTION A-016-2 : la date d'entrée en relation est la date de création de la fiche client ; le champ est affiché pour tout client, qu'il ait ou non un conseiller.]

Critère d'acceptation (test) : pour un client créé le 01/02/2024 et affecté à un conseiller le 15/06/2025, le panneau de détail affiche « Client depuis » 01/02/2024 ; pour un client sans conseiller, le champ « Client depuis » est affiché avec sa date d'entrée en relation.

Source : RG-016-025, BUG-055

#### FR-016-11 : Activation et désactivation d'un client

Pour un client actif, l'administrateur dispose du bouton « Desactiver » : le clic rend le client inactif et affiche « Client désactivé ». Pour un client inactif, il dispose du bouton « Activer » : le clic rend le client actif et affiche « Client activé ». Chaque action est tracée selon FR-016-06.

Source : RG-016-021, RG-016-022

#### FR-016-12 : Historique de tous les prêts

La section « Historique de tous les prets », en lecture seule, affiche tous les prêts quel que soit leur statut, triés par date de début décroissante (colonnes « ID », « Client », « Type », « Montant », « Statut », « Duree », « Mensualite », « Date debut »). Si aucun prêt n'existe, elle affiche « Aucun pret en base ».

Source : RG-016-026

#### FR-016-13 : Tous les utilisateurs du système

La section « Tous les utilisateurs du systeme », en lecture seule, affiche tous les utilisateurs de tous les rôles (CLIENT, CONSEILLER, ADMIN), avec les colonnes « Nom », « Login », « Email », « Role », « Statut ».

Source : RG-016-027

#### FR-016-14 : Tableau des alertes fraude et actions selon le statut

La section « Alertes Fraude — Moteur de détection » liste les alertes (colonnes « ID », « Client », « Règle déclenchée », « Score risque » au format « {score}/100 », « Statut », « Description », « Date », « Actions »). Pour une alerte au statut OPEN, les boutons « Confirmer » et « Faux positif » sont affichés ; pour une alerte d'un autre statut, le texte « Traité par {reviewedBy} » est affiché à la place des boutons. Le badge « {n} ouverte(s) » n'est affiché que si le nombre d'alertes OPEN est strictement supérieur à 0. Si aucune alerte n'existe, la section affiche « Aucune alerte fraude enregistrée ».

Source : RG-016-028, RG-016-029, RG-016-033

#### FR-016-15 : Revue d'une alerte fraude

Quand l'administrateur clique sur « Confirmer » sur une alerte OPEN, le système passe l'alerte au statut CONFIRMED ; sur « Faux positif », il la passe au statut FALSE_POSITIVE. Dans les deux cas, il enregistre le nom complet de l'administrateur connecté comme auteur de la revue et l'horodatage de la revue. Une demande de confirmation ou de classement sans identifiant d'alerte est ignorée sans erreur.

Source : RG-016-030, RG-016-031, RG-016-032

#### FR-016-16 : Affichage de toutes les alertes fraude

Sans filtre de statut, le tableau des alertes fraude affiche toutes les alertes, quel que soit leur statut (OPEN, CONFIRMED, FALSE_POSITIVE). Ce comportement remplace le comportement legacy RG-016-034, qui n'affichait que les alertes OPEN.

Critère d'acceptation (test) : avec une alerte OPEN, une CONFIRMED et une FALSE_POSITIVE en base, le tableau affiche les trois lignes ; les deux alertes traitées affichent « Traité par {reviewedBy} » et le badge affiche « 1 ouverte(s) ».

Source : RG-016-034, BUG-056

#### FR-016-17 : Journal des traitements périodiques

La section « Traitements nocturnes », en lecture seule, affiche les 50 entrées les plus récentes du journal d'exécution des traitements (colonnes « Job », « Debut », « Fin », « Duree (ms) », « Enregistrements », « Statut », « Details »). Si aucune entrée n'existe, elle affiche « Aucun traitement nocturne enregistre ».

Source : RG-016-035, RG-016-036

#### FR-016-18 : Catalogue des produits bancaires

La section « Catalogue produits bancaires », en lecture seule, affiche les produits bancaires actifs regroupés par catégorie : « Produits d epargne », « Produits de credit » et « Produits d investissement », avec un lien « Voir la page Produits » vers la page complète des produits. Une catégorie sans produit affiche « Aucun produit ».

Source : RG-016-037

**Notes :**
- RG-016-001 : couverte par l'exigence fusionnée de FEAT-008 (règles voisines).
- FR-016-01 est voisine de l'exigence fusionnée de FEAT-001 (RG-001-011 + RG-030-003, non-ADMIN sur l'administration) ; RG-016-002 ne figure pas dans la liste des fusions, elle reste portée ici. Les deux exigences doivent rester cohérentes (redirection vers le tableau de bord, sans message).
- Le bandeau legacy « Espace Administration — JEE 8 / JSF 2.3 / EJB 3.2 / RBAC custom — Role : ADMIN » cite des technologies : il n'est pas repris tel quel ; le titre d'écran et le badge « ADMIN » restent.
- Le comportement legacy de la date « Client depuis » (RG-016-025) n'est pas reproduit : FR-016-10 le remplace. L'énoncé de BUG-055 parle de la « fiche client du conseiller », mais la règle concernée porte sur le panneau de détail de l'administration ; le panneau de détail de l'espace conseiller (FEAT-015) n'affiche pas cette date.
- Après la correction de BUG-072 (FEAT-022), le traitement de surveillance des soldes faibles, exécuté toutes les 30 minutes, alimente le journal affiché en FR-016-17 : 48 entrées par jour peuvent repousser les traitements nocturnes hors des 50 entrées affichées.

### FEAT-017 — Téléchargement documents PDF

**Description :** Fonction de téléchargement appelée depuis la page documents (FEAT-008) par tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN). Selon le type demandé (RELEVE, RIB, ATTESTATION, IFU), elle génère un PDF et le renvoie en pièce jointe avec un nom de fichier normalisé. Elle contrôle elle-même la session, valide le type et l'identifiant de compte, refuse les comptes d'autres clients et masque les détails internes en cas d'échec de génération. Scénarios golden master : SC-017-01, SC-017-02.

**Exigences fonctionnelles :**

#### FR-017-01 : Session obligatoire pour télécharger

Si une demande de téléchargement arrive sans session authentifiée valide, le système redirige vers la page de connexion. Ce contrôle est effectué par la fonction de téléchargement elle-même, indépendamment du contrôle d'accès général des écrans.

Source : RG-017-001, RG-017-002

#### FR-017-02 : Type de document

Si le type de document est absent, le système rejette la demande avec un code 400 et le message "Parametre type manquant". Si le type ne correspond à aucune des valeurs RELEVE, RIB, ATTESTATION ou IFU, comparées sans tenir compte de la casse, le système rejette la demande avec un code 400 et le message "Type inconnu : {type}".

Source : RG-017-003, RG-017-004

#### FR-017-03 : Identifiant de compte

Si le type est RELEVE, RIB ou ATTESTATION et que l'identifiant de compte est absent, vide ou non numérique, le système rejette la demande avec un code 400 et le message "accountId manquant".

Source : RG-017-005

#### FR-017-04 : PDF du relevé de compte

Pour le type RELEVE, le système génère un PDF qui contient toutes les opérations du compte pour l'année et le mois demandés, triées par date d'opération croissante, et le renvoie en pièce jointe nommée `releve_{accountId}_{année}_{mois sur 2 chiffres}.pdf`. Le PDF est titré "Demo Bank — Releve de compte", porte les lignes "Compte", "Titulaire", "Periode", "Solde actuel" (montant au format "{montant} EUR"), et un tableau aux colonnes "Date", "Description", "Type", "Montant", "Solde apres" (type "CREDIT" ou "DEBIT", montants "+{montant} EUR" ou "-{montant} EUR") ; sans opération sur la période, il affiche "Aucune operation sur cette periode.".

Source : RG-017-007

#### FR-017-05 : PDF du RIB

Pour le type RIB, le système génère un PDF titré "Demo Bank — Releve d Identite Bancaire (RIB)", avec le nom complet du titulaire ("Titulaire"), "Banque" : "Demo Bank SA", "Code banque" : "30004", "Code guichet" : "00001", "Numero de compte", "Cle RIB" : "42", "IBAN", "BIC / SWIFT" : "DEMOFR21XXX", "Domiciliation" : "Demo Bank - Agence Paris Centre", et la note "Document genere le {jj/mm/aaaa} - Demo Bank SA, 1 Place Vendome, 75001 Paris". Il le renvoie en pièce jointe nommée `rib_{accountId}.pdf`.

Source : RG-017-008

#### FR-017-06 : PDF de l'attestation de solde

Pour le type ATTESTATION, le système génère un PDF titré "Demo Bank — Attestation de Solde" qui atteste le solde courant du compte à la date de génération, avec les textes "Demo Bank SA, dont le siege social est situe 1 Place Vendome, 75001 Paris, atteste par la presente que :", "M. / Mme {nom complet}", "est titulaire du compte n° {numéro de compte}", "Le solde de ce compte, arrete au {jj mois aaaa}, est de :", "{montant} EUR", "Cette attestation est delivree a la demande de l interesse(e) pour faire valoir ce que de droit." et "Fait a Paris, le {jj mois aaaa}". Il le renvoie en pièce jointe nommée `attestation_solde_{accountId}.pdf`.

Source : RG-017-009

#### FR-017-07 : PDF de l'IFU

Pour le type IFU, le système génère l'Imprimé Fiscal Unique du client de la session authentifiée (jamais d'un client désigné par la demande) pour l'année demandée : il totalise les opérations au crédit dont la description contient "interet" et applique un Prélèvement Forfaitaire Unique de 30 %, arrondi au demi supérieur à 2 décimales. Le PDF est titré "Demo Bank — Imprime Fiscal Unique (IFU) {année}", porte "Etablissement" : "Demo Bank SA", "SIREN" : "123 456 789 00012", "Titulaire", "Adresse" : "1 rue de la Paix, 75001 Paris", "Annee fiscale", un tableau "Nature des revenus" / "Montant brut" avec les lignes "Interets et produits de placement" et "Prelevement Forfaitaire Unique (30%)", la note "Ce document est fourni a titre informatif. Il recense les revenus de capitaux mobiliers soumis au PFU pour l annee {année}." et "Genere le {jj/mm/aaaa}". Il est renvoyé en pièce jointe nommée `ifu_{customerId}_{année}.pdf`.

Source : RG-017-010

#### FR-017-08 : Refus d'un compte appartenant à un autre client

Si le client de la session demande un document pour un compte qui ne lui appartient pas, le système refuse la demande et ne génère aucun document. Ce contrôle remplace le comportement legacy RG-017-011 (aucune vérification d'appartenance). Il s'applique après les contrôles de FR-017-02 et FR-017-03 ; un compte inexistant relève de FR-017-09. [ASSUMPTION A-017-1 : le refus est signalé comme un accès interdit (HTTP 403), sans message métier défini par le corpus ; même choix que FEAT-008.]

Critère d'acceptation (test) : connecté en tant que client A, une demande RELEVE, RIB ou ATTESTATION sur un compte existant du client B ne renvoie aucun PDF et est refusée ; la même demande sur un compte du client A renvoie un PDF.

Source : RG-017-011, BUG-058

#### FR-017-09 : Compte ou client introuvable

Si le compte ou le client visé n'existe pas, le système rejette la demande avec un code 404 et le message "Compte introuvable" (compte) ou "Client introuvable" (client, type IFU).

Source : RG-017-012

#### FR-017-10 : Échec de génération sans détail interne

Si une erreur inattendue survient pendant la génération du PDF, le système répond par un code 500 avec le seul message "Erreur lors de la generation du document", sans aucun détail technique interne (message d'exception, trace, nom de composant). Ce comportement remplace le comportement legacy RG-017-013.

Critère d'acceptation (test) : en provoquant une erreur de génération, la réponse a le code 500, son corps vaut exactement "Erreur lors de la generation du document" et ne contient ni message d'exception ni trace.

Source : RG-017-013, BUG-059

#### FR-017-11 : Format de la réponse PDF

Pour tout document généré, le système renvoie le type de contenu `application/pdf`, la disposition `attachment; filename="{nom de fichier}"` et le contrôle de cache `no-cache, no-store, must-revalidate`. Tous les PDF portent le pied de page "Demo Bank SA — Capital 500 000 000 EUR — RCS Paris 123 456 789 00012 — Établissement de crédit agréé par l'ACPR".

Source : RG-017-014

**Notes :**
- RG-017-006 (dette legacy) : non reproduite. Bugs conservés associés (année ou mois absent, non numérique ou hors plage) : BUG-057, BUG-062, BUG-063.
- RG-017-015 (dette legacy) : non reproduite. Bug conservé associé : BUG-060 (SIRET écrit en dur).
- BUG-061 (conservé) : le legacy renvoie une erreur 500 au lieu de 404 pour un compte inexistant ; FR-017-09 reprend la règle (404), qui fait foi.
- Écart corpus : en exécution, le legacy renvoie une erreur 500 pour tout RELEVE, RIB ou ATTESTATION (défaut de chargement des données) ; seul l'IFU fonctionne. Les FR décrivent le comportement des règles.
- Recouvrement avec FEAT-008 (aucune fusion décidée) : FR-017-01 et FR-008-02, FR-017-02/03/09 et FR-008-11, FR-017-08 et FR-008-04 décrivent les mêmes contrôles et doivent rester identiques.
- Les libellés de la page documents sont portés par FEAT-008 (FR-008-05, FR-008-06).

### FEAT-018 — Calcul des intérêts nocturnes

**Description :** Traitement automatique de nuit, sans écran propre, qui crédite les intérêts des comptes d'épargne (type EPARGNE) au taux annuel de 0,5 %, soit 0,5 % / 12 par mois, une seule fois par mois. Chaque crédit crée une opération au crédit visible dans le relevé du client. Chaque exécution est journalisée (succès ou échec) sous le nom « INTEREST_CALCULATION », et les ADMIN consultent ce journal dans la section des traitements nocturnes de l'écran d'administration. Cas limites marquants : intérêt nul ou négatif (compte ignoré), erreur pendant le traitement (exécution journalisée en échec), le legacy créditait chaque nuit le taux mensuel (environ 30 fois trop d'intérêts), corrigé ici. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-018-01 : Déclenchement mensuel du crédit d'intérêts

Le traitement de calcul des intérêts, nommé « INTEREST_CALCULATION », se déclenche automatiquement à 2 h du matin. Il applique le taux mensuel de 0,5 % / 12 au solde de chaque compte EPARGNE une seule fois par mois civil, et non à chaque nuit [ASSUMPTION A-018-1 : le crédit a lieu lors de l'exécution de 2 h du matin du premier jour de chaque mois ; les autres nuits, le traitement ne crédite rien] [ASSUMPTION A-018-2 : si le traitement est relancé dans un mois où les intérêts ont déjà été crédités, aucun compte n'est crédité une seconde fois]. Cette règle remplace le comportement legacy RG-018-003, où le taux mensuel était appliqué chaque nuit par un déclenchement quotidien (RG-018-001).

Critère d'acceptation (test) : pour un compte EPARGNE de solde 12 000,00 EUR, après une simulation de toutes les exécutions nocturnes d'un mois civil complet, le compte a reçu exactement une opération « Interets mensuels livret epargne (batch nocturne) » de 5,00 EUR (12 000 × 0,005 / 12), et son solde vaut 12 005,00 EUR.

Source : RG-018-001, RG-018-003, BUG-064

#### FR-018-02 : Comptes concernés et calcul de l'intérêt

Le traitement sélectionne tous les comptes de type EPARGNE. L'intérêt de chaque compte est arrondi à 2 décimales, au plus proche, la moitié étant arrondie vers le haut. Si l'intérêt calculé est nul ou négatif, le compte est ignoré : son solde n'est pas modifié et aucune opération n'est créée.

Source : RG-018-002, RG-018-004, RG-018-005

#### FR-018-03 : Crédit du compte et opération d'intérêts

Quand des intérêts sont crédités sur un compte EPARGNE, son solde augmente du montant des intérêts, et une opération au crédit est enregistrée avec ce montant, le solde résultant du compte, la date et l'heure du crédit, et le libellé « Interets mensuels livret epargne (batch nocturne) ».

Source : RG-018-006, RG-018-007

#### FR-018-04 : Journal d'exécution du traitement

Quand le traitement se termine sans erreur, il enregistre une entrée de journal des traitements avec le nom « INTEREST_CALCULATION », le statut « SUCCESS », l'heure de début, l'heure de fin, le nombre de comptes EPARGNE évalués (y compris ceux ignorés) et le détail « Interets calcules sur {N} comptes epargne ». Si une erreur survient pendant le traitement, il enregistre une entrée avec le nom « INTEREST_CALCULATION », le statut « FAILED » et le détail « Erreur: {message} », et journalise l'erreur au niveau ERROR dans les journaux techniques.

Source : RG-018-008, RG-018-009

#### FR-018-05 : Consultation des exécutions par l'ADMIN

Un utilisateur ADMIN voit, dans la section des traitements nocturnes de l'écran d'administration, les exécutions récentes des traitements, dont celles de « INTEREST_CALCULATION », triées par heure de début décroissante, avec les colonnes « Job », « Debut », « Fin », « Duree (ms) », « Enregistrements », « Statut » et « Details », et un bouton « Rafraichir ». S'il n'y a aucune exécution, la section affiche « Aucun traitement nocturne enregistre ».

Source : RG-018-010

**Notes :**
- RG-018-003 (Suspect, dette legacy) : remplacée par FR-018-01 (correction BUG-064).
- Le titre legacy de la section d'administration contient une mention technique ; il est affiché « Traitements nocturnes » [ASSUMPTION A-018-3 : titre sans le suffixe technique].
- Le libellé « Interets mensuels livret epargne (batch nocturne) » est conservé à l'identique : il apparaît dans le relevé du client.

### FEAT-019 — Détection fraude nocturne par batch

**Description :** Traitement planifié, sans écran ni rôle utilisateur, qui s'exécute chaque nuit à 02:30. Il compte les transactions (débits et crédits) de 10 000 € ou plus des 24 heures précédentes et inscrit le résultat dans le journal d'exécution des traitements, consultable par l'ADMIN dans l'administration (FEAT-016). Il ne crée aucune alerte de fraude : c'est un simple compte rendu. Cas limites marquants : montant exactement égal à 10 000 € (compté), changement d'heure saisonnier (fenêtre désormais toujours de 24 heures exactes), redémarrage du serveur après 02:30 (pas de rattrapage), erreur pendant le traitement (entrée FAILED). Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-019-01 : Exécution quotidienne à 02:30, sans rattrapage

Le système exécute le traitement de détection de fraude nocturne chaque jour à 02:30, heure du fuseau configuré de l'application (NFR-BATCH-01, NFR-BATCH-03). Si le serveur redémarre après 02:30, le traitement n'est pas relancé pour ce jour-là. [ASSUMPTION A-019-1 : le traitement s'exécute une fois et une seule par jour calendaire, y compris les jours de changement d'heure, où 02:30 n'existe pas ou existe deux fois]

Source : RG-019-001, RG-019-002

#### FR-019-02 : Transactions retenues

Le traitement retient toutes les transactions dont le montant est supérieur ou égal à 10 000 € (borne incluse) et dont la date est dans les 24 heures qui précèdent son exécution, débits et crédits confondus, sans filtrer sur le type.

Source : RG-019-003, RG-019-004

#### FR-019-03 : Fenêtre de 24 heures exactes

Le traitement calcule le début de sa fenêtre à partir d'une référence à décalage fixe, de sorte que la fenêtre dure toujours exactement 24 heures, y compris lors des changements d'heure saisonniers. Cette exigence remplace le comportement legacy RG-019-009, où la fenêtre suivait le fuseau par défaut du serveur et pouvait durer 23 ou 25 heures.

Critère d'acceptation (test) : pour une exécution le jour du passage à l'heure d'été et pour une exécution le jour du passage à l'heure d'hiver, la fenêtre de recherche couvre exactement 24 heures écoulées avant l'heure d'exécution ; une transaction de 10 000 € datée de 24 heures et 30 minutes avant l'exécution n'est pas comptée, une transaction datée de 23 heures et 30 minutes avant l'est.

Source : RG-019-009, BUG-066

#### FR-019-04 : Une entrée de journal par exécution

À la fin de chaque exécution, réussie ou non, le système écrit exactement une entrée dans le journal d'exécution des traitements, avec le nom de traitement « FRAUD_DETECTION », le nombre de transactions retenues comme nombre d'enregistrements traités, et le statut « SUCCESS » ou « FAILED ». En cas de succès, le détail est « Transactions suspectes detectees: {N} transaction(s) > 10000 EUR », où N est le nombre de transactions retenues par FR-019-02.

Source : RG-019-006

#### FR-019-05 : Aucune alerte de fraude créée

Le traitement nocturne n'enregistre aucune alerte de fraude individuelle : il ne produit que le compte rendu de FR-019-04. Les alertes de fraude ne sont créées que par le contrôle en temps réel (FEAT-027).

Source : RG-019-007

#### FR-019-06 : Erreur pendant le traitement

Si une erreur survient pendant le traitement, le système l'intercepte, écrit l'entrée de journal de FR-019-04 avec le statut « FAILED » et le détail « Erreur: {message} », journalise l'erreur, et ne la propage pas : les autres traitements et l'exécution du lendemain ne sont pas affectés.

Source : RG-019-008

**Notes :**
- RG-019-009 (Suspect, dette legacy) est couverte par la FR de correction FR-019-03 (BUG-066) ; son comportement legacy n'est pas reproduit.
- RG-019-005 (dette legacy) : non reproduite. La borne inclusive de 10 000 € qu'elle décrit est portée par FR-019-02 (RG-019-003) ; la dette porte sur le texte du détail, qui écrit « > 10000 EUR ». BUG-065 (conservé, non corrigé) : ce texte est repris tel quel dans FR-019-04, bien que la borne soit inclusive.

### FEAT-020 — Relevés mensuels — batch stub

**Description :** Traitement planifié, sans écran ni rôle utilisateur, déclenché par le système le 1er de chaque mois à 03h00. Dans le legacy conservé, il compte les clients actifs de rôle CLIENT et enregistre une exécution dans l'historique des traitements, visible dans le catalogue des produits (FEAT-011) ; il ne génère aucun document. Les relevés restent disponibles à la demande depuis la page documents (FEAT-008, FEAT-017). Une exécution manquée pendant un arrêt de l'application n'est pas rattrapée. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-020-01 : Déclenchement mensuel

Le 1er de chaque mois à 03h00, le système déclenche le traitement des relevés mensuels.

Source : RG-020-001

#### FR-020-02 : Journal d'une exécution réussie

Quand le traitement se termine sans erreur, le système enregistre une exécution avec le nom de traitement "MONTHLY_STATEMENTS", le nombre de clients actifs de rôle CLIENT comme nombre d'enregistrements traités, le statut "SUCCESS" et le détail "Releves mensuels generes pour {N} clients".

Source : RG-020-003

#### FR-020-03 : Journal d'une exécution en échec

Si une erreur survient pendant le traitement, le système enregistre une exécution avec le nom de traitement "MONTHLY_STATEMENTS", zéro enregistrement traité, le statut "FAILED" et le détail "Erreur: {message de l'erreur}".

Source : RG-020-004

#### FR-020-04 : Pas de rattrapage des exécutions manquées

Le système ne rejoue pas les exécutions du traitement manquées pendant que l'application était arrêtée.

Source : RG-020-005

**Notes :**
- RG-020-002 (dette legacy) : non reproduite. Bug conservé associé : BUG-067 (le traitement ne génère aucun relevé ; son détail "Releves mensuels generes pour {N} clients" est trompeur). Le comportement conservé est décrit par FR-020-02.
- Le détail d'échec de FR-020-03 reprend le message brut de l'erreur, visible dans la colonne "Details" de l'historique (FEAT-011) ; comportement legacy conservé tel quel.

### FEAT-021 — Exécution des virements programmés

**Description :** Traitement automatique sans écran, qui exécute les virements programmés créés par les clients (CLIENT) dans l'écran des virements programmés (FEAT-004). Toutes les 5 minutes, il sélectionne les virements actifs arrivés à échéance, débite le compte source, crédite le compte bénéficiaire interne, enregistre les opérations visibles au relevé (FEAT-005) et au tableau de bord, puis fait avancer la date de prochaine exécution selon la fréquence. Les cas limites marquants sont le solde insuffisant (virement retenté au passage suivant), le compte bénéficiaire absent de la banque (désormais refusé avant tout débit), la dérive du calendrier (désormais calculé depuis la date prévue) et l'échec d'un virement, qui ne doit pas affecter les autres. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-021-01 : Déclenchement périodique

Le système déclenche l'exécution des virements programmés dus toutes les 5 minutes, à toute heure, et journalise « Verification virements programmes dus » à chaque passage. Le déclenchement ne conserve pas d'état propre : après un redémarrage du serveur, il reprend au prochain cycle de 5 minutes, et les virements arrivés à échéance pendant l'arrêt sont exécutés à cette reprise (FR-021-02), soit au plus 5 minutes de retard après le démarrage.

Source : RG-021-001, RG-021-002

#### FR-021-02 : Sélection des virements dus

À chaque passage, le système sélectionne tous les virements programmés actifs dont la date de prochaine exécution est antérieure ou égale à l'instant courant, triés par date de prochaine exécution croissante, et les traite l'un après l'autre. Les virements inactifs (suspendus) ne sont pas sélectionnés.

Source : RG-021-003

#### FR-021-03 : Débit du compte source et crédit du compte bénéficiaire

Pour un virement dû dont le solde du compte source est suffisant, le système débite le compte source du montant du virement et crédite du même montant le compte de la banque dont le numéro correspond au compte bénéficiaire, sauf si le compte bénéficiaire n'est pas un compte de la banque (FR-021-04, aucun débit) ou si le solde est insuffisant (FR-004-12, aucun débit, virement retenté au passage suivant). Le système journalise « Virement programme execute id={id} montant={montant} ».

Source : RG-021-005, RG-021-006

#### FR-021-04 : Compte bénéficiaire hors de la banque refusé avant tout débit

Si le numéro de compte bénéficiaire ne correspond à aucun compte de la banque, le système rejette le virement avant tout débit : aucun débit, aucune opération enregistrée, et le client est informé par une notification dans l'application, avec un message d'erreur clair [ASSUMPTION A-021-1 : texte proposé « Virement programme vers {libellé bénéficiaire} refuse : compte destinataire introuvable », non fixé par le corpus]. Ce contrôle précède celui du solde (FR-004-12) [ASSUMPTION A-021-2 : le corpus ne donne pas l'ordre ; le refus définitif passe avant le report pour solde insuffisant]. Comme la création vers un tel compte est déjà refusée (FR-004-13), ce cas ne concerne que les virements existants dans les données de référence ou dont le compte bénéficiaire a disparu ; le virement est alors suspendu (état inactif), pour ne pas être refusé et notifié à chaque passage [ASSUMPTION A-021-3 : suspension du virement après le refus ; le corpus ne dit pas ce que devient le virement]. Remplace le comportement legacy RG-021-007 (compte source débité sans crédit nulle part).

Critère d'acceptation (test) : avec un virement programmé actif et dû vers un numéro de compte absent de la banque, après un passage du traitement automatique, le solde du compte source est inchangé, aucune opération n'est enregistrée sur ce compte, le client a reçu une notification de refus, et le virement est à l'état inactif.

Source : RG-021-007, BUG-068

#### FR-021-05 : Opération DEBIT sur le compte source

Lors de l'exécution, le système enregistre sur le compte source une opération DEBIT dont le libellé est « Virement programme vers {libellé bénéficiaire} », ou « Virement programme vers {libellé bénéficiaire} - {motif} » si un motif a été saisi.

Source : RG-021-008

#### FR-021-06 : Opération CREDIT sur le compte bénéficiaire interne

Lors de l'exécution vers un compte de la banque, le système enregistre sur le compte bénéficiaire une opération CREDIT dont le libellé est « Virement programme de {numéro du compte source} », ou « Virement programme de {numéro du compte source} - {motif} » si un motif a été saisi.

Source : RG-021-009

#### FR-021-07 : Suivi des exécutions

Après une exécution réussie, le système enregistre l'horodatage de la dernière exécution et augmente de 1 le nombre d'exécutions du virement programmé (affiché dans la colonne « Nb executions », FR-004-03).

Source : RG-021-010

#### FR-021-08 : Calcul de la prochaine échéance sans dérive

Après une exécution réussie, le système calcule la date de prochaine exécution à partir de la date d'échéance prévue (et non de l'heure réelle d'exécution), en ajoutant 7 jours calendaires pour « Hebdomadaire », 1 mois calendaire pour « Mensuel », 2 mois pour « Bimestriel », 3 mois pour « Trimestriel », 6 mois pour « Semestriel » et 1 an pour « Annuel ». Remplace le comportement legacy RG-021-011 (calcul depuis l'heure réelle d'exécution, qui faisait glisser le calendrier à chaque cycle). [ASSUMPTION A-021-4 : si la date ainsi calculée est encore passée (virement en retard de plus d'une période, par exemple après un report pour solde insuffisant), une seule exécution a lieu par passage, et le rattrapage se fait aux passages suivants ; le corpus ne traite pas ce cas.]

Critère d'acceptation (test) : un virement mensuel prévu le 15/01 à 08:00 et exécuté le 15/01 à 08:04 a pour prochaine exécution le 15/02 à 08:00 ; après douze exécutions mensuelles successives, l'échéance reste le 15 du mois à 08:00.

Source : RG-021-011, RG-021-012, BUG-069

#### FR-021-09 : Échec d'un virement isolé des autres

Si l'exécution d'un virement programmé échoue, le système annule entièrement les effets de cette exécution (ni débit, ni crédit, ni opération) [ASSUMPTION A-021-5 : « isoler l'échec » implique l'annulation complète de l'exécution en échec], journalise « Echec execution virement programme id={id} », et poursuit l'exécution des autres virements dus du même passage, sans que l'échec les affecte. Remplace le comportement legacy RG-021-013 (les virements suivants du même passage pouvaient être perdus).

Critère d'acceptation (test) : lors d'un passage avec trois virements dus dont le deuxième provoque une erreur, le premier et le troisième sont exécutés (soldes, opérations, compteurs et échéances mis à jour), le deuxième reste inchangé et un message d'échec est journalisé pour lui.

Source : RG-021-013, BUG-070

**Notes :**
- RG-021-004 : couverte par l'exigence fusionnée de FEAT-004 (règles voisines), FR-004-12 (solde insuffisant ; BUG-014 y ajoute la notification du client).
- Contradiction dans le corpus : le texte de RG-021-007 dit « reject the transfer and refund the source account », alors que son détail de nature et BUG-068 décrivent un débit sans crédit ni remboursement. FR-021-04 retient le comportement attendu de BUG-068 (refus avant tout débit).
- Les libellés de fréquence « Hebdomadaire », « Mensuel », « Bimestriel », « Trimestriel », « Semestriel », « Annuel » sont ceux de FR-004-08.
- Ce traitement n'appelle pas le contrôle de fraude des virements (aucune règle du corpus ne le relie à RG-027-003).

### FEAT-022 — Surveillance soldes faibles et génération d'alertes

**Description :** Traitement automatique sans écran, qui ne demande l'action d'aucun rôle : toutes les 30 minutes, il parcourt les comptes des clients actifs et crée une notification dans l'application pour chaque compte dont le solde est passé sous 100 €. Les notifications créées sont consultées par le CLIENT dans son centre de notifications (FEAT-002). Le traitement tolère les erreurs ponctuelles (un client ou un compte en échec est sauté) mais s'interrompt si la liste des clients ne peut pas être chargée. Cas limites marquants : solde absent, compte durablement sous le seuil (plafond quotidien de notifications), traçabilité de chaque exécution dans le journal des traitements consulté par l'ADMIN. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-022-01 : Analyse périodique des comptes des clients actifs

Toutes les 30 minutes, le système analyse les comptes des clients dont l'indicateur actif est vrai, et seulement ceux-là. Pour chaque client, il examine tous ses comptes, quel que soit leur type.

Source : RG-022-001, RG-022-002, RG-022-003

#### FR-022-02 : Détection d'un solde faible

Si le solde d'un compte est strictement inférieur à 100 €, le système crée une notification de solde faible pour le titulaire du compte, sauf si le plafond quotidien de FR-022-04 est atteint pour ce compte. Un compte dont le solde est absent est ignoré, sans notification.

Source : RG-022-004, RG-022-006

#### FR-022-03 : Contenu de la notification de solde faible

La notification créée a le type ACCOUNT_BALANCE, la priorité HIGH, le titre « Solde insuffisant sur votre compte » et le texte « Votre solde ({currentBalance} €) est passé sous le seuil d'alerte de {lowBalanceThreshold} €. Veuillez approvisionner votre compte. », où {currentBalance} est le solde du compte au moment de l'analyse et {lowBalanceThreshold} le seuil (100).

Source : RG-022-005

#### FR-022-04 : Plafond de 20 notifications de solde faible par compte et par jour

Le système crée au plus 20 notifications de solde faible par compte et par jour. Une fois ce plafond atteint, un compte resté sous le seuil ne génère plus de notification jusqu'au jour suivant ; ce plafond prime sur FR-022-02. Ce comportement remplace le comportement legacy RG-022-010, où le plafond déclaré n'était jamais appliqué (jusqu'à 48 notifications par jour). [ASSUMPTION A-022-1 : le « jour » est le jour calendaire, de 0 h à 24 h, heure de Paris.]

Critère d'acceptation (test) : pour un compte dont le solde reste à 50 € pendant 48 exécutions successives de l'analyse sur une même journée, exactement 20 notifications ACCOUNT_BALANCE sont créées pour ce compte ce jour-là ; la première exécution du jour suivant en crée de nouveau une.

Source : RG-022-010, BUG-071

#### FR-022-05 : Tolérance aux erreurs de l'analyse

Si la liste des clients actifs ne peut pas être chargée, le système interrompt toute l'exécution de l'analyse et journalise l'erreur. Si les comptes d'un client ne peuvent pas être chargés, il ignore ce client, journalise un avertissement et poursuit avec les clients suivants. Si la création de la notification d'un compte échoue, il journalise un avertissement, ignore ce compte et poursuit avec les comptes suivants.

Source : RG-022-007, RG-022-008, RG-022-009

#### FR-022-06 : Trace de chaque exécution dans le journal des traitements

À la fin de chaque exécution de l'analyse des soldes faibles, le système enregistre le résultat de l'exécution dans le journal d'exécution des traitements, consultable par l'administrateur (FEAT-016, section « Traitements nocturnes »). Ce comportement remplace le comportement legacy RG-022-011, où l'analyse n'était pas tracée dans ce journal. [ASSUMPTION A-022-2 : l'entrée enregistre le début, la fin, la durée, le nombre de notifications créées et un statut ; une exécution interrompue (FR-022-05) est aussi enregistrée, avec un statut d'échec et le détail de l'erreur.]

Critère d'acceptation (test) : après une exécution de l'analyse, une nouvelle entrée apparaît dans le journal des traitements avec son statut et son nombre d'enregistrements ; après une exécution interrompue faute de pouvoir charger les clients, une entrée en échec apparaît.

Source : RG-022-011, BUG-072

**Notes :**
- Le seuil de 100 € est codé en dur dans le legacy (un seuil par client était envisagé) ; il est repris tel quel.
- Dans le legacy, le déclencheur périodique ne survit pas à un redémarrage du serveur ; c'est un détail d'exécution, non repris comme exigence.
- Le solde {currentBalance} est inséré dans le texte sans mise en forme (par exemple « 50.00 ») ; la mise en forme relève de l'affichage des notifications (FEAT-002).
- Point d'attention : avec FR-022-06, l'analyse produit 48 entrées par jour dans le journal des traitements, dont l'écran d'administration n'affiche que les 50 plus récentes (FEAT-016).

### FEAT-023 — Traitement asynchrone des virements

**Description :** Traitement en arrière-plan, sans écran propre, qui prend le relais d'un virement enregistré depuis l'écran de virement (FEAT-003) par un utilisateur authentifié. Il produit pour l'émetteur une notification dans l'application, dont le type dépend du montant : CONFIRMATION sous le seuil d'alerte, ALERTE entre le seuil d'alerte et le seuil de fraude, FRAUDE au-delà. Les notifications de chaque utilisateur sont plafonnées (20 par défaut). Cas limites marquants : remise impossible au traitement (désormais virement refusé et client informé), virement d'un montant supérieur ou égal au seuil de fraude, dont la suspension avant tout débit dépend de la question ouverte QO-1. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-023-01 : Remise du virement au traitement asynchrone [EN ATTENTE RG-027-003 (a)(b)]

Acquis : une fois le virement enregistré, de sorte que son identifiant soit attribué, le système le remet au traitement asynchrone des virements, avec son identifiant, son montant, son émetteur, le numéro du compte destinataire et son motif. Ouvert : le legacy ne remet que des virements déjà validés ; la remise d'un virement que le contrôle de fraude juge suspect (a) ou dont le contrôle est en erreur (b) dépend de la question QO-1 du PRD. Voir FR-003-13.

Source : RG-023-001

#### FR-023-02 : Échec de la remise : virement refusé et client informé

Si le virement ne peut pas être remis au traitement asynchrone, le système refuse le virement et en informe le client sur l'écran de virement, au lieu de le présenter comme pris en compte ; aucun solde n'est modifié. [ASSUMPTION A-023-1 : texte du message « Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard » ; le corpus ne donne aucun texte] [ASSUMPTION A-023-2 : le refus annule l'enregistrement du virement, son débit, son crédit et ses opérations au relevé] Cette exigence remplace le comportement legacy RG-023-002, où l'échec était ignoré en silence et le virement jamais traité.

Critère d'acceptation (test) : avec le traitement asynchrone rendu indisponible, la soumission d'un virement valide de 100 € affiche le message d'échec, ne redirige pas vers le tableau de bord, laisse les soldes des deux comptes inchangés et n'enregistre aucun virement.

Source : RG-023-002, BUG-073

#### FR-023-03 : Notification selon le montant [EN ATTENTE RG-027-003 (a)]

Acquis : le traitement asynchrone ajoute une seule notification à l'émetteur du virement (FR-002-10), choisie selon le montant, avec le seuil d'alerte (5 000 € par défaut) et le seuil de fraude (10 000 € par défaut) de la configuration de déploiement :
- montant strictement inférieur au seuil d'alerte : CONFIRMATION, « Virement de {amount} EUR vers le compte {targetAccountNumber} confirme » (avec le motif selon FR-003-26) ;
- montant supérieur ou égal au seuil d'alerte et strictement inférieur au seuil de fraude : ALERTE, « Virement de {amount} EUR en attente de validation - transaction importante, verification en cours. » ;
- montant supérieur ou égal au seuil de fraude : FRAUDE, « Virement de {amount} EUR bloque pour verification (seuil anti-fraude : {seuilFraudeEur} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation. ».

Ouvert : une notification FRAUDE à la place de la CONFIRMATION pour un virement jugé suspect par le contrôle de fraude, et la véracité des mentions « en attente » et « bloque », dépendent de la question QO-1 du PRD. Même règle que FR-003-14 et FR-025-01.

Source : RG-023-004, RG-023-005, RG-023-006

#### FR-023-04 : Virement au-delà du seuil de fraude [EN ATTENTE RG-027-003 (a)]

Acquis : un virement d'un montant supérieur ou égal au seuil de fraude (10 000 € par défaut) donne lieu à la notification FRAUDE de FR-023-03, qui annonce un virement « bloque pour verification ». Le legacy débitait pourtant le compte source immédiatement : ce désaccord entre le message et le solde n'est pas reproduit. Ouvert, selon la réponse à la question QO-1 (a) du PRD :
- si un virement suspect est bloqué ou suspendu, le virement au-delà du seuil de fraude est suspendu en attente de vérification manuelle avant toute modification de solde (comportement attendu par BUG-076) ;
- s'il est seulement signalé, l'articulation avec le comportement attendu par BUG-076 (exception explicite ou conflit) est à trancher dans QO-1.

Cette exigence remplace le comportement legacy RG-023-009.

Critère d'acceptation (test) : après un virement de 10 000,00 €, le texte de la notification affichée, le statut du virement et le solde du compte source sont cohérents entre eux et conformes à la réponse à QO-1 (a) ; dans l'option « bloqué », le solde du compte source est inchangé tant que la vérification n'a pas eu lieu.

Source : RG-023-009, BUG-076

#### FR-023-05 : Plafond de notifications par utilisateur

Le système conserve au plus un nombre de notifications par utilisateur fixé par la configuration de déploiement, 20 par défaut ; une nouvelle notification est placée en tête de liste et, lorsque la limite est atteinte, la plus ancienne est supprimée. Même règle que FR-025-04 et FR-002-09.

Source : RG-023-010

#### FR-023-06 : Conservation des notifications

Les notifications sont conservées après un redémarrage du serveur, selon FR-002-09. Écart dans le corpus : RG-023-011 (Confirmé) décrit des notifications en mémoire, perdues au redémarrage ; le bug à corriger BUG-006 (FEAT-002) remplace ce comportement legacy.

Source : RG-023-011

**Notes :**
- RG-023-002 et RG-023-009 (Suspect, dette legacy) sont couvertes par les FR de correction FR-023-02 (BUG-073) et FR-023-04 (BUG-076) ; leur comportement legacy n'est pas reproduit.
- RG-023-003 (dette legacy) : non reproduite. BUG-074 (conservé, non corrigé) : un message de virement dans un format inattendu est écarté sans être dirigé vers une file de récupération.
- RG-023-008 (dette legacy) : non reproduite. BUG-075 (conservé, non corrigé) : une erreur pendant le traitement d'un virement écarte le message sans le diriger vers une file de récupération.
- RG-023-007 : couverte par l'exigence fusionnée de FEAT-003 (règles voisines), FR-003-26.

### FEAT-024 — Traitement asynchrone approbation prêts

**Description :** Traitement en arrière-plan, sans écran propre, qui décide du sort d'une demande de prêt soumise depuis la page Prêts (FEAT-007) par un utilisateur authentifié (CLIENT, CONSEILLER ou ADMIN). La demande enregistrée au statut EN_ATTENTE est transmise à un service de scoring crédit, qui l'approuve ou la refuse selon la durée et un plafond de montant par type de prêt ; le prêt passe alors à EN_COURS (avec son échéancier) ou à REFUSE, et l'emprunteur reçoit une notification dans l'application. Cas limites marquants : durée supérieure à 300 mois, montant exactement égal au plafond (approuvé), type de prêt non reconnu (refusé), message reçu pour un prêt qui n'est plus EN_ATTENTE (ignoré), transmission impossible (demande refusée et client informé, au lieu d'un prêt bloqué en attente). Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-024-01 : Transmission de la demande au service de scoring crédit

Quand un utilisateur soumet une demande de prêt valide, le système enregistre le prêt au statut EN_ATTENTE, de sorte que son identifiant soit attribué et lisible par le service de scoring crédit, puis transmet la demande à ce service pour un traitement asynchrone, avant de rendre la main à l'utilisateur.

Source : RG-024-001, RG-024-002

#### FR-024-02 : Échec de la transmission de la demande

Si la demande de prêt ne peut pas être transmise au service de scoring crédit, le système refuse la demande et en informe l'utilisateur sur la page Prêts par un message d'erreur, au lieu du message de succès de FR-007-07 [ASSUMPTION A-024-1 : texte « Service credit temporairement indisponible, veuillez reessayer plus tard »] ; le prêt n'est pas laissé au statut EN_ATTENTE [ASSUMPTION A-024-2 : le prêt enregistré est annulé et n'apparaît pas dans la liste des emprunts]. Cette règle remplace le comportement legacy RG-024-020, où la demande était abandonnée sans prévenir l'utilisateur et restait indéfiniment en attente.

Critère d'acceptation (test) : avec un service de scoring crédit rendu indisponible, la soumission d'une demande valide affiche un message d'erreur d'indisponibilité, n'affiche pas « Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes) », et aucun prêt EN_ATTENTE n'existe pour l'utilisateur après la soumission.

Source : RG-024-020, BUG-077

#### FR-024-03 : Refus pour durée excessive

Si la durée du prêt dépasse 300 mois, le service de scoring crédit refuse le prêt, quels que soient son type et son montant. Ce contrôle est évalué avant les plafonds par type (FR-024-04).

Source : RG-024-003

#### FR-024-04 : Plafond de montant par type de prêt

Pour une durée de 300 mois au plus, le service de scoring crédit approuve le prêt si son montant est inférieur ou égal au plafond de son type, et le refuse s'il le dépasse : CONSOMMATION 25 000 EUR, AUTO 40 000 EUR, TRAVAUX 80 000 EUR, ETUDIANT 20 000 EUR, IMMOBILIER 600 000 EUR. Un montant exactement égal au plafond est approuvé.

Source : RG-024-004, RG-024-005, RG-024-006, RG-024-007, RG-024-008, RG-024-009, RG-024-010, RG-024-011, RG-024-012, RG-024-013

#### FR-024-05 : Refus d'un type de prêt non reconnu

Si le type du prêt est absent ou ne fait pas partie des cinq types connus (IMMOBILIER, AUTO, CONSOMMATION, TRAVAUX, ETUDIANT), le service de scoring crédit refuse le prêt, quel que soit son montant. Cette règle remplace le comportement legacy RG-024-021, qui approuvait un tel prêt si son montant était inférieur ou égal à 25 000 EUR.

Critère d'acceptation (test) : une demande de 10 000 EUR sur 60 mois, transmise au service de scoring crédit avec un type absent, aboutit au statut REFUSE et à la notification ALERTE de refus.

Source : RG-024-021, BUG-078

#### FR-024-06 : Effets de l'approbation

Quand le service de scoring crédit approuve un prêt, le statut du prêt passe à EN_COURS, l'échéancier complet est généré (une échéance par mois de la durée), et l'emprunteur reçoit une notification de type CONFIRMATION : « Votre demande de pret {libelle} de {montant} EUR a ete approuvee par le service credit. », où {libelle} est « Immobilier », « Automobile », « Consommation », « Travaux » ou « Etudiant », et {montant} le montant en EUR.

Source : RG-024-014, RG-024-016

#### FR-024-07 : Effets du refus

Quand le service de scoring crédit refuse un prêt, le statut du prêt passe à REFUSE et l'emprunteur reçoit une notification de type ALERTE : « Votre demande de pret {libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive). »

Source : RG-024-015, RG-024-017

#### FR-024-08 : Demande reçue pour un prêt déjà traité

Quand le service de scoring crédit reçoit une demande d'approbation pour un prêt dont le statut n'est pas EN_ATTENTE, il l'ignore, sans modifier aucun état ni envoyer de notification.

Source : RG-024-018

#### FR-024-09 : Nombre maximal de notifications par utilisateur

Le système conserve au plus 20 notifications par utilisateur (valeur par défaut, configurable) ; quand ce nombre est atteint, l'ajout d'une notification supprime la plus ancienne.

Source : RG-024-019

**Notes :**
- RG-024-020 (Suspect, dette legacy) : remplacée par FR-024-02 (correction BUG-077).
- RG-024-021 (Suspect, dette legacy) : remplacée par FR-024-05 (correction BUG-078).
- Les règles de scoring sont aussi portées par FEAT-007 (FR-007-09, FR-007-10), avec les mêmes valeurs. Avec la correction BUG-025 (FR-007-06), une demande de 301 à 360 mois n'atteint plus le service de scoring ; FR-024-03 reste une garde.
- Les notifications s'affichent dans le centre de notifications du tableau de bord (FEAT-002).

### FEAT-025 — Système de notifications internes

**Description :** Les notifications internes informent l'utilisateur authentifié (CLIENT surtout ; tout rôle peut en recevoir) dans l'application, sur le centre de notifications du tableau de bord. Elles sont produites par le traitement des virements (CONFIRMATION, ALERTE ou FRAUDE selon le montant), par le traitement des demandes de prêt (approbation ou refus) et par le contrôle périodique des soldes faibles, qui, désormais, atteint réellement le client. L'utilisateur voit ses notifications de la plus récente à la plus ancienne, peut toutes les marquer lues ou toutes les effacer ; leur nombre est plafonné par utilisateur (20 par défaut). Cas limites marquants : plafond atteint (la plus ancienne disparaît), motif vide, solde strictement inférieur à 100 €. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-025-01 : Notification de virement selon le montant [EN ATTENTE RG-027-003 (a)]

Acquis : le traitement d'un virement ajoute à l'émetteur une seule notification (FR-002-10), choisie selon le montant, avec le seuil d'alerte (5 000 € par défaut) et le seuil de fraude (10 000 € par défaut) de la configuration de déploiement :
- montant strictement inférieur au seuil d'alerte : CONFIRMATION, « Virement de {amount} EUR vers le compte {targetAccountNumber} confirme », avec le montant et le numéro du compte destinataire ;
- montant supérieur ou égal au seuil d'alerte et strictement inférieur au seuil de fraude : ALERTE, « Virement de {amount} EUR en attente de validation - transaction importante, verification en cours. » ;
- montant supérieur ou égal au seuil de fraude : FRAUDE, « Virement de {amount} EUR bloque pour verification (seuil anti-fraude : {seuilFraudeEur} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation. ».

Ouvert : une notification FRAUDE à la place de la CONFIRMATION pour un virement jugé suspect, et la véracité des mentions « en attente de validation » et « bloque pour verification », dépendent de la question QO-1 du PRD. Même règle que FR-003-14 et FR-023-03.

Source : RG-025-001, RG-025-002, RG-025-003

#### FR-025-02 : Motif dans la notification de confirmation

Lorsque la notification est de type CONFIRMATION et que le motif est renseigné (ni absent, ni vide, ni composé uniquement d'espaces), le message devient « Virement de {amount} EUR vers le compte {targetAccountNumber} confirme (motif : {motif}) ». Même règle que l'exigence fusionnée FR-003-26.

Source : RG-025-004

#### FR-025-03 : Notifications de décision sur une demande de prêt

Lorsque le traitement des demandes de prêt (FEAT-024) approuve une demande, le système ajoute au demandeur une notification CONFIRMATION « Votre demande de pret {loanType.libelle} de {montant} EUR a ete approuvee par le service credit. ». Lorsqu'il la refuse, il ajoute une notification ALERTE « Votre demande de pret {loanType.libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive). ».

Source : RG-025-005, RG-025-006

#### FR-025-04 : Plafond de notifications par utilisateur

Lorsqu'une notification est ajoutée et que le nombre de notifications de l'utilisateur dépasserait la limite de la configuration de déploiement (20 par défaut), le système supprime la plus ancienne, pour que la liste reste dans la limite. Même règle que FR-023-05 et FR-002-09.

Source : RG-025-007

#### FR-025-05 : Affichage sur le tableau de bord

Le tableau de bord affiche, dans la section « Centre de notifications », les notifications de l'utilisateur authentifié, de la plus récente à la plus ancienne. Chaque notification affiche son type (« CONFIRMATION », « ALERTE » ou « FRAUDE », avec les icônes ✅, ⚠️ et 🚨), son message et son horodatage au format dd/MM/yyyy HH:mm:ss ; une notification non lue porte l'indication « Non lu ». Les autres textes du centre de notifications (état vide, badge) sont fixés par FR-002-07 et FR-002-08 ; ses étiquettes techniques ne sont pas reprises (voir FEAT-002, A-002-4).

Source : RG-025-008

#### FR-025-06 : Tout marquer comme lu

Lorsque l'utilisateur a au moins une notification non lue, le badge « {N} nouvelle(s) » et le bouton « Tout marquer lu » sont affichés ; un clic sur ce bouton marque comme lues toutes les notifications de l'utilisateur, et le badge disparaît.

Source : RG-025-009

#### FR-025-07 : Tout effacer

Lorsque l'utilisateur a au moins une notification, le bouton « Effacer tout » est affiché ; un clic sur ce bouton supprime toutes les notifications de l'utilisateur.

Source : RG-025-010

#### FR-025-08 : Alerte de solde faible affichée au client

Toutes les 30 minutes (NFR-BATCH-01), le système contrôle les comptes des clients actifs ; pour tout compte dont le solde est strictement inférieur à 100 €, il ajoute au client une notification de priorité haute, affichée dans le centre de notifications de son tableau de bord, de titre « Solde insuffisant sur votre compte » et de message « Votre solde ({currentBalance} €) est passé sous le seuil d'alerte de {lowBalanceThreshold} €. Veuillez approvisionner votre compte. ». [ASSUMPTION A-025-1 : la notification est affichée avec le type ALERTE et suit le même plafond que les autres notifications (FR-025-04)] [ASSUMPTION A-025-2 : une seule notification par compte tant que le solde reste sous le seuil ; une nouvelle notification n'est produite qu'après un retour du solde à 100 € ou plus, pour ne pas répéter l'alerte toutes les 30 minutes] Cette exigence remplace le comportement legacy RG-025-011, où la notification était enregistrée mais jamais affichée.

Critère d'acceptation (test) : un client actif dont le compte a un solde de 99,99 € voit, après le passage du contrôle périodique, la notification « Solde insuffisant sur votre compte » dans le centre de notifications de son tableau de bord ; un client dont le solde est de 100,00 € ne la reçoit pas.

Source : RG-025-011, BUG-079

**Notes :**
- RG-025-011 (Suspect, dette legacy) est couverte par la FR de correction FR-025-08 (BUG-079) ; son comportement legacy n'est pas reproduit.
- RG-025-012 (dette legacy) : non reproduite. BUG-080 (conservé, non corrigé) : le seuil de solde faible reste fixé à 100 € et n'est pas configurable.
- RG-025-013 (dette legacy) : non reproduite. BUG-081 (conservé, non corrigé) : l'ancienne méthode d'envoi de notification, obsolète, sans appelant et sans effet, n'est pas reprise.
- RG-025-014 (dette legacy) : non reproduite. BUG-082 (conservé, non corrigé) : la priorité haute des notifications de transaction de plus de 1 000 € n'est jamais appliquée dans le legacy ; les notifications de virement suivent FR-025-01.
- Le legacy a deux circuits de notifications ; seul le circuit affiché est repris, plus l'alerte de solde faible (FR-025-08). Les notifications de prêt du circuit non affiché (« Prêt approuvé », « Prêt refusé ») ne sont pas reprises : seules celles de FR-025-03 étaient visibles.

### FEAT-026 — Audit trail

**Description :** L'audit trail est une fonction transverse sans écran : il enregistre les actions significatives des utilisateurs CLIENT, CONSEILLER et ADMIN (connexion, échec de connexion, déconnexion, actions d'administration sur les clients et les prêts, virements). Chaque entrée porte l'utilisateur, un code d'action, un détail, l'adresse IP d'origine et un horodatage serveur. Une entrée est conservée même si l'opération qui l'a déclenchée échoue. Le journal est en écriture seule : l'application n'offre aucun écran ni aucune fonction pour le consulter. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-026-01 : Audit des connexions, échecs de connexion et déconnexions

Le système enregistre une entrée d'audit :
- à chaque connexion réussie : identifiant de l'utilisateur, action « LOGIN », détail « Connexion réussie », adresse IP du client et horodatage serveur ;
- à chaque tentative de connexion échouée : identifiant saisi, action « LOGIN_FAILED », détail « Tentative échouée », adresse IP du client et horodatage serveur ;
- à chaque fin de session par déconnexion d'un utilisateur authentifié : identifiant de l'utilisateur, action « LOGOUT », détail « Déconnexion » et horodatage serveur (l'adresse IP est exigée par FR-026-05).

Voir aussi FR-001-12 (déclenchement depuis la connexion).

Source : RG-026-001, RG-026-002, RG-026-003

#### FR-026-02 : Audit des actions d'administration

Le système enregistre une entrée d'audit portant l'identifiant de l'administrateur :
- à la désactivation d'un client : action « ADMIN_DEACTIVATE », détail « Désactivation client id={customerId} » ;
- à l'activation d'un client : action « ADMIN_ACTIVATE », détail « Activation client id={customerId} » ;
- à l'approbation d'une demande de prêt : action « ADMIN_LOAN_APPROVE », détail « Approbation prêt id={loanId} » ;
- au refus d'une demande de prêt : action « ADMIN_LOAN_REFUSE », détail « Refus prêt id={loanId} ».

Source : RG-026-004, RG-026-005, RG-026-006, RG-026-007

#### FR-026-03 : Audit des virements

Lorsqu'un virement est soumis avec succès, le système enregistre une entrée d'audit portant l'identifiant de l'utilisateur, l'action « VIREMENT » et le détail « Montant: {amount} - Motif: {motif} » (montant et motif du virement).

Source : RG-026-008

#### FR-026-04 : Entrée d'audit indépendante de l'opération

Le système enregistre chaque entrée d'audit dans une transaction indépendante de celle de l'opération qui la déclenche : l'entrée est conservée même si l'opération est annulée.

Source : RG-026-009

#### FR-026-05 : Adresse IP d'origine pour toute action auditée

Lorsqu'une action est auditée, quelle qu'elle soit (LOGIN, LOGIN_FAILED, LOGOUT, ADMIN_DEACTIVATE, ADMIN_ACTIVATE, ADMIN_LOAN_APPROVE, ADMIN_LOAN_REFUSE, VIREMENT), le système enregistre l'adresse IP d'origine du client dans l'entrée, résolue selon FR-001-13. Cette exigence remplace le comportement legacy RG-026-010 (adresse enregistrée seulement pour LOGIN et LOGIN_FAILED).

Critère d'acceptation (test) : pour chacun des huit codes d'action, une action déclenchée depuis une adresse IP connue produit une entrée d'audit dont l'adresse IP est renseignée et égale à cette adresse.

Source : RG-026-010, BUG-083

#### FR-026-06 : Journal en écriture seule

Le système conserve les entrées d'audit en écriture seule : aucun écran ni aucune fonction de l'application ne permet de consulter, filtrer ou afficher le journal d'audit.

Source : RG-026-011

**Notes :**
- RG-026-010 (Suspect, dette legacy) est couverte par la FR de correction FR-026-05 ; son comportement legacy n'est pas reproduit.
- Les entrées LOGIN, LOGIN_FAILED et LOGOUT sont aussi décrites dans FEAT-001 (RG-001-013 à RG-001-015, FR-001-12), sans fusion décidée ; les deux descriptions concordent (mêmes codes d'action et détails).
- Les traitements système (sans utilisateur) ne sont pas audités par cette fonctionnalité : le corpus ne décrit que les actions des utilisateurs.

### FEAT-027 — Détection de fraude en temps réel

**Description :** Contrôle transversal, sans écran propre côté client, appliqué à chaque virement soumis (FEAT-003) : huit signaux de fraude, chacun avec un score de risque, sont évalués ; au-delà d'un score cumulé de 75, une alerte par signal levé est enregistrée au statut OPEN. Le client est averti du nombre d'alertes détectées ; l'ADMIN consulte les alertes dans l'administration et les confirme (CONFIRMED) ou les classe en faux positif (FALSE_POSITIVE). Le contrôle s'exécute désormais avant la validation finale du virement et ses erreurs sont journalisées ; le sort d'un virement suspect (a) et celui d'un virement dont le contrôle est en erreur (b) restent ouverts (QO-1). Cas limites marquants : score cumulé exactement 75 (aucune alerte), heure du serveur hors 06:00-23:00, IBAN étranger, fréquence comptée sur le jour courant, échecs de connexion répétés. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-027-01 : Évaluation des huit signaux pour chaque virement

Pour chaque virement soumis par un client, le système évalue les huit signaux de fraude : LARGE_AMOUNT, HIGH_FREQUENCY, NEW_BENEFICIARY, UNUSUAL_HOURS, ROUND_AMOUNT, FOREIGN_IBAN, RAPID_SUCCESSION et VELOCITY_CHECK (FR-003-17, FR-003-21, FR-003-23 et FR-027-05 à FR-027-10).

Source : RG-027-001

#### FR-027-02 : Contrôle avant la validation finale, erreurs journalisées [EN ATTENTE RG-027-003 (a)(b)]

Acquis : le contrôle de fraude s'exécute avant la validation finale du virement, et toute erreur du contrôle est journalisée ; une erreur n'est plus ignorée en silence. Cette exigence remplace le comportement legacy RG-027-003, où le contrôle s'exécutait après la validation du virement et où ses erreurs étaient ignorées. Ouvert, voir la question QO-1 du PRD :
- (a) quand le contrôle détecte une suspicion, le virement est-il bloqué (ou suspendu), ou seulement signalé ;
- (b) quand le contrôle tombe en erreur, le virement est-il refusé, ou exécuté avec une alerte.

Critère d'acceptation (test) : pour un virement soumis, le contrôle de fraude est exécuté avant que le virement ne soit validé (aucun débit ni crédit n'est validé avant la fin du contrôle) ; avec un contrôle de fraude forcé en erreur, l'erreur figure dans le journal du serveur et l'issue du virement est celle que fixe la réponse à QO-1 (b) ; avec un virement dont le score cumulé dépasse 75, l'issue est celle que fixe la réponse à QO-1 (a).

Source : RG-027-003, BUG-084

#### FR-027-03 : Enregistrement des alertes au-delà du score 75

Si la somme des scores de tous les signaux levés pour un virement dépasse strictement 75, le système enregistre une alerte par signal levé, au statut OPEN, avec son score et sa description. Un score cumulé de 75 ou moins n'enregistre aucune alerte. Même règle que FR-003-25.

Source : RG-027-002

#### FR-027-04 : Avertissement du client [EN ATTENTE RG-027-003 (a)]

Acquis : si au moins une alerte de fraude est détectée pour le virement, le système affiche au client un avertissement générique : « Attention : {N} alerte(s) fraude détectée(s) sur ce virement. Votre conseiller a été notifié. », où N est le nombre d'alertes. Ouvert : le legacy affiche ce message après un virement exécuté ; que le virement soit exécuté, bloqué ou suspendu à ce moment dépend de la question QO-1 du PRD. Voir FR-003-16.

Source : RG-027-013

#### FR-027-05 : Signal LARGE_AMOUNT de score 40

Le système lève un signal LARGE_AMOUNT de score 40 si le montant est supérieur ou égal à 5 000 € et strictement inférieur à 15 000 €, avec la description « Transaction amount {amount} exceeds the large threshold of {threshold} ». Le score 80 à partir de 15 000 € est porté par l'exigence fusionnée FR-003-17.

Source : RG-027-004

#### FR-027-06 : Signal HIGH_FREQUENCY sur les transactions du jour

Le système lève un signal HIGH_FREQUENCY de score 50 si le nombre de transactions du client exécutées le jour courant dépasse 8, avec la description « Customer has {count} transactions today, exceeding the daily limit of {maxDailyTransactions} ». Les transactions des jours précédents ne sont pas comptées. Cette exigence remplace le comportement legacy de RG-027-006, qui comptait tout l'historique du client.

Critère d'acceptation (test) : un client qui a 100 transactions anciennes et 8 transactions le jour courant ne déclenche pas HIGH_FREQUENCY ; avec 9 transactions le jour courant, le signal est levé avec le score 50.

Source : RG-027-006, BUG-085

#### FR-027-07 : Signal NEW_BENEFICIARY

Le système lève un signal NEW_BENEFICIARY de score 35 si l'IBAN destinataire n'est pas enregistré comme bénéficiaire actif du client (FEAT-013), avec la description « Target IBAN {iban} is not registered as a trusted beneficiary ».

Source : RG-027-007

#### FR-027-08 : Signal UNUSUAL_HOURS

Le système lève un signal UNUSUAL_HOURS de score 45 si l'heure du serveur au moment du virement est avant 06:00 ou après 23:00, avec la description « Transaction initiated at hour {hourOfDay} which is outside normal operating hours (06:00-23:00) ». L'heure est celle du fuseau configuré de l'application (NFR-BATCH-03), et non celle du client. [ASSUMPTION A-027-1 : la correction suggérée par le corpus (fuseau du client, JIRA-7703) n'a pas fait l'objet d'une décision ; le fuseau de l'application est conservé]

Source : RG-027-008

#### FR-027-09 : Signal FOREIGN_IBAN

Le système lève un signal FOREIGN_IBAN de score 30 si l'IBAN destinataire ne commence pas par « FR », sans tenir compte de la casse, avec la description « Target IBAN {iban} does not start with FR (possible foreign transfer) ».

Source : RG-027-010

#### FR-027-10 : Signal VELOCITY_CHECK

Le système lève un signal VELOCITY_CHECK de score 65 si le nombre de virements de plus de 500 € exécutés par le client dans les 60 dernières minutes est supérieur ou égal à 5, avec la description « Customer executed {highValueCount} transactions above 500 EUR in the last {velocityWindowMinutes} minutes (limit: {velocityMaxCount}) ». Voir FR-003-24, qui parle de « transactions ».

Source : RG-027-012

#### FR-027-11 : Signal MULTIPLE_FAILED_AUTH après échecs de connexion

Le système lève un signal MULTIPLE_FAILED_AUTH de score 80 après 5 échecs de connexion consécutifs sur un même compte client (FR-001-06). Son score dépassant 75, il donne lieu à une alerte OPEN, comme le prévoit FR-027-03, consultable et traitable par l'ADMIN (FR-027-12). [ASSUMPTION A-027-2 : le signal est évalué à la connexion, indépendamment de tout virement ; il est levé au 5e échec consécutif, un succès de connexion remettant le décompte à zéro, et ne s'ajoute pas au score d'un virement] [ASSUMPTION A-027-3 : description de l'alerte « Customer account has {count} consecutive failed authentication attempts », dans la langue des autres descriptions]

Source : RG-027-017, RG-027-002

#### FR-027-12 : Consultation des alertes par l'ADMIN

Dans l'administration, réservée au rôle ADMIN (FR-016-01), le panneau « Alertes Fraude — Moteur de détection » liste les alertes de fraude par date de création décroissante, avec le badge « {N} ouverte(s) » quand des alertes sont au statut OPEN. Écart dans le corpus : RG-027-014 limite la liste aux alertes OPEN, alors que le bug à corriger BUG-056 (FEAT-016) exige toutes les alertes, quel que soit leur statut ; FR-016-16 s'applique et remplace cette limitation. Présentation détaillée du tableau : FR-016-14.

Source : RG-027-014

#### FR-027-13 : Confirmation d'une alerte

Lorsque l'ADMIN clique sur « Confirmer » pour une alerte OPEN, le système passe l'alerte au statut CONFIRMED, enregistre l'identifiant de l'ADMIN réviseur et l'heure du serveur de la revue ; la ligne affiche ensuite « Traité par {reviewedBy} » à la place des boutons.

Source : RG-027-015

#### FR-027-14 : Classement en faux positif

Lorsque l'ADMIN clique sur « Faux positif » pour une alerte OPEN, le système passe l'alerte au statut FALSE_POSITIVE, enregistre l'identifiant de l'ADMIN réviseur et l'heure du serveur de la revue ; la ligne affiche ensuite « Traité par {reviewedBy} » à la place des boutons.

Source : RG-027-016

#### FR-027-15 : IBAN masqué dans les journaux

Le système masque l'IBAN destinataire dans toutes les entrées de journal du serveur produites par le contrôle de fraude, par exemple « FR76****1234 » (NFR-SEC-05). Cette exigence remplace le comportement legacy de RG-027-018, qui journalisait l'IBAN en clair.

Critère d'acceptation (test) : après un virement vers l'IBAN FR7630006000011234567890189 qui lève des signaux de fraude, aucune ligne du journal du serveur ne contient cet IBAN en clair ; les lignes qui le mentionnent n'en montrent que les 4 premiers et les 4 derniers caractères.

Source : RG-027-018, BUG-086

**Notes :**
- RG-027-005 : couverte par l'exigence fusionnée de FEAT-003 (règles voisines), FR-003-17.
- RG-027-009 : couverte par l'exigence fusionnée de FEAT-003 (règles voisines), FR-003-21.
- RG-027-011 : couverte par l'exigence fusionnée de FEAT-003 (règles voisines), FR-003-23.
- RG-027-003, RG-027-006 et RG-027-018 (Suspect, dette legacy) sont couvertes par les FR de correction FR-027-02 (BUG-084), FR-027-06 (BUG-085) et FR-027-15 (BUG-086) ; leur comportement legacy n'est pas reproduit.
- RG-027-003 : le corpus porte, sous cette règle, une mention de résolution selon laquelle la détection « ne bloque pas le virement : elle le signale ». Cette mention est en tension avec le statut ouvert de la question (note de revue de BUG-084 dans LEGACY-BUGS.md) ; elle n'est pas reprise comme décision. QO-1 reste la seule référence.
- RG-027-017 (Suspect, nature métier) : reprise dans FR-027-11 comme prolongement de RG-027-002, conformément à la résolution du corpus (signal de score 80 après 5 échecs consécutifs).
- RG-027-019 (dette legacy) : non reproduite. BUG-087 (conservé, non corrigé) : le point d'entrée public d'évaluation d'un score isolé n'est ni repris ni supprimé par une exigence ; voir QO-2.
- Le statut REVIEWED existe dans les données de référence du legacy mais aucune action ne le produit ; il n'est atteint que par ces données.
- Les descriptions d'alertes sont en anglais dans le legacy alors que l'interface est en français ; le corpus le signale comme une dette à homogénéiser, sans bug à corriger. Les FR reprennent le texte affiché par le legacy.

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

### FEAT-029 — Intégration AlphaVantage (cotations boursières)

**Description :** Capacité transverse qui alimente la page bourse (FEAT-009), utilisée par tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) : récupération de l'historique des cours auprès du fournisseur AlphaVantage selon la période, filtrage et tri des points, calculs affichés et gestion de la watchlist. Toute réponse en échec du fournisseur est traitée comme une absence de données, sans erreur propagée à l'utilisateur. La clé d'accès au fournisseur ne figure jamais dans le code source. L'accès au fournisseur (port, adaptateur simulé, délai d'attente, coupe-circuit, cache) suit NFR-INT. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-029-01 : Série demandée selon la période

Pour les périodes "1Y" et "5Y", le système demande au fournisseur la série hebdomadaire ; pour "1W" et "1M", la série journalière compacte (100 jours). Pour une période qui n'est aucune des quatre, le système applique une date limite par défaut d'un mois. Voir NFR-INT pour l'accès au fournisseur.

Source : RG-029-001, RG-029-002, RG-029-003

#### FR-029-02 : Filtrage et tri des points

Le système exclut tout point dont la date est strictement antérieure à la date limite de la période et renvoie les points triés par date croissante.

Source : RG-029-004, RG-029-005

#### FR-029-03 : Réponses du fournisseur en échec

Si la réponse contient un champ d'information, de note ou de message d'erreur, si le statut n'est pas un succès, ou si une erreur quelconque survient pendant l'appel, le système journalise un avertissement ou une erreur et traite le résultat comme une absence de données, sans propager l'erreur. Une absence de données affiche le message "⚠️ Aucune donnée reçue pour « {symbole} » sur {libellé de période}. Causes possibles : symbole invalide pour cette place boursière (vérifiez le suffixe, ex: .PA pour Euronext), ou limite de 25 requêtes/jour atteinte (plan gratuit Alpha Vantage). Réessayez dans quelques minutes ou changez de valeur." (libellés de période : "1 semaine (données journalières)", "1 mois (données journalières)", "1 an (données hebdomadaires)", "5 ans (données hebdomadaires)").

Source : RG-029-006, RG-029-007, RG-029-008, RG-029-015

#### FR-029-04 : Adresse du fournisseur configurable

Au démarrage, le système lit l'adresse du fournisseur dans le paramètre ALPHA_VANTAGE_BASE_URL (variable d'environnement, puis propriété système), avec la valeur par défaut "https://www.alphavantage.co/query".

Source : RG-029-010

#### FR-029-05 : Clé d'accès protégée

Au démarrage, le système lit la clé d'accès dans le paramètre ALPHA_VANTAGE_API_KEY (variable d'environnement, puis propriété système) et ne se replie jamais sur une valeur écrite dans le code source : la clé n'est jamais exposée dans le code. Ce comportement remplace le comportement legacy RG-029-009 (repli sur une clé par défaut écrite en dur). [ASSUMPTION A-029-1 : sans clé configurée, aucun appel réel n'est émis et la page se comporte comme une absence de données (FR-029-03).]

Critère d'acceptation (test) : aucune clé d'accès ne figure dans le code source ni dans les fichiers versionnés ; au démarrage sans clé configurée, aucun appel n'est émis avec une clé par défaut.

Source : RG-029-009, RG-029-012, BUG-091

#### FR-029-06 : Période par défaut et première valeur

Au premier chargement de la page bourse, le système présélectionne la période "1M" ("1 Mois") et, si la watchlist n'est pas vide, charge automatiquement les données de sa première valeur.

Source : RG-029-013, RG-029-014

#### FR-029-07 : Calculs et indicateur de variation

Quand un historique est disponible, le cours actuel est la clôture du dernier point, la variation est (dernière clôture − première clôture) et la variation en pourcentage est ((dernière clôture − première clôture) / première clôture) × 100. Le système affiche la variation en positif (vert, "▲") quand la variation en pourcentage est supérieure ou égale à zéro, en négatif (rouge, "▼") sinon (même règle que FR-009-05).

Source : RG-029-016, RG-029-017, RG-029-018, RG-029-019

#### FR-029-08 : Dernières séances et graphique

Le tableau des dernières séances affiche au plus les 10 points les plus récents, du plus récent au plus ancien. Le graphique est généré côté serveur et intégré à la page, au format 800 × 300, avec 5 lignes horizontales de grille et 5 dates (même exigence que FR-009-06 et FR-009-07).

Source : RG-029-020, RG-029-021

#### FR-029-09 : Contrôles à l'ajout d'une valeur

Le système rejette un ajout à la watchlist avec "Le symbole est obligatoire (ex: AMZN, SG.PA)" si le symbole est vide ou absent, "Le nom de la valeur est obligatoire" si le nom est vide ou absent, et "Le symbole « {SYMBOLE} » existe déjà dans votre watchlist" (symbole en majuscules) si le symbole existe déjà, sans tenir compte de la casse.

Source : RG-029-022, RG-029-023, RG-029-024

#### FR-029-10 : Normalisation et sélection après ajout

Le système enregistre le symbole en majuscules sans espaces de bord, le nom et le marché sans espaces de bord ; après un ajout réussi ("Valeur ajoutée !"), il sélectionne la nouvelle valeur et charge immédiatement ses données.

Source : RG-029-025, RG-029-026

#### FR-029-11 : Ordre de la watchlist

Le système renvoie les valeurs de la watchlist triées par marché puis par symbole.

Source : RG-029-027

#### FR-029-12 : Suppression d'une valeur sélectionnée

Quand l'utilisateur supprime la valeur sélectionnée et que la watchlist n'est pas vide, le système sélectionne la première valeur restante et charge ses données. Quand il supprime la dernière valeur, le système efface la sélection, l'historique et le graphique.

Source : RG-029-028, RG-029-029

#### FR-029-13 : Longueur du symbole et du nom

Le système refuse un symbole de plus de 20 caractères et un nom de plus de 100 caractères ; les champs "Symbole *" et "Nom *" limitent la saisie à ces longueurs. [ASSUMPTION A-029-2 : le refus passe par la limitation de saisie du champ, sans message d'erreur dédié ; le corpus n'en donne aucun.]

Source : RG-029-030, RG-029-031

#### FR-029-14 : Marchés et devises autorisés

Le système n'accepte comme marché que "Euronext Paris", "NASDAQ", "NYSE", "LSE" (libellé affiché "London SE") ou "Autre", et comme devise que "EUR", "USD" ou "GBP".

Source : RG-029-032, RG-029-033

**Notes :**
- RG-029-011 (dette legacy) : non reproduite. Bug conservé associé : BUG-090 (appel sans délai d'attente). Le délai d'attente est imposé par NFR-INT.
- Les messages de journalisation techniques (par exemple "Erreur Alpha Vantage pour {symbol}") ne sont jamais affichés à l'utilisateur : non repris comme exigence d'affichage.
- Recouvrement avec FEAT-009 (aucune fusion décidée) : les FR de cette fonctionnalité et celles de FEAT-009 décrivent le même écran et doivent rester identiques.
- Écart corpus : la validation de FEAT-009 indique qu'aucune limite de longueur n'est appliquée à la saisie, alors que la page legacy limite le symbole à 20 caractères et le nom à 100 ; les bugs BUG-035 et BUG-036 (conservés) décrivent le comportement serveur au-delà de ces limites.

### FEAT-030 — Contrôle d'accès RBAC

**Description :** Le contrôle d'accès est une fonction transverse, sans écran propre, qui s'applique à toutes les requêtes des trois rôles CLIENT, CONSEILLER et ADMIN (le rôle d'un nouvel utilisateur est CLIENT par défaut). Il laisse passer sans session la page de connexion, le retour du fournisseur d'open banking et les ressources statiques, renvoie vers la connexion tout autre accès non authentifié, réserve l'espace conseiller aux rôles CONSEILLER et ADMIN et l'administration au rôle ADMIN, et n'affiche les liens de navigation correspondants qu'aux rôles autorisés. Les refus sont des redirections silencieuses, sans message. Le contrôle des rôles est aussi appliqué dans les services métier, et non plus seulement à l'entrée des requêtes. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-030-01 : Accès public limité et redirection des accès non authentifiés

Le système sert sans session authentifiée la page de connexion, la page de retour du fournisseur d'open banking et les ressources statiques de l'interface. Toute autre requête vers un écran, sans utilisateur authentifié dans la session, est redirigée vers la page de connexion. [ASSUMPTION A-030-1 : l'exemption est accordée sur le chemin exact de ces écrans, et non à toute adresse qui contient leur nom comme dans le legacy, afin qu'une adresse forgée ne contourne pas l'authentification] Voir aussi FR-001-01 et FR-001-02.

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


## 5. Non-Goals (hors périmètre explicite)

- **FEAT-031 — Analytique client et segmentation** : non refaite. C'est du code mort du legacy, sans aucun appelant, et le métier a confirmé que la fonctionnalité est abandonnée (`docs/retro/00-global/dead-features.json`, gate retro-features).
- **FEAT-032 — Bean de gestion des notifications côté interface** : non refaite. Code mort, référencé par aucun écran, et le métier a confirmé que la fonctionnalité est abandonnée. Les notifications internes restent dans le périmètre (§4, notifications internes).
- **Migration des données du legacy** : aucune reprise de clients, de comptes ni d'historique réels. Les données de référence sont rechargées (décision de cadrage).
- **Notifications hors de l'application** : ni e-mail, ni SMS, ni push (décision de cadrage).
- **Engagement réglementaire de production** (DSP2, RGPD, durée légale de conservation de l'audit) : hors de cette version, qui est un démonstrateur (décision de cadrage). Les exigences de sécurité d'une vraie banque restent dues (§7.3).
- **Environnements autres que le local** : seul le lancement local par `compose.yaml` est dans le périmètre. Aucun hébergement cible n'est spécifié (décision de cadrage).
- **Évolutions fonctionnelles au-delà de la parité** : aucune, en dehors des 50 bugs à corriger et des décisions tracées dans ce PRD.
- **Bugs conservés** : les bugs du legacy décidés « ne pas corriger » ne sont ni des exigences ni des tests. Ils sont listés dans `docs/retro/00-global/LEGACY-BUGS.md`.

## 6. Périmètre de la version

### 6.1 Dans le périmètre

- Les 30 fonctionnalités FEAT-001 à FEAT-030 (§4), en parité avec le legacy.
- La correction des 50 bugs à corriger, chacune couverte par un test.
- Les contrôles de `docs/quality-gates.md`, livrés et verts avant toute story fonctionnelle (NFR-QG-00).
- Les adaptateurs simulés des trois fournisseurs externes.
- Le golden master dérivé et son rejeu final.

### 6.2 Hors périmètre de la version

Voir §5.

## 7. Exigences non fonctionnelles transverses

### 7.1 Contrôles qualité (repris intégralement de `docs/quality-gates.md`)

Chaque contrôle est exécuté par un outil, jamais évalué par un agent. Les seuils sont ceux de `docs/quality-gates.md`, sans reformulation. Pour le tracing, le moment d'exécution est précisé quand la référence ne le donne pas (A-G-03).

- **NFR-QG-00 — Contrôles d'abord.** Les contrôles sont livrés et verts avant toute story fonctionnelle. Aucune story fonctionnelle ne démarre tant que les contrôles NFR-QG-01 à NFR-QG-09 ne sont pas livrés et verts.
- **NFR-QG-01 — Hexagone back.** Outil : test ArchUnit `*ArchitectureTest` dans chaque service. Seuil : deux règles, le sens des dépendances, et aucun framework dans domain et application. Moment : bloquant à chaque build.
- **NFR-QG-02 — Hexagone front.** Outil : dependency-cruiser, configuration `frontend/.dependency-cruiser.cjs`. Seuil : règles en sévérité error, le sens des dépendances, et aucun paquet npm dans `src/domain` et `src/application`. Moment : bloquant.
- **NFR-QG-03 — Couverture back.** Outil : JaCoCo check. Seuil : 100 % lignes et branches sur domain et application ; 80 % sur adapter et infrastructure. Moment : phase verify, bloquant.
- **NFR-QG-04 — Couverture front.** Outil : Vitest `coverage.thresholds` par glob. Seuil : mêmes seuils (100 % lignes et branches sur `src/domain` et `src/application` ; 80 % sur `src/adapters` et `src/infrastructure`). Moment : bloquant.
- **NFR-QG-05 — Mutation back.** Outil : PIT dans le profil Maven `mutation`, sur domain et application. Seuil : `mutationThreshold` 85, historique activé. Moment : à la demande.
- **NFR-QG-06 — Mutation front.** Outil : StrykerJS sur `src/domain` et `src/application`. Seuil : thresholds break 85 / high 90, mode incremental. Moment : à la demande.
- **NFR-QG-07 — CVE back.** Outil : OWASP Dependency-Check dans le profil Maven `security`. Seuil : `failBuildOnCVSS` 7. Configuration : clé d'API NVD via `settings.xml`, OSS Index et Node Audit désactivés, mise à jour de la base désactivée pendant les scans. Moment : à l'exécution du profil `security`.
- **NFR-QG-08 — CVE back et front.** Outil : Trivy fs. Seuil : sévérités HIGH et CRITICAL ; porte bloquante avec `--ignore-unfixed`. Les vulnérabilités sans correctif sont journalisées dans `reports/`, sans bloquer. Moment : porte bloquante.
- **NFR-QG-09 — Tracing prouvé.** Outil : `scripts/gates/trace-smoke.sh`. Seuil : il prouve qu'une trace traverse deux services, avec un span JDBC et un span Kafka. Moment : avant de déclarer terminée toute story qui touche un échange entre services. [ASSUMPTION A-G-03 : moment non précisé dans `docs/quality-gates.md`]
- **NFR-QG-10 — Les contrôles ne se modifient pas.**
  - Un contrôle rouge se corrige dans le code ou dans les tests, jamais en modifiant le contrôle.
  - Fichiers interdits de modification : `scripts/gates/**`, `.bmad-loop/policy.toml`, `.bmad-loop/gates.sha256`, `.claude/settings.json`, `.claude/hooks/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`.
  - Interdit d'abaisser un seuil, d'ajouter une exclusion de couverture ou de mutation, de désactiver un test.
  - Si un seuil paraît inatteignable : s'arrêter et le signaler, ne pas contourner.

### 7.2 Parité et tests

- **NFR-PAR-01 — Parité par le golden master.**
  - Le golden master du legacy, dérivé pour la nouvelle application, rejoue sans échec avant la bascule.
  - Il se rejoue une seule fois, développement terminé, pas entre les stories ni les epics.
  - Les 31 scénarios applicables doivent passer. Les 2 scénarios exclus (SC-003-03 et SC-010-02) reproduisent des bugs à corriger. Contrat : `docs/retro/golden-master-contract.md`.
- **NFR-PAR-02 — Bugs corrigés prouvés.** Chacun des 50 bugs à corriger est couvert par un test automatisé qui vérifie le comportement attendu (critère d'acceptation de la FR qui le cite).
- **NFR-PAR-03 — Règles couvertes.** Chaque règle de gestion reprise est couverte par au moins une exigence de ce PRD et par au moins un test.
- **NFR-PAR-04 — Données de référence.**
  - Un jeu de données de référence, équivalent à celui du legacy (`import.sql` : 7 clients, 7 comptes, 45 transactions…), est chargé par une commande.
  - Une commande de remise à zéro le rétablit.
  - Il sert au développement local, aux tests de bout en bout et au rejeu du golden master.

### 7.3 Sécurité

- **NFR-SEC-01** : les mots de passe sont stockés et vérifiés par une fonction de hachage à sens unique sûre (FR de FEAT-001, BUG-002).
- **NFR-SEC-02** : à l'authentification, la session existante est invalidée et une nouvelle session est créée avant de stocker le contexte d'authentification. Aucune fixation de session n'est possible (BUG-003, BUG-092).
- **NFR-SEC-03** : toute action ou lecture portant sur une ressource d'un client (compte, document, virement programmé, bénéficiaire) vérifie que la ressource appartient à l'utilisateur authentifié. Le contrôle d'accès par rôle est appliqué dans les services, indépendamment du filtrage des requêtes (BUG-013, BUG-027, BUG-047, BUG-058, BUG-093).
- **NFR-SEC-04** : aucun message d'erreur affiché à un utilisateur n'expose de détail technique (pile d'appels, code d'erreur interne).
- **NFR-SEC-05** : les IBAN sont masqués dans tous les journaux du serveur (par exemple `FR76****1234`) (BUG-086).
- **NFR-SEC-06** : aucun secret (clé d'API, mot de passe) n'est écrit dans le code source ni dans un fichier versionné. Les secrets viennent de la configuration d'exécution (BUG-034, BUG-091).
- **NFR-SEC-07** : la session utilisateur expire après 30 minutes d'inactivité, comme dans le legacy (`docs/retro/00-global/PERMISSIONS.md`).

### 7.4 Fournisseurs externes

- **NFR-INT-01 — Port et adaptateur simulé.**
  - Chaque fournisseur externe (Azure OpenAI, Powens, AlphaVantage) est appelé derrière un port.
  - Un adaptateur simulé est actif par défaut en local, dans les tests et dans les contrôles. Les vrais appels sont une option de configuration.
  - Aucun test ni aucun contrôle ne dépend du réseau ni d'une clé externe.
- **NFR-INT-02 — Résilience.**
  - Chaque appel réel à un fournisseur externe a un délai d'attente et passe par un coupe-circuit.
  - Les cotations boursières sont mises en cache, pour respecter la limite du plan AlphaVantage (25 requêtes par jour, 5 par minute).
  - L'indisponibilité d'un fournisseur est signalée à l'utilisateur, sans bloquer le reste de l'application.
- **NFR-INT-03 — Mêmes fournisseurs.** Les fournisseurs sont ceux du legacy : Azure OpenAI, Powens, AlphaVantage.

### 7.5 Performance

Les temps du legacy sont une référence mesurée, pas une cible (brief). Cibles au 95e centile, en local, avec les adaptateurs simulés :

- **NFR-PERF-01** : pages de navigation inférieures à 200 ms ; pages à calcul léger (relevé, budget, bénéficiaires) inférieures à 500 ms ; page bourse inférieure à 3 000 ms ; téléchargement PDF inférieur à 2 000 ms. [ASSUMPTION A-G-04 : ces valeurs, recommandées par `docs/retro/00-global/NFR.md`, sont adoptées comme cibles]

### 7.6 Traitements planifiés

- **NFR-BATCH-01** : les traitements planifiés gardent les horaires du legacy :
  - calcul des intérêts tous les jours à 02h00 ;
  - détection de fraude nocturne tous les jours à 02h30 ;
  - relevés mensuels le 1er du mois à 03h00 ;
  - virements programmés toutes les 5 minutes ;
  - contrôle des soldes faibles et des seuils toutes les 30 minutes.
- **NFR-BATCH-02** : chaque exécution d'un traitement planifié est enregistrée dans un journal d'exécution (heure, résultat).
- **NFR-BATCH-03** : le fuseau horaire des traitements est configuré explicitement. [ASSUMPTION A-G-07 : Europe/Paris ; le legacy ne le configure pas (`docs/retro/GAPS.md`, BATCH.md)]

### 7.7 Observabilité

- **NFR-OBS-01** : chaque requête et chaque message entre services est tracé de bout en bout, selon l'architecture de tracing imposée (addendum, §A.1). La preuve est NFR-QG-09.

### 7.8 Langue et interface

- **NFR-UI-01** : l'interface est en français. Les libellés et les messages affichés sont repris à l'identique du corpus (`docs/retro/00-global/MESSAGES.md` et `MESSAGES.md` de chaque fonctionnalité), sauf correction d'un bug à corriger.
- **NFR-UI-02** : chaque champ et chaque bouton actionné par un scénario du golden master porte un attribut de test stable. [ASSUMPTION A-G-08 : exigence posée ici pour rendre le rejeu possible ; la forme exacte est décidée en architecture]

## 8. Indicateurs de succès

**Principaux**
- **SM-1** : rejeu du golden master. Les 31 scénarios applicables passent, lors du rejeu final unique. Valide NFR-PAR-01 et l'ensemble du §4.
- **SM-2** : couverture des règles. 100 % des règles de gestion reprises sont citées par une FR (mesure : `docs/retro/tools/check-prd-coverage.py`) et couvertes par un test. Valide NFR-PAR-03.
- **SM-3** : bugs corrigés. 50 sur 50 bugs à corriger ont un test vert du comportement attendu. Valide NFR-PAR-02.

**Secondaires**
- **SM-4** : contrôles. Tous les contrôles bloquants de §7.1 sont verts sur la branche principale à la fin de chaque story. Valide NFR-QG-00 à NFR-QG-09.
- **SM-5** : performance. Les cibles NFR-PERF-01 sont tenues sur le parcours UJ-1.

**Contre-indicateurs (à ne pas optimiser)**
- **SM-C1** : nombre de stories livrées par epic. Accélérer au prix d'un contrôle rouge ou contourné est un échec. Contrebalance SM-4.
- **SM-C2** : taux de couverture au-delà des seuils. Il ne sert à rien d'écrire des tests sans assertion pour atteindre 100 % ; la mutation (NFR-QG-05 et NFR-QG-06) en est le garde-fou. Contrebalance SM-2.
- **SM-C3** : rejeux intermédiaires du golden master. Rejouer en cours de route ne prouve rien sur une application partielle et pousse à adapter les tests. Contrebalance SM-1.

## 9. Questions ouvertes

### QO-1 — Contrôle de fraude d'un virement (RG-027-003, BUG-084) — BLOQUANTE

**Acquis :** le contrôle de fraude s'exécute **avant la validation finale** du virement, et **toute erreur du contrôle est journalisée**.

**Non tranché, à décider par le métier avant l'architecture du flux de virement :**

**(a) Quand le contrôle détecte une suspicion, le virement est-il bloqué, ou seulement signalé ?**

| Option | Ce qu'elle implique |
|---|---|
| **a1 — Bloqué** (ou suspendu en attente de vérification) | Le contrôle est synchrone et décide du sort du virement. Il faut un statut « bloqué ou suspendu », sans débit ni crédit tant que l'administrateur n'a pas statué. L'administrateur a besoin d'une action « libérer ou rejeter ». Le client reçoit une notification FRAUDE au lieu de CONFIRMATION (BUG-007), et son parcours affiche un état « en vérification ». La règle de suspension au-delà du seuil de fraude (BUG-076) devient un cas particulier de cette option. Le contrôle est sur le chemin critique : sa latence s'ajoute à celle du virement. |
| **a2 — Seulement signalé** | Le virement est exécuté. Le contrôle produit une alerte de fraude consultable par l'administrateur, sans effet sur le solde. Le contrôle peut rester sur le chemin critique (il s'exécute avant la validation) sans en décider l'issue. Il faut alors préciser l'articulation avec BUG-076, qui exige une suspension au-delà de 10 000 € : soit BUG-076 reste une exception explicite, soit il entre en conflit avec a2. |

**(b) Quand le contrôle tombe en erreur, que veut dire « ne pas se poursuivre silencieusement » ?**

| Option | Ce qu'elle implique |
|---|---|
| **b1 — Virement refusé** | Fail-closed : une panne du contrôle de fraude bloque tous les virements. Le client voit un message d'indisponibilité. La disponibilité du service de fraude conditionne celle des virements, ce qui demande un dimensionnement et une surveillance du contrôle. |
| **b2 — Virement exécuté avec alerte** | Fail-open : le virement passe, et une alerte est créée pour l'administrateur, avec l'erreur journalisée. Les virements restent disponibles, mais une fraude peut passer pendant une panne. Il faut une alerte d'exploitation, et peut-être un rattrapage par le traitement nocturne (FEAT-019). |

Exigences suspendues : toutes celles marquées `[EN ATTENTE RG-027-003 (a)]`, `(b)` ou `(a)(b)` en §4.

*Note : le corpus est incohérent sur ce point. `docs/retro/00-global/LEGACY-BUGS.md` présente BUG-084 comme « validé par le métier », mais sa note dit qu'il dépend de la question bloquante FEAT-027 (« le contrôle fraude doit-il bloquer le virement ? »), et cette question n'est pas reportée dans `docs/retro/GAPS.md`.*

### Autres questions ouvertes (non bloquantes)

- **QO-2** : les questions ouvertes du brief non tranchées, et traitées en §4 par une supposition (état `DEFAUT` des prêts, compteur de téléchargements, bouton « Rafraîchir » des comptes tiers, méthodes de catalogue sans appelant, `evaluateSingleScore`). À confirmer par le métier. Voir l'index §10.
- **QO-3** : valeur de production du connecteur Powens (`POWENS_CONNECTOR_ID`). Sans objet pour le démonstrateur, qui tourne avec l'adaptateur simulé par défaut.


## 10. Index des suppositions

Chaque `[ASSUMPTION]` du document, classée de la plus risquée à la moins risquée. Le risque mesure ce qui arrive si la supposition est fausse : correction de sécurité ou de comportement métier, texte comparé par le golden master, ou simple détail. Deux suppositions globales, A-G-01 et A-G-02, n'apparaissent qu'ici : elles couvrent des choix appliqués à tout le §4.

### Risque élevé

- **A-G-01** (§4, global) : les 405 règles de statut « À confirmer » du corpus sont reprises telles quelles, comme si elles étaient confirmées. Si le métier en invalide une partie, les FR concernées changent.
- **A-G-02** (§4, global) : les règles de nature « dette legacy » qu'aucun bug à corriger ne couvre ne sont pas reproduites et ne sont remplacées par rien. Conséquence : aucune FR ne fixe de longueur maximale pour le motif, le numéro de compte et le libellé du bénéficiaire (RG-004-070 à RG-004-072), ni le comportement d'un motif de prêt de plus de 255 caractères (RG-007-016), ni les contrôles de l'année, du mois et du SIRET des documents (RG-008-016, RG-008-017, RG-017-006, RG-017-015).
- **A-018-1** (FEAT-018) : les intérêts mensuels sont crédités lors de l'exécution de 2 h du 1er jour de chaque mois, et rien les autres nuits. BUG-064 exige « une fois par mois » sans dire quel jour.
- **A-027-2** (FEAT-027) : le signal MULTIPLE_FAILED_AUTH (règle Suspect RG-027-017) est évalué à la connexion, au 5e échec consécutif. Il ne s'ajoute pas au score d'un virement. C'est une capacité que le legacy n'avait pas.
- **A-030-1** (FEAT-030) : les écrans publics sont exemptés d'authentification sur leur chemin exact, et non pour toute adresse qui contient leur nom. C'est une correction de sécurité qu'aucun bug déclaré ne couvre.
- **A-021-3** (FEAT-021) : un virement programmé refusé parce que son compte destinataire est introuvable passe à l'état suspendu. Le corpus ne dit pas ce qu'il devient.
- **A-014-1** (FEAT-014) : un retour du fournisseur d'open banking qui ne porte qu'un `code` est traité comme incomplet, tant que le parcours n'est pas confirmé auprès de Powens.

### Risque moyen

- **Textes absents du corpus**, que le golden master ou les tests compareront. Chacun doit être validé avant le rejeu :
  - A-012-1 : retour Powens en erreur.
  - A-012-4 : message après un échec de chargement des comptes.
  - A-016-1 : erreur de traitement d'un prêt.
  - A-021-1 : refus d'un virement programmé, compte introuvable.
  - A-023-1 : échec de remise d'un virement.
  - A-024-1 : service de crédit indisponible.
  - A-004-3 : notification de solde insuffisant d'un virement programmé.
  - A-004-4 : « Compte destinataire introuvable ».
- **Textes du legacy non reproduits ou arbitrés.** Des écarts sont attendus au rejeu du golden master, à traiter à l'étape G2 :
  - A-001-3, A-002-4, A-003-6, A-011-2, A-018-3 : bandeaux et mentions techniques supprimés ;
  - A-007-2 : « En attente » plutôt que « En cours » ;
  - A-002-2 : format des montants, deux rendus dans le corpus.
- **A-G-09** (addendum A.2) : les scénarios du golden master des fonctionnalités qui appellent un fournisseur externe se rejouent contre les adaptateurs simulés. SC-010-02 demande un simulateur qui renvoie un refus d'authentification.
- **A-G-04** (§7.5) : les temps recommandés par `docs/retro/00-global/NFR.md` sont adoptés comme cibles au 95e centile.
- **A-001-1** (FEAT-001) : un compte désactivé est refusé avec le message générique de mauvais identifiants, et une entrée d'audit LOGIN_FAILED.
- **A-001-2** (FEAT-001, FEAT-026) : l'adresse IP transmise par un en-tête n'est retenue que si elle vient d'un relais de confiance.
- **A-008-1, A-017-1** (documents) : l'accès au document d'un autre client est refusé comme un accès interdit (HTTP 403), sans message métier.
- **A-010-1** (FEAT-010) : « erreur d'authentification » du service d'IA couvre 401 et 403.
- **A-011-1** (FEAT-011) : le message « aucun produit » s'affiche par type de produit vide, comme le dit la règle, et non seulement quand tout le catalogue est vide, comme dans le legacy.
- **A-012-5** (FEAT-012) : un bouton « Rafraichir » est exposé. Il n'existe pas à l'écran dans le legacy (question ouverte du brief).
- **A-012-6, A-014-2, A-028-1** (Powens) : aucune attente imposée avant la lecture des comptes, contrairement à la pause bloquante du legacy.
- **A-016-2** (FEAT-016) : « Client depuis » est la date de création de la fiche client, affichée pour tout client.
- **A-018-2** (FEAT-018) : une relance dans le même mois ne recrédite pas d'intérêts.
- **A-021-4** (FEAT-021) : un virement programmé en retard de plus d'une période ne s'exécute qu'une fois par passage, puis rattrape aux passages suivants.
- **A-022-2** (FEAT-022) : contenu de l'entrée du journal d'exécution, y compris pour une exécution interrompue.
- **A-023-2** (FEAT-023) : un virement refusé faute de remise au traitement est entièrement annulé (enregistrement, débit, crédit, opérations).
- **A-024-2** (FEAT-024) : une demande de prêt non transmise est annulée et n'apparaît pas dans la liste.
- **A-025-1, A-025-2** (FEAT-025) : la notification de solde faible est de type ALERTE, soumise au même plafond, et n'est émise qu'une fois tant que le solde reste sous 100 €.
- **A-004-2** (FEAT-004) : une seule notification par échéance de virement programmé non exécutée.
- **A-027-1** (FEAT-027) : le signal d'heure inhabituelle utilise le fuseau de l'application, pas celui du client.
- **A-029-1** (FEAT-029) : sans clé AlphaVantage, aucun appel réel, et la page se comporte comme une absence de données.
- **A-029-2** (FEAT-029) : un symbole ou un nom trop long est bloqué par la limite du champ, sans message.
- **A-002-3** (FEAT-002) : un client sans compte COURANT voit un tableau d'opérations vide.

### Risque faible

- **A-G-03** (§7.1) : moment d'exécution de `trace-smoke.sh`, non précisé dans `docs/quality-gates.md`.
- **A-G-05** (§2.2, addendum) : l'API REST est interne au front.
- **A-G-06** (§2.3) : les parcours utilisateurs sont illustratifs.
- **A-G-07** (§7.6) : fuseau des traitements planifiés, Europe/Paris.
- **A-G-08** (§7.8) : attribut de test stable exigé sur les éléments actionnés par le golden master.
- **A-002-1** (FEAT-002) : un utilisateur supprimé pendant sa session est déconnecté et renvoyé vers la connexion.
- **A-003-1, A-003-2** (FEAT-003) : messages non techniques pour un montant invalide et pour un solde insuffisant.
- **A-003-3** (FEAT-003) : les contrôles de saisie et de solde passent avant le contrôle de fraude.
- **A-003-4** (FEAT-003, FEAT-027) : un seul signal LARGE_AMOUNT par virement, de score 40 entre 5 000 € inclus et 15 000 € exclus, de score 80 à partir de 15 000 €. RG-003-025 n'a pas de borne haute, contrairement à RG-027-004 : l'écart est visible dans la FR.
- **A-003-5** (FEAT-003, FEAT-027) : RAPID_SUCCESSION compare au précédent virement, alors que RG-003-032 dit « transaction » et RG-027-011 dit « virement ».
- **A-004-1, A-013-1** : messages de refus génériques sur la ressource d'un autre client.
- **A-004-5** (FEAT-004) : la correction de BUG-069 s'applique aussi à RG-004-064.
- **A-005-1** (FEAT-005) : signe moins en tiret ASCII.
- **A-007-1** (FEAT-007) : une durée de 301 à 360 mois est refusée avec « Duree invalide ».
- **A-010-2** (FEAT-010) : le champ du chat n'est pas vidé après l'envoi, comme dans le legacy.
- **A-012-2, A-012-3** (FEAT-012) : « Solde indisponible » et message d'indisponibilité de Powens.
- **A-014-3** (FEAT-014) : la remise à zéro au retour de Powens efface aussi le message d'erreur.
- **A-019-1** (FEAT-019) : une exécution par jour calendaire, y compris aux changements d'heure.
- **A-021-2** (FEAT-021) : le refus « compte introuvable » passe avant le report pour solde insuffisant.
- **A-021-5** (FEAT-021) : « isoler l'échec » veut dire annuler entièrement l'exécution en échec.
- **A-022-1** (FEAT-022) : le plafond de 20 alertes de solde faible porte sur le jour calendaire, heure de Paris.
- **A-027-3** (FEAT-027) : texte de la description de l'alerte MULTIPLE_FAILED_AUTH.

*Hors index, par décision : la question QO-1 (RG-027-003, points (a) et (b)) n'est couverte par aucune supposition.*
