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
