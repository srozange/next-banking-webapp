# FEAT-007 — Demande et suivi de prêts : Champs et composants UI

_Source : crawl MCP Playwright, rôles CLIENT / CONSEILLER / ADMIN, http://localhost:8080/banking-legacy/pret.xhtml_

---

## Section 1 — Simulateur de prêt

**Titre** : « Simuler un emprunt »

| Champ | Type | Libellé | Obligatoire | Valeur par défaut | Validation |
|---|---|---|---|---|---|
| `simMontant` | textbox | Montant (EUR) | Non | 10,000.00 | Numérique positif — erreur inline « Montant invalide » si vide ou non numérique |
| `simTaux` | textbox | Taux annuel (%) | Non | 3.50 | — |
| `simDuree` | textbox | Duree (mois) | Non | 36 | — |

**Action** : bouton « Calculer »

**Résultat dynamique** (apparaît après clic Calculer) :
- Mensualite (EUR)
- Cout total (EUR)
- Total interets (EUR)
- Panneau collapsible « Voir l'écheancier complet (N écheances) » → table : #, Capital, Intérêts, Total, Capital restant

---

## Section 2 — Demande de prêt

**Titre** : « Nouveau prêt »

| Champ | Type | Libellé | Obligatoire | Valeur par défaut | Options / Validation |
|---|---|---|---|---|---|
| `typePret` | combobox | Type de prêt | Non | Consommation | Immobilier, Automobile, Consommation, Travaux, Etudiant |
| `montant` | textbox | Montant (EUR) | **Oui** | — | Numérique — erreur inline « Montant invalide » si vide |
| `taux` | textbox | Taux annuel (%) | Non | 3.50 | — |
| `duree` | textbox | Duree (mois) | Non | 36 | — |
| `objetPret` | textbox | Objet du prêt | Non | — | Texte libre |

**Action** : bouton « Soumettre la demande »
- Envoi asynchrone sur `queue/pretDemandesQueue` (MDB WildFly)
- Message de confirmation : « Demande transmise au service crédit — traitement en cours (rechargez la page dans quelques secondes) »
- Le prêt apparaît dans la table avec statut « En attente d'approbation »

---

## Section 3 — Emprunts en cours

**Titre** : « Emprunts en cours »

**Table de liste** :

| Colonne | Contenu |
|---|---|
| Type | Type de prêt (ex. Travaux, Automobile, Consommation) |
| Montant | Montant en EUR (ex. EUR5,000.00) |
| Taux | Taux annuel (ex. 4.9000%) |
| Duree | Durée en mois (ex. 24 mois) |
| Mensualite | Mensualité calculée (ex. EUR218.63) |
| Total interets | Montant total des intérêts (ex. EUR247.12) |
| Statut | En cours / En attente d'approbation / En cours de traitement... |
| Echeancier | Bouton « Voir » (si disponible) ou texte « En cours de traitement... » |
| Action | Bouton « Supprimer » (suppression directe sans confirmation) |

**Statuts observés** : « En cours », « En attente d'approbation », « En cours de traitement... »

**État vide** : « Aucun emprunt enregistré » (texte affiché si aucun prêt)

**Action Voir (écheancier d'un prêt existant)** — affiche le panneau « Écheancier du prêt sélectionné » :

| Colonne | Contenu |
|---|---|
| # | Numéro d'échéance |
| Date échéance | dd/MM/yyyy |
| Capital | Montant capital remboursé |
| Intérêts | Montant intérêts |
| Total | Capital + Intérêts |
| Capital restant | Capital dû après échéance |
| Statut | Payé / En cours |
| Action | — (vide pour les échéances futures) |

**Action Supprimer** : suppression directe sans modale de confirmation — le prêt disparaît immédiatement de la table.

---

## Droits par rôle

| Rôle | Accès | Différences observées |
|---|---|---|
| CLIENT | Complet | Voit ses propres prêts |
| CONSEILLER | Identique CLIENT | Aucun prêt dans la démo (compte sophie vide) |
| ADMIN | Identique CLIENT | Aucun prêt dans la démo ; nav inclut « Espace Conseiller » et « Administration » |

**Pas d'état droits_réduits** : tous les rôles voient la même interface complète.

---

## Messages d'erreur et de confirmation

| Code | Message exact |
|---|---|
| ERR_MONTANT | « Montant invalide » (inline, dans une cellule de table) |
| CONF_SOUMISSION | « Demande transmise au service crédit — traitement en cours (rechargez la page dans quelques secondes) » |
| EMPTY_LOANS | « Aucun emprunt enregistré » |

---

## Activations dynamiques

| Composant | Condition d'apparition | Écran |
|---|---|---|
| Panneau résultat simulation (Mensualite, Cout total, Total interets) | Après clic « Calculer » | SCR-007-02_simulation.png |
| Tableau écheancier simulateur (collapsible) | Après clic « Voir l'écheancier complet » | SCR-007-03_echeancier_simulateur.png |
| Panneau « Echeancier du pret sélectionné » | Après clic « Voir » sur un prêt existant | SCR-007-04_echeancier_pret.png |
