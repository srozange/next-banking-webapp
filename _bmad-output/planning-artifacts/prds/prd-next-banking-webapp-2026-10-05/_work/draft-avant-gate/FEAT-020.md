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
