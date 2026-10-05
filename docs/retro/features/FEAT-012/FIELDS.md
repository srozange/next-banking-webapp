# FIELDS — FEAT-012 Comptes Tiers — Open Banking Powens

_Source : comptes-tiers.xhtml · PowensBean.java · PowensService.java_
_Crawl : 2026-10-03 · Rôles : CLIENT, ADMIN, CONSEILLER_

---

## Résumé

Cette feature ne contient **aucun champ de saisie utilisateur**. Toutes les interactions se font via des boutons d'action et des liens de sélection. La saisie de données bancaires est déléguée à la **Webview Powens externe** (OAuth2 PSD2).

---

## Actions (boutons)

| Libellé | Type JSF | État visible | Rôle | Effet |
|---|---|---|---|---|
| Connecter une banque | `h:commandButton` → `input[type=submit]` | Non-connecté | tous | Appelle `powensBean.connectBank()` : crée user Powens anonyme, stocke `accessToken` en session, redirige vers Webview Powens externe |
| Deconnecter | `h:commandButton` → `input[type=submit]` + `f:ajax` | Connecté | tous | Appelle `powensBean.disconnect()` via AJAX : efface `accessToken`, `externalAccounts`, `selectedAccount`, `transactions`, remet `connected=false` |

---

## Données affichées (lecture seule)

| Donnée | Source | État visible | Notes |
|---|---|---|---|
| Logos banques partenaires | Statique xhtml | Non-connecté | BNP Paribas, Crédit Agricole, La Banque Postale, CIC, LCL, + 1800 banques |
| Badge « Connected via Powens » | `powensBean.connected` | Connecté | CSS classe `connected-badge` |
| Patrimoine total | `powensBean.totalBalance` | Connecté | `f:convertNumber type="currency" currencySymbol="EUR" groupingUsed="true" minFractionDigits="2"` — somme des soldes de tous les comptes externes |
| Comptes externes (grille) | `powensBean.externalAccounts` | Connecté | Par compte : banque (fixe "Banque externe"), nom, typeLabel, solde (minFractionDigits=2, maxFractionDigits=2, groupingUsed=true), IBAN (optionnel si `acc.iban` non vide) |
| Transactions du compte sélectionné | `powensBean.transactions` | Connecté, compte sélectionné | Colonnes : Date, Libellé, Montant (crédit vert / débit rouge) avec signe `+` pour crédit ; formaté minFractionDigits=2, maxFractionDigits=2 |
| Message erreur | `powensBean.errorMessage` | Erreur | CSS classe `error-box` (fond orange). Apparaît au-dessus de l'état courant (connecté ou non-connecté) |

---

## Messages d'erreur observés

| Message | Déclencheur | Bean |
|---|---|---|
| `Impossible d initialiser la connexion Powens.` | `PowensService.initAndGetWebviewUrl()` retourne `null` | `PowensBean.connectBank()` |
| `Session expiree. Veuillez recommencer la connexion.` | Callback reçu mais `powensBean.accessToken == null` | `PowensBean.handleCallback()` |
| `Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes.` | Callback OK mais `getAccounts()` retourne liste vide | `PowensBean.handleCallback()` |
| `Callback Powens incomplet. Parametres recus: []` | Callback reçu sans `connection_id` ni `code` | `PowensCallbackBean.processCallback()` |

---

## Interactions dynamiques

| Interaction | Déclencheur | Effet |
|---|---|---|
| Sélection de compte | Clic sur une account-card (`h:commandLink` + `f:ajax render=":transactionsPanel"`) | Appelle `powensBean.selectAccount(acc)` → `loadTransactions()` ; met à jour la section transactions par AJAX sans rechargement de page |
| Rafraîchissement comptes | `powensBean.refreshAccounts()` (méthode présente dans le bean, non exposée dans l'UI actuelle) | Recharge `externalAccounts` et sélectionne le premier compte si aucun n'est sélectionné |

---

## Champs JSF internes (non-métier)

| Nom | Type | Description |
|---|---|---|
| `javax.faces.ViewState` | hidden | Token anti-CSRF JSF — présent dans chaque formulaire |
| `j_idt6`, `j_idt20`, `accountsForm` | hidden | Identifiants de formulaires JSF générés automatiquement |

---

## Droits par rôle

| Rôle | Accès | Différences de contenu |
|---|---|---|
| CLIENT | Plein accès | Navigation standard |
| CONSEILLER | Plein accès | Navigation enrichie : lien « Espace Conseiller » |
| ADMIN | Plein accès | Navigation enrichie : liens « Espace Conseiller » + « Administration » |

Aucune restriction de rôle sur le contenu de la page (AppFilter : seules `admin.xhtml` et `conseiller-workspace.xhtml` sont protégées). Pas d'état « DroitsRéduits » pour cette feature.

---

## Paramètres d'intégration Powens (observés)

| Paramètre | Valeur observée | Source |
|---|---|---|
| `domain` | `demi-sandbox.biapi.pro` | URL Webview capturée (SCR-012-02) |
| `client_id` | `99961298` | URL Webview capturée |
| `redirect_uri` | `http://localhost:8080/banking-legacy/powens-callback.xhtml` | URL Webview capturée |
| `connector_ids` | `59` | URL Webview capturée |
| `code` | jeton Bearer JWT (opaque) | Transmis par Powens dans l'URL Webview initiale |
