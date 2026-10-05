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
