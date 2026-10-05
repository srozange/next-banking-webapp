### FEAT-004 — Virements programmés

**Description :** L'écran « Virements programmes » permet à tout utilisateur authentifié (en pratique un CLIENT titulaire de comptes ; un CONSEILLER ou un ADMIN sans compte voit un formulaire sans compte source et une liste vide) de créer des virements récurrents depuis l'un de ses comptes, avec une fréquence parmi six (hebdomadaire à annuelle), puis de les suspendre, les réactiver ou les supprimer. Un traitement automatique exécute toutes les 5 minutes les virements actifs arrivés à échéance, chaque échéance passant par le contrôle de fraude (détaillé dans FEAT-021). Les cas limites marquants sont la date de première exécution saisie au format `yyyy-MM-dd` (différent des autres écrans, conservé), la date passée acceptée, le solde insuffisant à l'échéance (désormais notifié au client), le compte bénéficiaire qui n'est pas un compte de la banque (désormais refusé avant tout débit) et les actions sur le virement d'un autre client (désormais refusées). Scénarios golden master : SC-004-01, SC-004-02, SC-004-03.

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

Toutes les 5 minutes, le système recherche les virements programmés actifs dont la date de prochaine exécution est passée ou atteinte, et tente de les exécuter l'un après l'autre (détail en FR-021-01 à FR-021-03), chaque échéance passant par le contrôle de fraude du virement immédiat (FR-021-10, décision de finalisation B2). Lorsque le compte bénéficiaire est un compte de la banque et que le solde est suffisant, le système débite le compte source, crédite le compte bénéficiaire et enregistre une opération DEBIT et une opération CREDIT (libellés en FR-021-05 et FR-021-06), sauf si le solde est insuffisant (FR-004-12) ou si le compte bénéficiaire n'est pas un compte de la banque (FR-004-13).

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

Après une exécution réussie, le système enregistre l'horodatage de cette exécution, augmente de 1 le nombre d'exécutions et calcule la date de prochaine exécution à partir de la date d'échéance prévue et de la fréquence, comme en FR-021-08, et non à partir de l'heure réelle d'exécution comme le décrit RG-004-064. [ASSUMPTION A-004-5 : la correction du BUG-069 (FEAT-021) s'applique aussi à RG-004-064, qui décrit le même calcul.] Cette exigence remplace le comportement legacy RG-004-064 (correction BUG-069, voir FR-021-08).

Source : RG-004-064, RG-021-011, BUG-069

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
