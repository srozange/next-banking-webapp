# FIELDS — FEAT-002 Tableau de bord

_Source : snapshots MCP Playwright (CLIENT-charge.snap, ADMIN-charge.snap, CONSEILLER-charge.snap) — 2026-10-03_

## Résumé

Page purement affichage (lecture seule). Aucun champ de saisie. Aucun formulaire. Tous les éléments interactifs sont des liens de navigation.

**Champs obligatoires : 0 | Conditionnels : 0 | Lecture seule : 5 (éléments affichage)**

---

## Éléments d'affichage

| Identifiant | Type | Libellé | Visibilité par rôle | Format | Notes |
|---|---|---|---|---|---|
| `solde_total` | Affichage | Solde total Demo Bank | CLIENT (données réelles) / ADMIN et CONSEILLER (EUR0.00) | EURx,xxx.xx | Somme de tous les comptes Demo Bank de l'utilisateur connecté |
| `compte_courant_solde` | Affichage | COURANT | CLIENT uniquement (si comptes existants) | EURx,xxx.xx + N IBAN partiel | Absent pour ADMIN et CONSEILLER (aucun compte personnel) |
| `compte_epargne_solde` | Affichage | EPARGNE | CLIENT uniquement (si comptes existants) | EURx,xxx.xx + N IBAN partiel | Absent pour ADMIN et CONSEILLER (aucun compte personnel) |
| `dernieres_operations` | Tableau | Dernières opérations | Tous rôles (vide pour ADMIN/CONSEILLER) | Colonnes : Date (dd/MM/yyyy HH:mm), Description, Type (DEBIT\|CREDIT), Montant (EURx.xx), Solde après (EURx,xxx.xx) | 5 dernières opérations du compte courant ; tableau vide si aucun compte |
| `notifications_jms` | Affichage | Centre de notifications | Tous rôles | Texte libre | Temps réel via ActiveMQ Artemis / VirementProcessorMDB. Affiche "Aucune notification - effectuez un virement pour voir les messages publiés via ActiveMQ Artemis en temps réel" si aucun virement récent |

---

## Navigation contextuelle (liens affichés selon le rôle)

| Lien | CLIENT | CONSEILLER | ADMIN |
|---|---|---|---|
| Tableau de bord | ✓ | ✓ | ✓ |
| Virement | ✓ | ✓ | ✓ |
| Virements programmés | ✓ | ✓ | ✓ |
| Relevé | ✓ | ✓ | ✓ |
| Budget | ✓ | ✓ | ✓ |
| Prêts | ✓ | ✓ | ✓ |
| Documents | ✓ | ✓ | ✓ |
| Bourse | ✓ | ✓ | ✓ |
| Mon Conseiller | ✓ | ✓ | ✓ |
| Produits | ✓ | ✓ | ✓ |
| Comptes Tiers | ✓ | ✓ | ✓ |
| Bénéficiaires | ✓ | ✓ | ✓ |
| Espace Conseiller | — | ✓ | ✓ |
| Administration | — | — | ✓ |

---

## États non observables depuis ce seul écran

| État | Raison | Comment le déclencher |
|---|---|---|
| `ChargéAvecNotification` | Pas de formulaire sur dashboard.xhtml — notifications déclenchées par un virement | Soumettre un virement depuis virement.xhtml, puis revenir sur dashboard.xhtml |
| `Erreur` | Page lecture seule — aucun appel côté serveur initié par l'utilisateur | Non applicable (sauf panne serveur non observable via crawl) |
| `Modale` | Aucun élément déclencheur de modale sur cette page | Non applicable |
| `Confirmation` | Aucune action modifiant les données sur cette page | Non applicable |
| `Chargement` | Chargement quasi-instantané — pas d'état intermédiaire visible | Non observable (JSF ViewScoped + init server-side) |
