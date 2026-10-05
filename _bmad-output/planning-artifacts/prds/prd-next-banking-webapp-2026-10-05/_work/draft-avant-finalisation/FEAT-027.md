### FEAT-027 — Détection de fraude en temps réel

**Description :** Contrôle transversal, sans écran propre côté client, appliqué à chaque virement soumis (FEAT-003) : huit signaux de fraude, chacun avec un score de risque, sont évalués ; au-delà d'un score cumulé de 75, une alerte par signal levé est enregistrée au statut OPEN. Un virement suspect est suspendu avant tout débit et le client reçoit un avis de suspension ; l'ADMIN consulte les alertes dans l'administration et les confirme (CONFIRMED) ou les classe en faux positif (FALSE_POSITIVE). Le contrôle s'exécute désormais avant la validation finale du virement et ses erreurs sont journalisées ; un virement suspect (score cumulé strictement supérieur à 75, ou montant supérieur ou égal au seuil de fraude) est suspendu avant tout débit, et un virement dont le contrôle tombe en erreur est refusé (décision RG-027-003, D1 à D12). Cas limites marquants : score cumulé exactement 75 (aucune alerte), heure du serveur hors 06:00-23:00, IBAN étranger, fréquence comptée sur le jour courant, échecs de connexion répétés. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-027-01 : Évaluation des huit signaux pour chaque virement

Pour chaque virement soumis par un client, le système évalue les huit signaux de fraude : LARGE_AMOUNT, HIGH_FREQUENCY, NEW_BENEFICIARY, UNUSUAL_HOURS, ROUND_AMOUNT, FOREIGN_IBAN, RAPID_SUCCESSION et VELOCITY_CHECK (FR-003-17, FR-003-21, FR-003-23 et FR-027-05 à FR-027-10).

Source : RG-027-001

#### FR-027-02 : Contrôle avant la validation finale et issue du virement [ÉVOLUTION — décision RG-027-003 D1 à D4]

Le contrôle de fraude s'exécute de façon synchrone, avant la validation finale de chaque virement soumis, et décide de l'une de trois issues :
- **exécuté** : score cumulé inférieur ou égal à 75 et montant strictement inférieur au seuil de fraude (10 000 € par défaut) ;
- **suspendu**, avant tout débit, en attente de la revue de l'administrateur (FR-016-19) : score cumulé strictement supérieur à 75, OU montant supérieur ou égal au seuil de fraude, OU, une fois FR-027-18 livrée, dépassement du cumul glissant ;
- **refusé pour erreur du contrôle**, quel que soit le montant : toute erreur du contrôle, y compris l'expiration du délai technique d'accès aux données d'historique (NFR-SEC-09), refuse le virement ; rien n'est débité, l'erreur est journalisée, et l'écran de FR-003-27 est affiché.

Aucun virement n'est exécuté sans que le contrôle ait rendu son issue. Cette exigence remplace le comportement legacy RG-027-003, où le contrôle s'exécutait après la validation du virement et où ses erreurs étaient ignorées.

Critère d'acceptation (test) : un virement de score cumulé 75 et de montant 9 999,99 € est exécuté ; un virement de score cumulé 76 est suspendu sans débit ; un virement de 10 000,00 € de score cumulé nul est suspendu sans débit ; pour chaque cause d'erreur simulée (entrée invalide, historique absent ou corrompu, expiration de l'accès aux données d'historique), le virement est refusé, aucun solde ne change, l'erreur figure dans le journal du serveur et la trace du contrôle porte un statut d'erreur.

Source : RG-027-003, RG-027-002, BUG-084

#### FR-027-03 : Enregistrement des alertes au-delà du score 75

Si la somme des scores de tous les signaux levés pour un virement dépasse strictement 75, le système enregistre une alerte par signal levé, au statut OPEN, avec son score et sa description. Un score cumulé de 75 ou moins n'enregistre aucune alerte. Même règle que FR-003-25.

Source : RG-027-002

#### FR-027-04 : Avis au client d'un virement suspendu [ÉVOLUTION — décision RG-027-003 D2, D6, D7]

Quand un virement est suspendu, le client en est informé par l'unique notification du virement (type FRAUDE, FR-002-10) et à l'écran : le virement est en cours de vérification, rien n'a été débité, et une réponse sera donnée avant une heure butoir précise (date et heure), égale à l'heure de soumission plus 24 heures (FR-027-17). Le texte n'évoque ni la fraude ni les signaux levés. [ASSUMPTION A-027-4 : texte « Votre virement de {montant} EUR vers le compte {numCompte} est en cours de verification. Rien n'a ete debite. Vous aurez notre reponse avant le {date} a {heure}. », proposé en table ronde (Sally) et non arbitré ; il remplace les textes legacy de la notification FRAUDE et de l'avertissement] Cette exigence remplace le message legacy de RG-027-013 (« Attention : {N} alerte(s) fraude détectée(s) sur ce virement. Votre conseiller a été notifié. »), qui annonçait à tort un conseiller (FR-016-19).

Critère d'acceptation (test) : un virement suspendu à 10 h 15 le 5 annonce une réponse avant 10 h 15 le 6 ; le texte ne contient ni « fraude » ni le nom d'un signal.

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

#### FR-027-16 : États d'un virement [ÉVOLUTION — décision RG-027-003 D5]

