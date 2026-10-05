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

Chaque action administrative est enregistrée dans le journal d'audit avec son action et l'identifiant concerné : « ADMIN_LOAN_APPROVE » et l'identifiant du prêt pour une approbation, « ADMIN_LOAN_REFUSE » et l'identifiant du prêt pour un refus, « ADMIN_DEACTIVATE » et l'identifiant du client pour une désactivation, « ADMIN_ACTIVATE » et l'identifiant du client pour une activation. La libération et le rejet d'un virement suspendu (FR-016-19) sont aussi des actions administratives auditées, avec leurs propres codes (FR-026-02). L'entrée d'audit est enregistrée indépendamment de l'action : elle est conservée même si l'action principale échoue et est annulée.

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

Quand l'administrateur clique sur « Confirmer » sur une alerte OPEN, le système passe l'alerte au statut CONFIRMED ; sur « Faux positif », il la passe au statut FALSE_POSITIVE. Dans les deux cas, il enregistre comme auteur de la revue l'identifiant de l'administrateur connecté, comme FR-027-13 et FR-027-14, et l'horodatage de la revue ; le texte « Traité par {reviewedBy} » de FR-016-14 affiche le nom complet de cet administrateur. [ASSUMPTION A-016-4 : on enregistre l'identifiant (RG-027-015, RG-027-016) et on affiche le nom complet (RG-016-030, RG-016-031) ; les deux règles du corpus, qui se contredisaient sur la valeur enregistrée, sont ainsi conciliées] Une demande de confirmation ou de classement sans identifiant d'alerte est ignorée sans erreur.

La revue d'une alerte n'a aucun effet sur le virement qui l'a levée : confirmer une alerte ou la classer en faux positif ne libère ni ne rejette le virement suspendu ; seule l'action de FR-016-19 le fait, et elle passe alors les alertes encore OPEN de ce virement au statut REVIEWED (décision de finalisation B4).

Source : RG-016-030, RG-016-031, RG-016-032

#### FR-016-16 : Affichage de toutes les alertes fraude

Sans filtre de statut, le tableau des alertes fraude affiche toutes les alertes, quel que soit leur statut (OPEN, CONFIRMED, FALSE_POSITIVE, REVIEWED). Ce comportement remplace le comportement legacy RG-016-034, qui n'affichait que les alertes OPEN.

Critère d'acceptation (test) : avec une alerte OPEN, une CONFIRMED et une FALSE_POSITIVE en base, le tableau affiche les trois lignes ; les deux alertes traitées affichent « Traité par {reviewedBy} » et le badge affiche « 1 ouverte(s) ».

Source : RG-016-034, BUG-056

#### FR-016-17 : Journal des traitements périodiques

La section « Traitements nocturnes », en lecture seule, affiche les 50 entrées les plus récentes du journal d'exécution des traitements (colonnes « Job », « Debut », « Fin », « Duree (ms) », « Enregistrements », « Statut », « Details »). Si aucune entrée n'existe, elle affiche « Aucun traitement nocturne enregistre ».

Source : RG-016-035, RG-016-036

#### FR-016-18 : Catalogue des produits bancaires

La section « Catalogue produits bancaires », en lecture seule, affiche les produits bancaires actifs regroupés par catégorie : « Produits d epargne », « Produits de credit » et « Produits d investissement », avec un lien « Voir la page Produits » vers la page complète des produits. Une catégorie sans produit affiche « Aucun produit ».

Source : RG-016-037

#### FR-016-19 : Revue des virements suspendus [ÉVOLUTION — décision RG-027-003 D6, D7] [ÉVOLUTION — décision de finalisation B4, B5, B6, B7]

L'administrateur, et lui seul, dispose d'une file des virements suspendus, immédiats ou issus d'une échéance de virement programmé (FR-021-10). Pour chaque virement, elle affiche le client, le montant, le compte destinataire, le motif de la suspension (signaux et score cumulé, montant supérieur ou égal au seuil de fraude, ou dépassement du cumul glissant) et l'heure butoir de revue. [ASSUMPTION A-016-3 : file triée par heure butoir la plus proche, colonnes proposées en table ronde (Sally), non arbitrées] Il peut libérer ou rejeter un virement suspendu, avec un motif obligatoire ; son identité, la décision, le motif et l'heure sont enregistrés au journal d'audit (FR-026-02). À l'instant de la libération, le système contrôle le solde du compte source (décision de finalisation B6, FR-027-16) : si le solde est insuffisant, la libération est refusée immédiatement et le virement passe à l'état REJETÉ, sans aucun débit ; sinon, le virement passe à l'état LIBÉRÉ et est remis au traitement asynchrone des virements (FR-023-01, correction A1 du gate), qui effectue ensuite le débit et le crédit et le passe à l'état EXÉCUTÉ. Aucun autre rôle (CONSEILLER, CLIENT) ne peut libérer ni rejeter un virement.

