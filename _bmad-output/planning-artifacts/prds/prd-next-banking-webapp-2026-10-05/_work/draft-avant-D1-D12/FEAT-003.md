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
