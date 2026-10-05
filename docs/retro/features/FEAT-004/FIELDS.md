# FEAT-004 — Virements programmés : Champs

_Source : accessibility tree MCP Playwright (CLIENT-charge.snap, 2026-10-03)._
_Page_ : `/virement-programme.xhtml` — `RecurringTransferBean`

---

## Formulaire — Programmer un virement récurrent

| Champ | Type | Libellé UI | Requis | Valeur par défaut | Contraintes |
|---|---|---|---|---|---|
| `srcAcc` | combobox | Compte débiteur | Oui | `-- Choisir --` | Options alimentées dynamiquement par les comptes du client connecté |
| `montant` | textbox | Montant (EUR) | Oui | — | Validation serveur (montant numérique, positif) |
| `freq` | combobox | Fréquence | Oui | `Mensuel` | Options fixes : Hebdomadaire, Mensuel, Bimestriel, Trimestriel, Semestriel, Annuel |
| `targetNum` | textbox | IBAN / N compte bénéficiaire | Oui | — | — |
| `targetLabel` | textbox | Libellé bénéficiaire | Oui | — | — |
| `dateExec` | textbox | Date de première exécution | Oui | — | Format **`yyyy-MM-dd`** (ISO 8601) — incohérence : les autres pages de l'app affichent `dd/MM/yyyy` |
| `motif` | textbox | Motif (optionnel) | Non | — | Champ libre |

**Bouton de soumission :** « Créer le virement programmé » — soumission directe JSF, sans modale de confirmation.

---

## Tableau — Mes virements programmés

| Colonne | Type | Notes |
|---|---|---|
| Source | lecture seule | IBAN du compte débiteur (tronqué : ex. `FR76-0001-0001`) |
| Bénéficiaire | lecture seule | Affiché sur 3 lignes : **nom** (gras), IBAN, libellé |
| Montant | lecture seule | Format `EUR{montant}` (ex. `EUR35.00`) |
| Fréquence | lecture seule | Valeur de l'enum de fréquence |
| Prochain | lecture seule | Date de prochaine exécution — format `dd/MM/yyyy` |
| Nb exécutions | lecture seule | Nombre cumulé d'exécutions |
| État | lecture seule | `Actif` ou `Inactif` |
| Actions | boutons | Varie selon l'état : `[Suspendre] [Supprimer]` si Actif ; `[Réactiver] [Supprimer]` si Inactif — soumission directe sans confirmation |

---

## Messages observés

| Contexte | Message |
|---|---|
| Validation — compte source absent | `Veuillez selectionner un compte source` |
| Validation — format de date invalide | `Format de date invalide (attendu : yyyy-MM-dd)` |
| Création réussie | `Virement programme cree avec succes` |
| Liste vide | `Aucun virement programme configure` |

---

## Activations dynamiques

Aucune activation dynamique observée : tous les champs sont présents dès le chargement de la page.
Les boutons d'action dans le tableau varient selon l'état du virement (`Actif`/`Inactif`) — rendu serveur JSF, pas de JS côté client.

---

## Accessibilité multi-rôles

| Rôle | Comptes disponibles | Virements affichés |
|---|---|---|
| CLIENT (Jean Dupont) | FR76-0001-0001, FR76-0001-0002 | 3 virements (dont 1 Inactif) |
| ADMIN (Alice Martin) | aucun | Aucun virement programmé configuré |
| CONSEILLER (Sophie Durand) | à déterminer | non capturé (session identique à la structure CLIENT) |

Pas de restriction d'accès différenciée — tous les rôles voient la même interface, filtrée par les comptes et virements propres à leur utilisateur.
