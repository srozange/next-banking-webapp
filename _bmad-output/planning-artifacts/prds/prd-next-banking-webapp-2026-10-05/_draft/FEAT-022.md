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

Le système crée au plus 20 notifications de solde faible par compte et par jour. Une fois ce plafond atteint, un compte resté sous le seuil ne génère plus de notification jusqu'au jour suivant ; ce plafond prime sur FR-022-02. C'est la même règle, et la même notification, que l'alerte de solde faible affichée au client (FR-025-08, décision de finalisation B3). Ce comportement remplace le comportement legacy RG-022-010, où le plafond déclaré n'était jamais appliqué (jusqu'à 48 notifications par jour). [ASSUMPTION A-022-1 : le « jour » est le jour calendaire, de 0 h à 24 h, heure de Paris.]

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
