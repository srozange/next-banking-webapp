### FEAT-018 — Calcul des intérêts nocturnes

**Description :** Traitement automatique de nuit, sans écran propre, qui crédite les intérêts des comptes d'épargne (type EPARGNE) au taux annuel de 0,5 %, soit 0,5 % / 12 par mois, une seule fois par mois. Chaque crédit crée une opération au crédit visible dans le relevé du client. Chaque exécution est journalisée (succès ou échec) sous le nom « INTEREST_CALCULATION », et les ADMIN consultent ce journal dans la section des traitements nocturnes de l'écran d'administration. Cas limites marquants : intérêt nul ou négatif (compte ignoré), erreur pendant le traitement (exécution journalisée en échec), le legacy créditait chaque nuit le taux mensuel (environ 30 fois trop d'intérêts), corrigé ici. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-018-01 : Déclenchement mensuel du crédit d'intérêts

Le traitement de calcul des intérêts, nommé « INTEREST_CALCULATION », se déclenche automatiquement à 2 h du matin. Il applique le taux mensuel de 0,5 % / 12 au solde de chaque compte EPARGNE une seule fois par mois civil, et non à chaque nuit [ASSUMPTION A-018-1 : le crédit a lieu lors de l'exécution de 2 h du matin du premier jour de chaque mois ; les autres nuits, le traitement ne crédite rien] [ASSUMPTION A-018-2 : si le traitement est relancé dans un mois où les intérêts ont déjà été crédités, aucun compte n'est crédité une seconde fois]. Cette règle remplace le comportement legacy RG-018-003, où le taux mensuel était appliqué chaque nuit par un déclenchement quotidien (RG-018-001).

Critère d'acceptation (test) : pour un compte EPARGNE de solde 12 000,00 EUR, après une simulation de toutes les exécutions nocturnes d'un mois civil complet, le compte a reçu exactement une opération « Interets mensuels livret epargne (batch nocturne) » de 5,00 EUR (12 000 × 0,005 / 12), et son solde vaut 12 005,00 EUR.

Source : RG-018-001, RG-018-003, BUG-064

#### FR-018-02 : Comptes concernés et calcul de l'intérêt

Le traitement sélectionne tous les comptes de type EPARGNE. L'intérêt de chaque compte est arrondi à 2 décimales, au plus proche, la moitié étant arrondie vers le haut. Si l'intérêt calculé est nul ou négatif, le compte est ignoré : son solde n'est pas modifié et aucune opération n'est créée.

Source : RG-018-002, RG-018-004, RG-018-005

#### FR-018-03 : Crédit du compte et opération d'intérêts

Quand des intérêts sont crédités sur un compte EPARGNE, son solde augmente du montant des intérêts, et une opération au crédit est enregistrée avec ce montant, le solde résultant du compte, la date et l'heure du crédit, et le libellé « Interets mensuels livret epargne (batch nocturne) ».

Source : RG-018-006, RG-018-007

#### FR-018-04 : Journal d'exécution du traitement

Quand le traitement se termine sans erreur, il enregistre une entrée de journal des traitements avec le nom « INTEREST_CALCULATION », le statut « SUCCESS », l'heure de début, l'heure de fin, le nombre de comptes EPARGNE évalués (y compris ceux ignorés) et le détail « Interets calcules sur {N} comptes epargne ». Si une erreur survient pendant le traitement, il enregistre une entrée avec le nom « INTEREST_CALCULATION », le statut « FAILED » et le détail « Erreur: {message} », et journalise l'erreur au niveau ERROR dans les journaux techniques. [ASSUMPTION A-018-4 : les nuits où aucun intérêt n'est dû (A-018-1), le traitement enregistre une entrée SUCCESS avec 0 enregistrement et le détail « Interets calcules sur 0 comptes epargne »]

Source : RG-018-008, RG-018-009

#### FR-018-05 : Consultation des exécutions par l'ADMIN

Un utilisateur ADMIN voit, dans la section des traitements nocturnes de l'écran d'administration, les exécutions récentes des traitements, dont celles de « INTEREST_CALCULATION », triées par heure de début décroissante, avec les colonnes « Job », « Debut », « Fin », « Duree (ms) », « Enregistrements », « Statut » et « Details », et un bouton « Rafraichir ». S'il n'y a aucune exécution, la section affiche « Aucun traitement nocturne enregistre ».

Source : RG-018-010

**Notes :**
- RG-018-003 (Suspect, dette legacy) : remplacée par FR-018-01 (correction BUG-064).
- Le titre legacy de la section d'administration contient une mention technique ; il est affiché « Traitements nocturnes » [ASSUMPTION A-018-3 : titre sans le suffixe technique].
- Le libellé « Interets mensuels livret epargne (batch nocturne) » est conservé à l'identique : il apparaît dans le relevé du client.
