# FIELDS — FEAT-005 Relevé de compte

_Source : snap `CLIENT-charge.snap` + `releve.xhtml` (JSF EL — `extract-fields-from-dom.py` inapplicable, exit 1 sur XHTML)._
_Captures : `SCR-005-01_charge.png` (COURANT), `SCR-005-02_charge_epargne.png` (EPARGNE), `SCR-005-03_vide_conseiller.png`, `SCR-005-04_vide_admin.png`._

---

## Contrôles de saisie

| Champ | ID JSF | Type | Label | Obligatoire | Valeur par défaut | Notes |
|---|---|---|---|---|---|---|
| Sélecteur de compte | `releveForm:accountSelect` | `select` (combobox) | "Compte :" | Non | Premier compte du client (sélection auto à `@PostConstruct`) | Options chargées dynamiquement via `AccountService.getAccountsForCustomer`. AJAX `change` → `onAccountChange()` → rechargement partiel des panneaux `accountInfo` et `transactionPanel`. Non affiché si le client n'a aucun compte. |

**Activations dynamiques**
- Aucune (le sélecteur de compte est toujours visible dès le chargement, la liste peut être vide)

---

## Affichage lecture seule

| Panneau | Contenu | Condition d'affichage |
|---|---|---|
| `accountInfo` | `{accountType} — N° {accountNumber} — Solde actuel : {balance}` (devise EUR, 2 décimales) | `selectedAccount` non null |
| `transactionPanel` | Compteur + tableau des transactions | Toujours présent ; message "Aucune transaction pour ce compte." si `transactions` vide |

### Colonnes du tableau des transactions

| Colonne | Champ entité | Format | Notes |
|---|---|---|---|
| Date | `transaction.transactionDate` | `dd/MM/yyyy HH:mm` | `f:convertDateTime pattern="dd/MM/yyyy HH:mm"` |
| Description | `transaction.description` | Texte | Si description commence par `[EN ATTENTE]` : badge "En attente" affiché, préfixe retiré. Ligne fond jaune (`pending-row`). |
| Type | `transaction.type` | Badge `CREDIT` (vert) / `DEBIT` (rouge) | Valeurs énumérées |
| Montant | `transaction.amount` | `+ €X,XX` (CREDIT) / `- €X,XX` (DEBIT) | `f:convertNumber type=currency currencySymbol=€ minFractionDigits=2` |
| Solde après | `transaction.balanceAfter` | `€X,XX` | Même format devise |

---

## États observés par rôle

| Rôle | État | Résumé |
|---|---|---|
| CLIENT | Chargé | 2 comptes disponibles (COURANT 27 tx, EPARGNE 6 tx). Sélecteur fonctionnel via AJAX. |
| CONSEILLER | Vide | Aucun compte bancaire → combobox vide → "Aucune transaction pour ce compte." Nav: "Espace Conseiller" visible. |
| ADMIN | DroitsRéduits | Idem CONSEILLER. Nav: "Espace Conseiller" + "Administration" visibles. |

---

## Résumé

- **Champs de saisie : 1** (sélecteur de compte)
- **Obligatoires : 0** | **Conditionnels : 0** | **Lecture seule : 5 colonnes tableau**
- **Message état vide** : "Aucune transaction pour ce compte."
- **Transactions "en attente"** : badge visuel + fond jaune si description débute par `[EN ATTENTE]`
