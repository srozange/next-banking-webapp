# FIELDS — FEAT-016 Administration — Gestion globale

_Source : admin.xhtml — crawl rôle ADMIN — `extract-fields-from-dom.py` (31 éléments bruts)_
_Snapshots : ADMIN-charge.snap, ADMIN-modale-detail-client.snap, ADMIN-confirmation-desactiver.snap_

---

## Synthèse

| Catégorie | Nb |
|---|---|
| Champs de saisie | 1 |
| Boutons d'action (écriture) | 8 |
| Boutons de rafraîchissement (lecture) | 2 |
| Champs lecture seule (panneau détail) | 8 |
| Champs techniques (hidden JSF) | 20 |
| **Total champs métier** | **9** |

---

## Section 1 — Pipeline Prêts (En attente d'approbation)

Tableau lecture seule, alimenté par `AdminBean` / `LoanService`. Colonnes : ID, Client, Type, Montant, Durée, Taux, Mensualité, Motif.

| Champ | Type | Libellé | Obligatoire | Contraintes |
|---|---|---|---|---|
| (bouton) Approuver | submit | Approuver | — | Action directe (POST), sans modale de confirmation ; déclenche JMS → FEAT-024 |
| (bouton) Refuser | submit | Refuser | — | Action directe (POST), sans modale de confirmation ; message flash "Prêt refusé" |
| (bouton) Rafraîchir | submit | Rafraichir | — | Rechargement AJAX de la section |

**État vide** : "Aucun pret en attente" (texte affiché quand aucun prêt EN_ATTENTE).

---

## Section 2 — Gestion des clients

Tableau avec filtre de recherche. Colonnes : Nom, Login, Email, Rôle, Statut, Conseiller, Actions.

| Champ | Type | Libellé | Obligatoire | Contraintes |
|---|---|---|---|---|
| Filtre de recherche | text | (sans libellé visible) | Non | Recherche textuelle sur les clients ; soumis par le bouton "Filtrer" |
| (bouton) Filtrer | submit | Filtrer | — | Filtre la liste des clients selon la valeur saisie |
| (bouton) Detail | submit | Detail | — | Ouvre le panneau inline de détail client (lecture seule) ; 1 bouton par ligne de client |
| (bouton) Desactiver / Activer | submit | Desactiver / Activer | — | Toggle statut client Actif↔Inactif ; action directe sans confirmation ; message flash "Client désactivé" / "Client activé" |

---

## Section 3 — Panneau détail client (Modale inline)

Affiché après clic "Detail" — lecture seule, fermé par le bouton "Fermer".

| Champ | Type | Libellé | Valeur observée |
|---|---|---|---|
| Nom complet | lecture seule | Nom complet | Jean Dupont |
| Identifiant | lecture seule | Identifiant | user1 |
| Email | lecture seule | Email | jean.dupont@example.com |
| Téléphone | lecture seule | Telephone | +33601020304 |
| Rôle | lecture seule | Role | Client |
| Statut | lecture seule | Statut | Actif |
| Conseiller assigné | lecture seule | Conseiller assigne | Sophie Durand |
| Client depuis | lecture seule | Client depuis | 14/01/2024 (format dd/MM/yyyy) |

---

## Section 4 — Historique de tous les prêts

Tableau lecture seule. Colonnes : ID, Client, Type, Montant, Statut, Durée, Mensualité, Date début.

Aucun champ de saisie — consultation uniquement.

---

## Section 5 — Tous les utilisateurs du système

Tableau lecture seule. Colonnes : Nom, Login, Email, Rôle, Statut.

Aucun champ de saisie — consultation uniquement.

---

## Section 6 — Alertes Fraude

Tableau alimenté par `FraudAlertBean`. Colonnes : ID, Client, Règle déclenchée, Score risque, Statut, Description, Date, Actions.

| Champ | Type | Libellé | Contraintes |
|---|---|---|---|
| (texte) Traité par | lecture seule | Traité par | Colonne Actions — aucun bouton visible pour les alertes OPEN dans l'état crawlé |
| (bouton) Rafraîchir | submit | Rafraichir | Rechargement de la liste des alertes fraude |

**Règles observées :** NEW_BENEFICIARY, HIGH_FREQUENCY, VELOCITY_CHECK, FOREIGN_IBAN (scores : 30–65/100).

---

## Section 7 — Traitements nocturnes (EJB Timer @Schedule)

Tableau alimenté par `BatchAdminBean` / BATCH_JOB_LOGS. Colonnes : Job, Début, Fin, Durée (ms), Enregistrements, Statut, Détails.

| Champ | Type | Libellé | Contraintes |
|---|---|---|---|
| (bouton) Rafraîchir | submit | Rafraichir | Rechargement de la liste des traitements |

**État vide observé** : "Aucun traitement nocturne enregistré" (BATCH_JOB_LOGS vide en environnement local).

---

## Section 8 — Catalogue produits bancaires

Lien vers `/produits.xhtml` (FEAT-011). Tableaux lecture seule : Épargne, Crédit, Investissement.

Aucun champ de saisie — consultation uniquement.

---

## Activations dynamiques

- Le panneau **Detail client** (section 3) apparaît après clic sur le bouton "Detail" d'une ligne client — il disparaît après clic sur "Fermer".
- Le bouton **Desactiver** devient **Activer** (et vice versa) selon le statut courant du client (Actif/Inactif).

---

## Droits d'accès

| Rôle | Accès à admin.xhtml |
|---|---|
| ADMIN | Accès complet (toutes sections, tous boutons) |
| CONSEILLER | Redirection vers dashboard.xhtml |
| CLIENT | Redirection vers dashboard.xhtml |

---

## Champs techniques (hors périmètre métier)

- `javax.faces.ViewState` (hidden, 1 par formulaire JSF) : jeton anti-CSRF JSF
- `j_idt*` (hidden) : identificateurs de composants JSF générés automatiquement — pas de nom métier exposé
