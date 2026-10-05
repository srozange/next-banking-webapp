### FEAT-003 — Effectuer un virement

**Description :** Le formulaire de virement permet à un utilisateur authentifié de virer un montant d'un de ses comptes vers un autre de ses comptes. L'écran est ouvert à tous les rôles, mais seuls les comptes de l'utilisateur connecté sont proposés : un CONSEILLER ou un ADMIN, sans compte propre, voit des listes vides et ne peut rien virer. Le virement est soumis sans étape de confirmation. Une fois exécuté : débit et crédit simultanés, deux opérations au relevé, statut SUCCESS ou PENDING selon le seuil de mise en attente (affiché comme « transaction importante », décision B1), journal d'audit, notification dans l'application, puis retour au tableau de bord. Chaque virement passe par le contrôle de fraude en temps réel (FEAT-027) avant sa validation finale : il est exécuté, suspendu avant tout débit en attente de la revue de l'administrateur, ou refusé si le contrôle tombe en erreur (décision RG-027-003, D1 à D12). Cas limites marquants : comptes non sélectionnés ou identiques, montant nul ou négatif, solde insuffisant (désormais un message clair), virement de 5 000 € et plus (exécuté, « transaction importante »), de 10 000 € et plus (suspendu avant tout débit, notification FRAUDE). Scénarios golden master : SC-003-01, SC-003-02, SC-003-03, SC-003-04.

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

#### FR-003-08 : Soumission sans étape de confirmation

Aucune étape de confirmation n'est demandée à l'utilisateur : la soumission du formulaire déclenche directement le traitement du virement. L'issue du virement (exécuté, suspendu ou refusé pour erreur du contrôle) est fixée par FR-027-02.

Source : RG-003-009

#### FR-003-09 : Débit, crédit et opérations au relevé [ÉVOLUTION — décision RG-027-003 D1, D3]

Un virement n'est débité et crédité qu'au moment de son exécution : immédiatement si le contrôle de fraude le laisse passer (FR-027-02), ou après sa libération par l'administrateur (FR-016-19). L'exécution débite le compte source et crédite le compte destinataire du montant, de façon atomique (les deux ou aucun). Elle enregistre une opération DEBIT sur le compte source (« Virement vers {compte destinataire} - {motif} ») et une opération CREDIT sur le compte destinataire (« Virement depuis {compte source} - {motif} »), chacune avec le solde après opération, ainsi que le virement lui-même avec son horodatage ; un virement au statut PENDING porte en plus, en tête de ces descriptions, la mention « [TRANSACTION IMPORTANTE] » (FR-003-10, décision B1), à la place de la mention legacy « [EN ATTENTE] ». Un virement suspendu, rejeté ou refusé pour erreur du contrôle ne modifie aucun solde et n'enregistre aucune opération au relevé. Cette exigence remplace RG-003-010 et RG-003-011 (débit et crédit dès la soumission) : un virement suspendu n'est débité qu'à sa libération.

Critère d'acceptation (test) : un virement exécuté produit exactement un débit, un crédit et deux opérations ; un virement suspendu, rejeté ou refusé pour erreur laisse les deux soldes inchangés et n'ajoute aucune opération.

Source : RG-003-010, RG-003-011, RG-003-012, RG-003-013, RG-027-003

#### FR-003-10 : Virements importants exécutés (statut PENDING) [ÉVOLUTION — décision RG-027-003 D1, D2] [ÉVOLUTION — décision de finalisation B1]

Un virement exécuté d'un montant supérieur ou égal au seuil de mise en attente (configuration de déploiement, 5 000 € par défaut) prend le statut PENDING ; un virement exécuté d'un montant strictement inférieur prend le statut SUCCESS. PENDING est un statut d'affichage de l'état EXÉCUTÉ (FR-027-16) : le virement est exécuté et débité immédiatement, et n'attend aucune revue (décision D2). Seul un virement à l'état SUSPENDU attend une revue, avant tout débit (FR-027-02). Écart à RG-003-014 : un virement de 10 000 € ou plus, ou de score cumulé strictement supérieur à 75, n'est pas PENDING mais SUSPENDU sans débit (décisions D1, D2).

