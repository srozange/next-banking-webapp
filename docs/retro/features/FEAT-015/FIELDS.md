# FIELDS — FEAT-015 Espace Conseiller — Portefeuille clients

> Page en lecture seule (aucun champ de saisie). Tous les éléments sont des données affichées.
> Rôles autorisés : CONSEILLER, ADMIN. CLIENT redirigé vers dashboard.xhtml (silencieux).

---

## Section KPIs — Tableau de bord portefeuille

| Champ | Libellé affiché | Type | Format | Source (bean) |
|---|---|---|---|---|
| `nbClients` | Mes clients | Lecture seule | Entier | `getNbClients()` |
| `encodesTotal` | Encours total | Lecture seule | `EUR{N,NNN}` | `getEncoursTotal()` |
| `pendingLoansTotal` | Prets en attente | Lecture seule | Entier | `getPendingLoansTotal()` |

---

## Section Liste des clients (cartes)

| Champ | Libellé affiché | Type | Format / Valeurs | Source (bean) |
|---|---|---|---|---|
| `clientAvatar` | Avatar | Lecture seule | 1ère lettre du prénom | `customer.fullName.substring(0,1)` |
| `clientFullName` | Nom complet | Lecture seule | `Prénom Nom` | `customer.fullName` |
| `clientUsername` | Login | Lecture seule | `@{username}` | `customer.username` |
| `clientStatus` | Statut | Lecture seule | `Actif` / `Inactif` | `customer.active` |
| `clientTotalBalance` | Solde total | Lecture seule | `EUR{N,NNN}` | `ClientSummary.totalBalance` |
| `clientNbAccounts` | Comptes | Lecture seule | Entier | `accounts.size()` |
| `clientActiveLoans` | Prets actifs | Lecture seule | Entier | `activeLoansCount` |
| `clientPendingLoans` | Prets en attente | Lecture seule | Entier | `pendingLoansCount` |
| `pendingLoanAlert` | Alerte prêt(s) en attente | Lecture seule — conditionnel | `⚠ N pret(s) en attente` | Visible si `pendingLoansCount > 0` |

**Actions disponibles sur chaque carte** :

| Action | Libellé | Déclencheur JSF | Effet |
|---|---|---|---|
| Voir detail | `Voir detail` | `selectionnerClient(customerId)` | Ouvre le panneau de détail inline |

---

## Section Panneau de détail client (inline, non modal JS)

Visible uniquement après clic sur « Voir detail » (`selectedSummary` non null).

### Identité du client

| Champ | Libellé affiché | Type | Format | Source |
|---|---|---|---|---|
| `detailClientName` | Nom complet | Lecture seule | `Prénom Nom` | `selectedSummary.customer.fullName` |
| `detailClientLogin` | Login | Lecture seule | `@{username}` | `selectedSummary.customer.username` |
| `detailClientEmail` | Email | Lecture seule | adresse email | `selectedSummary.customer.email` |
| `detailClientPhone` | Téléphone | Lecture seule | `+33XXXXXXXXX` | `selectedSummary.customer.phone` |

**Actions** :

| Action | Libellé | Déclencheur JSF | Effet |
|---|---|---|---|
| Fermer | `Fermer` | `fermerDetail()` | Ferme le panneau, remet `selectedSummary` à null |

### Tableau Comptes bancaires

| Colonne | Libellé affiché | Type | Format / Valeurs | Source |
|---|---|---|---|---|
| `accountNumber` | Numero | Lecture seule | `FR76-XXXX-XXXX` | `BankAccount.accountNumber` |
| `accountType` | Type | Lecture seule | `COURANT`, `EPARGNE` | `BankAccount.type` |
| `accountBalance` | Solde | Lecture seule | `EUR{N,NNN.NN}` (gras) | `BankAccount.balance` |

Message si aucun compte : `(Aucun compte)` (bloc JSTL `c:when test="#{empty selectedSummary.accounts}"`)

### Tableau Prêts

| Colonne | Libellé affiché | Type | Format / Valeurs | Source |
|---|---|---|---|---|
| `loanType` | Type | Lecture seule | `CONSOMMATION`, `TRAVAUX`, `IMMOBILIER` | `Loan.type` |
| `loanAmount` | Montant | Lecture seule | `EUR{N,NNN}` | `Loan.amount` |
| `loanMonthly` | Mensualite | Lecture seule | `EUR{NNN.NN}` | `Loan.monthlyPayment` |
| `loanStatus` | Statut | Lecture seule | `EN_ATTENTE`, `EN_COURS`, `REMBOURSE` | `Loan.status` |

Message si aucun prêt : `(Aucun pret)` (bloc JSTL `c:when test="#{empty selectedSummary.loans}"`)

---

## État vide

Message affiché quand `portfolio` est vide (aucun client assigné au conseiller) :
> *Aucun client dans votre portefeuille. Contactez l administrateur pour l affectation des clients.*

Capture : `SCR-015-03_vide.png` (ADMIN Alice Martin, 0 clients)

---

## Activations dynamiques

| Élément | Condition d'affichage | Capture |
|---|---|---|
| Panneau de détail inline | `selectedSummary != null` (après `selectionnerClient()`) | `SCR-015-02_modale_detail_client.png` |
| Alerte `⚠ N pret(s) en attente` | `pendingLoansCount > 0` sur la carte | `SCR-015-01_charge.png` (Jean Dupont, 1 prêt EN_ATTENTE) |
| Classe CSS `selected` sur la carte | Client sélectionné dans `selectedSummary` | `SCR-015-02_modale_detail_client.png` |
| Classe CSS `inactive` sur la carte | `customer.active == false` | Non observable avec les données de test actuelles |
