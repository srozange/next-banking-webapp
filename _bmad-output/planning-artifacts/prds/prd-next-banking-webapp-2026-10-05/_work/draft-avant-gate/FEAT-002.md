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

#### FR-002-10 : Une seule notification par virement traité [ÉVOLUTION — décision RG-027-003 D1, D3]

Lorsqu'un virement est traité, le système produit exactement une notification pour l'émetteur, de type CONFIRMATION, ALERTE ou FRAUDE, jamais une combinaison de ces types. Le type suit d'abord l'issue du contrôle de fraude (FR-027-02), puis le montant, avec le seuil d'alerte (5 000 EUR par défaut) et le seuil de fraude (10 000 EUR par défaut) de configuration :
- virement suspendu (score cumulé strictement supérieur à 75, ou montant supérieur ou égal au seuil de fraude) : FRAUDE, avec l'avis de suspension de FR-027-04 ;
- virement exécuté d'un montant strictement inférieur au seuil d'alerte : CONFIRMATION, « Virement de {montant} EUR vers le compte {numCompte} confirme », ou « Virement de {montant} EUR vers le compte {numCompte} confirme (motif : {motif}) » si un motif est saisi ;
- virement exécuté d'un montant supérieur ou égal au seuil d'alerte : ALERTE, « Virement de {montant} EUR en attente de validation - transaction importante, verification en cours. » (voir FR-003-10).

Un virement refusé pour erreur du contrôle ne produit aucune notification : le refus est affiché à l'écran (FR-003-27). [ASSUMPTION A-003-8 : aucune notification pour un refus pour erreur du contrôle, non décidé en table ronde] La libération, le rejet ou le rejet automatique d'un virement suspendu ne produisent pas de nouvelle notification ; le client voit le nouvel état du virement (FR-027-16). La règle lui est annoncée dans l'unique notification de suspension (FR-027-04). C'est une décision du décideur, prise lors de la finalisation du 2026-10-05. Cette exigence remplace le comportement legacy RG-002-024 (plusieurs notifications possibles pour un même virement).

Critère d'acceptation (test) : un virement exécuté sous le seuil d'alerte, un virement exécuté entre les deux seuils, un virement suspendu par le score et un virement suspendu par le montant ajoutent chacun exactement une notification, du type attendu ; un virement refusé pour erreur du contrôle n'en ajoute aucune ; la libération ou le rejet d'un virement suspendu n'en ajoute aucune.

Source : RG-002-021, RG-002-022, RG-002-023, RG-002-024, BUG-007, RG-027-003

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