Si le solde est devenu insuffisant entre la libération et le débit asynchrone, le virement est rejeté sans aucun débit (décision de finalisation B7, FR-027-16). L'administrateur voit ce rejet dans l'historique des virements traités de la section de revue, avec son motif en clair, « solde insuffisant au moment du débit », distinct d'un rejet par l'administrateur et d'un rejet à l'heure butoir. [ASSUMPTION A-016-7 : l'historique des virements traités liste, sous la file, les virements libérés ou rejetés avec leur état et le motif de leur issue ; il n'apparaît que s'il contient au moins un virement ; sa profondeur, son titre et ses colonnes ne sont pas fixés]

L'heure butoir fait foi (décision de finalisation B5, FR-027-17) : la libération n'est acceptée que strictement avant l'heure butoir. À l'heure butoir ou après, elle est refusée et le virement est traité comme rejeté (REJETÉ, sans débit), que le traitement planifié du rejet automatique soit passé ou non. [ASSUMPTION A-016-6 : un rejet demandé à l'heure butoir ou après est lui aussi sans objet : le virement est rejeté par expiration (FR-027-17) et non par l'administrateur ; dans les deux cas, l'administrateur voit « Heure butoir dépassée : le virement est rejeté. »]

Libérer ou rejeter un virement suspendu passe ses alertes de fraude au statut REVIEWED dans la même action, avec l'identifiant de l'administrateur comme auteur de la revue et l'heure de la décision (décision de finalisation B4 ; voir FR-016-15, FR-027-13 et FR-027-14). [ASSUMPTION A-016-5 : seules les alertes encore OPEN de ce virement passent à REVIEWED ; une alerte déjà CONFIRMED ou FALSE_POSITIVE garde son statut ; un virement suspendu par le seul montant ou par le cumul, sans alerte, n'en modifie aucune]

Critère d'acceptation (test) : un CONSEILLER ou un CLIENT qui tente de libérer ou de rejeter est refusé ; une libération ou un rejet sans motif est refusé ; une libération avec un solde suffisant passe le virement à l'état LIBÉRÉ sans débit immédiat, puis le traitement asynchrone le débite, le crédite et le passe à l'état EXÉCUTÉ ; une libération avec un solde devenu insuffisant est refusée, le virement passe à l'état REJETÉ, aucun débit n'est effectué et ses alertes OPEN passent au statut REVIEWED ; un rejet passe le virement à l'état REJETÉ sans débit ; après une libération comme après un rejet, les alertes OPEN du virement sont au statut REVIEWED avec l'identifiant de l'administrateur, dans la même action ; une libération à l'heure butoir moins 1 seconde est acceptée ; une libération à l'heure butoir est refusée et le virement est REJETÉ, même si le traitement planifié n'est pas encore passé ; chaque décision ajoute une entrée d'audit avec l'identité de l'administrateur et le motif ; une libération acceptée suivie d'un solde devenu insuffisant avant le débit asynchrone fait apparaître le virement dans l'historique des virements traités, à l'état REJETÉ, avec le motif « solde insuffisant au moment du débit ».

Source : RG-023-009, RG-027-003, BUG-076

**Notes :**
- RG-016-001 : couverte par l'exigence fusionnée de FEAT-008 (règles voisines).
- FR-016-01 est voisine de l'exigence fusionnée de FEAT-001 (RG-001-011 + RG-030-003, non-ADMIN sur l'administration) ; RG-016-002 ne figure pas dans la liste des fusions, elle reste portée ici. Les deux exigences doivent rester cohérentes (redirection vers le tableau de bord, sans message).
- Le bandeau legacy « Espace Administration — JEE 8 / JSF 2.3 / EJB 3.2 / RBAC custom — Role : ADMIN » cite des technologies : il n'est pas repris tel quel ; le titre d'écran et le badge « ADMIN » restent.
- Le comportement legacy de la date « Client depuis » (RG-016-025) n'est pas reproduit : FR-016-10 le remplace. L'énoncé de BUG-055 parle de la « fiche client du conseiller », mais la règle concernée porte sur le panneau de détail de l'administration ; le panneau de détail de l'espace conseiller (FEAT-015) n'affiche pas cette date.
- Après la correction de BUG-072 (FEAT-022), le traitement de surveillance des soldes faibles, exécuté toutes les 30 minutes, alimente le journal affiché en FR-016-17 : 48 entrées par jour peuvent repousser les traitements nocturnes hors des 50 entrées affichées.