Un virement est dans l'un de cinq états :
- **EXÉCUTÉ** : débité et crédité, avec le statut d'affichage SUCCESS ou PENDING de FR-003-10 ;
- **SUSPENDU** : aucun débit, en attente de la revue de l'administrateur ;
- **LIBÉRÉ** : libéré par l'administrateur ; le système refait le contrôle du solde, puis passe le virement à EXÉCUTÉ, ou à REJETÉ si le solde est devenu insuffisant [ASSUMPTION A-027-6 : issue d'une libération sur solde insuffisant, non tranchée en table ronde] ;
- **REJETÉ** : rejeté par l'administrateur ou à l'expiration de la revue ; aucun débit ;
- **REFUSÉ_ERREUR_CONTRÔLE** : refusé parce que le contrôle de fraude est tombé en erreur ; aucun débit.

Transitions permises : à la soumission, vers EXÉCUTÉ, SUSPENDU ou REFUSÉ_ERREUR_CONTRÔLE ; de SUSPENDU vers LIBÉRÉ ou REJETÉ ; de LIBÉRÉ vers EXÉCUTÉ ou REJETÉ. EXÉCUTÉ, REJETÉ et REFUSÉ_ERREUR_CONTRÔLE sont terminaux ; toute autre transition est refusée. Le client voit l'état de ses virements suspendus, libérés ou rejetés. [ASSUMPTION A-027-5 : sur l'écran de virement et le tableau de bord, sans nouvelle notification]

Critère d'acceptation (test) : chaque transition permise est acceptée, chaque transition interdite est refusée, et aucun solde ne change hors du passage à EXÉCUTÉ.

Source : RG-027-003, BUG-084

#### FR-027-17 : Échéance de revue et rejet automatique [ÉVOLUTION — décision RG-027-003 D7, D8]

Un virement suspendu qui n'a été ni libéré ni rejeté 24 heures calendaires après sa soumission, sans calendrier de jours fériés, est rejeté automatiquement par un traitement planifié, sans débit ; ce rejet est tracé au journal d'audit. Le rejet automatique peut intervenir la nuit ou le week-end : c'est accepté pour le démonstrateur (risque R-2). Si une libération et l'expiration surviennent au même moment, une seule des deux s'applique (la première enregistrée), sans double débit.

Critère d'acceptation (test) : à 23 h 59 min 59 s après la soumission, le virement est toujours SUSPENDU ; à 24 h et à 24 h + 1 s, il est REJETÉ sans débit ; une libération et une expiration concurrentes produisent une seule transition et au plus un débit.

Source : RG-027-003, RG-023-009

#### FR-027-18 : Cumul glissant des virements d'un client [ÉVOLUTION — décision RG-027-003 D10] [STORY SUIVANTE : livrée par la story qui suit immédiatement le socle RG-027-003]

À chaque virement soumis, le système calcule le total des virements émis par le client sur les 24 heures glissantes qui précèdent, tous bénéficiaires confondus, en incluant ce virement. Au-delà du plafond (10 000 € par défaut, configuration de déploiement alignée sur le seuil de fraude de BUG-076), le virement qui fait franchir le plafond est suspendu (FR-027-02). [ASSUMPTION A-027-7 : le total compte les virements exécutés et suspendus, pas les virements rejetés ni refusés pour erreur] C'est une règle nouvelle, absente du corpus et du golden master. Tant qu'elle n'est pas livrée, le découpage d'un montant en virements sous les seuils reste possible (risque R-1).

Critère d'acceptation (test) : avec 6 000 € déjà émis dans les 24 heures, un virement de 4 000 € (total 10 000,00 €) n'est pas suspendu par le cumul, et un virement de 4 000,01 € l'est ; un virement émis il y a plus de 24 heures n'est pas compté.

Source : RG-027-003

**Notes :**
- RG-027-005 : couverte par l'exigence fusionnée de FEAT-003 (règles voisines), FR-003-17.
- RG-027-009 : couverte par l'exigence fusionnée de FEAT-003 (règles voisines), FR-003-21.
- RG-027-011 : couverte par l'exigence fusionnée de FEAT-003 (règles voisines), FR-003-23.
- RG-027-003, RG-027-006 et RG-027-018 (Suspect, dette legacy) sont couvertes par les FR de correction FR-027-02 (BUG-084), FR-027-06 (BUG-085) et FR-027-15 (BUG-086) ; leur comportement legacy n'est pas reproduit.
- RG-027-003 : le corpus porte, sous cette règle, une mention de résolution selon laquelle la détection « ne bloque pas le virement : elle le signale ». Cette mention n'est pas retenue : la décision RG-027-003 du 2026-10-05 (D1 à D12, voir §9) suspend un virement suspect avant tout débit.
- RG-027-017 (Suspect, nature métier) : reprise dans FR-027-11 comme prolongement de RG-027-002, conformément à la résolution du corpus (signal de score 80 après 5 échecs consécutifs).
- RG-027-019 (dette legacy) : non reproduite. BUG-087 (conservé, non corrigé) : le point d'entrée public d'évaluation d'un score isolé n'est ni repris ni supprimé par une exigence ; voir QO-2.
- Le statut REVIEWED existe dans les données de référence du legacy mais aucune action ne le produit ; il n'est atteint que par ces données.
- Les descriptions d'alertes sont en anglais dans le legacy alors que l'interface est en français ; le corpus le signale comme une dette à homogénéiser, sans bug à corriger. Les FR reprennent le texte affiché par le legacy.