Les textes affichés pour un virement PENDING disent « transaction importante », sans « en attente » ni « vérification » (décision B1, correction de BUG-009) :
- notification ALERTE : « Virement de {montant} EUR vers le compte {numCompte} execute - transaction importante. » ;
- mention en tête des descriptions des opérations au relevé (FR-003-09) : « [TRANSACTION IMPORTANTE] », à la place de « [EN ATTENTE] ».

[ASSUMPTION A-003-10 : formulation exacte de ces deux textes, sans accents dans le style du legacy, proposée au gate et non arbitrée mot à mot ; le statut enregistré garde la valeur technique PENDING, qui n'est affichée au client sous aucune forme « en attente »] L'écart de texte avec le golden master est voulu (décision B1) : un scénario qui capture « [EN ATTENTE] » ou l'ancien texte ALERTE est traité à l'étape G2. Cette exigence remplace le comportement legacy RG-003-014 (virement présenté « en attente » alors qu'il est débité, BUG-009).

Critère d'acceptation (test) : un virement de 5 000,00 € de score cumulé inférieur ou égal à 75 prend le statut PENDING, débite le compte source, produit la notification ALERTE au texte « … execute - transaction importante. » ci-dessus et des opérations dont la description commence par « [TRANSACTION IMPORTANTE] » ; aucun texte affiché ne contient « en attente » ni « verification » ; un virement de 4 999,99 € prend le statut SUCCESS ; un virement de 10 000,00 € est à l'état SUSPENDU et ne débite rien.

Source : RG-003-014, RG-003-015, BUG-009, RG-027-003

#### FR-003-11 : Journal d'audit de l'exécution du virement

Quand un virement passe à l'état EXÉCUTÉ, immédiatement à la soumission ou après sa libération, le système enregistre une entrée d'audit d'exécution avec l'identifiant de l'émetteur, le montant et le motif (code d'action de FR-026-03). La soumission, la libération, le rejet et le rejet automatique ont leurs propres entrées (FR-026-02, FR-026-03).

Source : RG-003-016

#### FR-003-12 : Retour au tableau de bord

Après l'exécution d'un virement, le système affiche le tableau de bord, où les soldes sont à jour. Pour un virement suspendu ou refusé pour erreur du contrôle, l'information affichée suit FR-027-04 et FR-003-27.

Source : RG-003-017

#### FR-003-13 : Remise au traitement asynchrone [ÉVOLUTION — décision RG-027-003 D1, D5]

Seul un virement exécuté, immédiatement ou après libération, est remis au traitement asynchrone des virements (FR-023-01) ; cette remise ne produit aucune notification, la notification unique du virement étant produite à la décision du contrôle (FR-002-10). Un virement suspendu rejoint la file de revue de l'administrateur (FR-016-19) ; un virement refusé pour erreur du contrôle n'est remis à aucun traitement. La remise est enregistrée avec la validation du virement, et son échec fait échouer la validation (FR-023-02, qui remplace la publication « au mieux » du legacy).

Source : RG-003-018, RG-027-003

#### FR-003-14 : Notification selon l'issue du contrôle et le montant [ÉVOLUTION — décision RG-027-003 D1, D2] [ÉVOLUTION — décision de finalisation B1]

L'utilisateur connecté reçoit dans l'application une seule notification par virement, produite au moment de la décision du contrôle de fraude et choisie selon l'issue du contrôle puis le montant, avec les textes et les seuils de FR-002-10 : FRAUDE pour un virement suspendu, CONFIRMATION pour un virement exécuté sous le seuil d'alerte (5 000 € par défaut), ALERTE « transaction importante » pour un virement exécuté à partir de ce seuil (FR-003-10). Un virement d'un montant supérieur ou égal au seuil de fraude (10 000 € par défaut) est toujours suspendu (FR-023-04). Un virement libéré puis exécuté ne reçoit pas de nouvelle notification, et un virement refusé pour erreur du contrôle n'en reçoit aucune (FR-003-27). Cette exigence remplace RG-003-019 et RG-003-021 pour un virement suspendu sur score : il reçoit FRAUDE quel que soit son montant (D2).

Source : RG-003-019, RG-003-021, RG-003-022, RG-027-003

#### FR-003-15 : Contrôle de fraude avant la validation finale [ÉVOLUTION — décision RG-027-003 D1 à D4]

Le contrôle de fraude en temps réel (FR-027-01) s'exécute pour chaque virement avant sa validation finale, et toute erreur de ce contrôle est journalisée. L'issue du virement (exécuté, suspendu ou refusé pour erreur du contrôle) est fixée par FR-027-02. Cette exigence remplace l'ordre legacy de RG-003-023 (contrôle après la validation, erreurs ignorées).

Source : RG-003-023

#### FR-003-16 : Fin de l'avertissement « N alerte(s) » [ÉVOLUTION — décision RG-027-003 D2, D6]

Le message legacy « Attention : {N} alerte(s) fraude détectée(s) sur ce virement. Votre conseiller a été notifié. » n'est plus affiché : un virement pour lequel des alertes sont enregistrées (score cumulé strictement supérieur à 75) est désormais suspendu (FR-027-02), et le client voit l'avis de suspension de FR-027-04. Aucun conseiller n'intervient dans la revue (FR-016-19). Un éventuel écart avec un scénario du golden master qui affichait ce message est traité à l'étape G2.

Source : RG-003-024

#### FR-003-17 : Signal LARGE_AMOUNT (règles voisines)

Le contrôle de fraude lève un signal LARGE_AMOUNT de score 40 pour un montant supérieur ou égal à 5 000 € et de score 80 pour un montant supérieur ou égal à 15 000 €. Écart dans le corpus : RG-003-025 lève le score 40 dès 5 000 € sans borne haute, alors que RG-027-004 le borne à moins de 15 000 € ; un montant de 15 000 € ou plus pourrait donc lever 40 et 80. [ASSUMPTION A-003-4 : un seul signal LARGE_AMOUNT par virement, de score 40 entre 5 000 € inclus et 15 000 € exclus, de score 80 à partir de 15 000 €, conformément à RG-027-004 et aux deux descriptions distinctes du legacy] Description de l'alerte : « Transaction amount {amount} exceeds the large threshold of {threshold} » (score 40) ou « Transaction amount {amount} exceeds the very-large threshold of {threshold} » (score 80).

Source : RG-003-025, RG-003-026, RG-027-004, RG-027-005

#### FR-003-18 : Signal HIGH_FREQUENCY sur les transactions du jour

Le contrôle de fraude lève un signal HIGH_FREQUENCY de score 50 si le nombre de transactions du client exécutées le jour courant dépasse 8 [ASSUMPTION A-027-11 : « transactions du client » désigne les opérations enregistrées au relevé des comptes du client, débits et crédits confondus, comme la requête legacy qui compte les transactions rattachées au client ; un virement entre deux comptes du même client compte donc pour deux]. Les transactions des jours précédents ne sont pas comptées. Cette exigence remplace le comportement legacy RG-003-027, qui comptait toutes les transactions depuis l'ouverture du compte. Même règle que FR-027-06.

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

#### FR-003-24 : Signal VELOCITY_CHECK (règles voisines)

Le contrôle de fraude lève un signal VELOCITY_CHECK de score 65 si le client a effectué 5 virements ou plus de plus de 500 € dans les 60 dernières minutes. Écart dans le corpus : RG-003-033 compte les « transactions » du client, RG-027-012 ses « virements » exécutés. [ASSUMPTION A-003-9 : l'assiette est celle des virements exécutés du client (RG-027-012), et non de toutes ses opérations au relevé (RG-003-033), comme A-003-5 pour RAPID_SUCCESSION] Même règle que FR-027-10.

Source : RG-003-033, RG-027-012

#### FR-003-25 : Enregistrement des alertes au-delà du score 75

Si la somme des scores de tous les signaux levés pour le virement dépasse strictement 75, le système enregistre toutes les alertes correspondantes. Même règle que FR-027-03 (une alerte par signal, au statut OPEN).

Source : RG-003-034

#### FR-003-26 : Motif dans la notification de confirmation (règles voisines)

Lorsque la notification CONFIRMATION est produite et que le motif est renseigné, le système l'ajoute au message : « Virement de {montant} EUR vers le compte {n°_compte_destinataire} confirme (motif : {motif}) ». Écart dans le corpus : RG-003-020 exige un motif « non vide », RG-023-007 un motif « non nul et non vide » ; le texte du legacy écarte aussi un motif composé uniquement d'espaces (MESSAGES de FEAT-025). Un motif absent, vide ou composé uniquement d'espaces donne le message sans motif de FR-003-14.

Source : RG-003-020, RG-023-007

#### FR-003-27 : Écran de refus pour erreur du contrôle [ÉVOLUTION — décision RG-027-003 D3, D9]

Quand un virement est refusé pour erreur du contrôle de fraude (FR-027-02), l'écran affiche au minimum : « Votre virement n'a pas pu être vérifié. Rien n'a été débité. Ce n'est pas un refus de votre banque. Vous pouvez refaire votre demande dans quelques minutes ; si cela persiste, contactez-nous avec la référence {référence}. », où la référence est l'identifiant de trace du virement. Le message ne mentionne ni la fraude ni aucun signal. Aucune notification n'est produite (A-003-8).

Critère d'acceptation (test) : avec un contrôle forcé en erreur, l'écran affiche ce texte, la référence affichée est l'identifiant de la trace du virement, aucun solde ne change et aucune notification n'est ajoutée.

Source : RG-003-023, RG-027-003

#### FR-003-28 : Reprise préremplie après un refus [ÉVOLUTION — décision RG-027-003 D12] [STORY SUIVANTE : livrée après le socle RG-027-003 et après le cumul glissant (FR-027-18)]

Après un refus pour erreur du contrôle (FR-003-27), le client peut refaire son virement depuis un formulaire prérempli avec le compte source, le compte destinataire, le montant et le motif saisis. [ASSUMPTION A-003-11 : liste des champs préremplis, non fixée en table ronde] Cette capacité ne fait pas partie du socle : d'ici sa livraison, le client ressaisit son virement.

Source : RG-027-003

**Notes :**
- RG-003-007, RG-003-014 et RG-003-027 (Suspect, dette legacy) sont couvertes par les FR de correction FR-003-06 (BUG-008, BUG-011), FR-003-10 (BUG-009, décision de finalisation B1 : textes « transaction importante ») et FR-003-18 (BUG-010) ; leur comportement legacy n'est pas reproduit.
- SC-003-03 capture le message technique legacy de solde insuffisant ; corrigé par FR-003-06 (BUG-011), son texte attendu change.
- Le motif dans la notification de confirmation est porté par l'exigence fusionnée FR-003-26 (RG-003-020 + RG-023-007).
- Les deux bandeaux techniques du legacy au-dessus du formulaire (pile technique de gestion du virement, publication sur une file nommée) et les messages de champ obligatoire du legacy, qui exposent des identifiants de composants internes, ne sont pas repris (NFR-SEC-04). [ASSUMPTION A-003-6 : ces textes sont exclus du golden master ; un champ obligatoire vide reçoit un message sans détail technique]
- Le corpus signale pour RAPID_SUCCESSION un risque d'accès concurrent dans le legacy (JIRA-8910), sans décision de correction ; FR-003-23 n'en fait pas une exigence.
