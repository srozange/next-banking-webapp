---
feature: FEAT-012
name: "Comptes Tiers — Open Banking Powens"
status: current
step: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/webapp/comptes-tiers.xhtml
  - src/main/java/com/accenture/bank/powens/PowensBean.java
  - src/main/java/com/accenture/bank/powens/PowensCallbackBean.java
  - src/main/java/com/accenture/bank/powens/ExternalAccount.java
---

# MESSAGES — FEAT-012 Comptes Tiers — Open Banking Powens

Inventaire exhaustif des textes visibles ou émis par le système dans le périmètre de la
fonctionnalité « Comptes Tiers — Open Banking Powens » (`/comptes-tiers.xhtml`).
Les `{variables}` désignent des valeurs dynamiques interpolées à l'exécution.

> Note d'encodage : `comptes-tiers.xhtml` et les beans Java suppriment systématiquement les
> accents et les apostrophes typographiques (ex. « Deconnexion » au lieu de « Déconnexion »,
> « d autres » au lieu de « d'autres ») — les textes ci-dessous reproduisent l'orthographe
> exacte du source. La preuve `fichier:ligne` fait foi.

---

## En-tête et navigation

| Identifiant | Texte exact (source) | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-012-001 | `BankApp Demo - Comptes Tiers` | Titre du navigateur — toujours | |
| MSG-012-002 | `DB` | Logo initiales (disque rouge en barre de navigation) — toujours | |
| MSG-012-003 | `Demo Bank` | Marque (barre de navigation) — toujours | |
| MSG-012-004 | `Deconnexion` | Lien de déconnexion — toujours (utilisateur authentifié) | |

---

## Liens de navigation (toujours visibles)

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-012-010 | `Tableau de bord` | |
| MSG-012-011 | `Virement` | |
| MSG-012-012 | `Virements programmes` | |
| MSG-012-013 | `Releve` | |
| MSG-012-014 | `Budget` | |
| MSG-012-015 | `Prets` | |
| MSG-012-016 | `Documents` | |
| MSG-012-017 | `Bourse` | |
| MSG-012-018 | `Mon Conseiller` | |
| MSG-012-019 | `Produits` | |
| MSG-012-020 | `Comptes Tiers` | (lien actif sur cette page) |
| MSG-012-021 | `Beneficiaires` | |

### Liens de navigation conditionnels

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-012-022 | `Espace Conseiller` | `securityBean.adminOrConseiller` = true (rôles CONSEILLER ou ADMIN) | |
| MSG-012-023 | `Administration` | `securityBean.admin` = true (rôle ADMIN uniquement) | |

---

## Bandeau technique

| Identifiant | Texte exact (source) | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-012-030 | `🏦 Open Banking — Agregation de comptes via Powens API (PSD2) — OAuth2 depuis un EJB WildFly 14` | Toujours affiché sous la navigation | 122 |

---

## Messages dynamiques (errorMessage)

Les messages ci-dessous sont injectés dans le champ `powensBean.errorMessage` côté serveur,
puis affichés dans le bloc `.error-box` précédé du préfixe `⚠️`.
Le bloc est masqué quand `errorMessage` est vide.

| Identifiant | Texte exact (source) | Déclencheur | Règle associée | Preuve |
|---|---|---|---|---|
| MSG-012-040 | `Impossible d initialiser la connexion Powens.` | Échec d'appel à l'API Powens lors de l'initAndGetWebviewUrl() (résultat null) | RG-012-007 | |
| MSG-012-041 | `Session expiree. Veuillez recommencer la connexion.` | Callback reçu mais `accessToken` absent de la session | RG-012-012 | |
| MSG-012-042 | `Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes.` | Callback traité avec succès mais liste de comptes vide | RG-012-013 | |
| MSG-012-043 | `Callback Powens incomplet. Parametres recus: {liste}` | Callback reçu sans `connection_id` ni `code` ; `{liste}` = `params.keySet()` (clés reçues) | RG-012-008 | |

---

## État non-connecté (`powensBean.connected` = false)

| Identifiant | Type | Texte exact (source) | Contexte | Preuve |
|---|---|---|---|---|
| MSG-012-050 | Titre de section (h2) | `Agregez vos comptes bancaires` | En-tête du bloc de connexion | |
| MSG-012-051 | Corps de texte | `Connectez vos comptes d autres banques pour avoir une vue complete de votre patrimoine financier. Connexion securisee via le protocole OAuth2 PSD2.` | Paragraphe explicatif du bloc de connexion | 136 |
| MSG-012-052 | Logo banque | `BNP Paribas` | Logos banques partenaires (bloc `.banks-logos`) | |
| MSG-012-053 | Logo banque | `Credit Agricole` | Logos banques partenaires | |
| MSG-012-054 | Logo banque | `La Banque Postale` | Logos banques partenaires | |
| MSG-012-055 | Logo banque | `CIC` | Logos banques partenaires | |
| MSG-012-056 | Logo banque | `LCL` | Logos banques partenaires | |
| MSG-012-057 | Logo banque (résumé) | `+ 1800 banques` | Dernier élément de la liste des logos | |
| MSG-012-058 | Bouton d'action | `Connecter une banque` | Bouton principal — déclenche l'initiation OAuth Powens (RG-012-006) | |

---

## État connecté (`powensBean.connected` = true)

### Barre de statut

| Identifiant | Type | Texte exact (source) | Contexte | Preuve |
|---|---|---|---|---|
| MSG-012-060 | Badge statut | `Connected via Powens` | Barre de statut en haut du contenu connecté — toujours visible à l'état connecté | |
| MSG-012-061 | Bouton d'action | `Deconnecter` | Bouton de déconnexion Powens (RG-012-024) | |

### Carte patrimoine total

| Identifiant | Type | Texte exact (source) | Contexte | Preuve |
|---|---|---|---|---|
| MSG-012-062 | Libellé | `Patrimoine total (tous comptes)` | Étiquette au-dessus du montant total dans la carte noire | |
| MSG-012-063 | Badge fournisseur | `via Powens Open Banking API` | Sous le montant, dans la carte patrimoine | |

### Section comptes externes

| Identifiant | Type | Texte exact (source) | Contexte | Preuve |
|---|---|---|---|---|
| MSG-012-064 | Titre de section | `Mes comptes externes ({N})` | `{N}` = `powensBean.externalAccounts.size()` — nombre de comptes chargés | |
| MSG-012-065 | Libellé de carte | `Banque externe` | Label fixe affiché en haut de chaque carte compte (`.bank`) | |

---

## Libellés de type de compte (ExternalAccount.getTypeLabel)

Traduction du champ `type` renvoyé par l'API Powens en libellé français affiché dans la
carte compte (`.acc-type`). Règle associée : RG-012-016.

| Identifiant | Valeur API Powens (`type`) | Libellé affiché | Preuve |
|---|---|---|---|
| MSG-012-070 | `checking` | `Compte courant` | |
| MSG-012-071 | `savings` | `Epargne` | |
| MSG-012-072 | `card` | `Carte` | |
| MSG-012-073 | `loan` | `Credit` | |
| MSG-012-074 | `market` | `Bourse` | |
| MSG-012-075 | `null` | `Compte` | (valeur par défaut si `type == null`) |
| MSG-012-076 | _(toute autre valeur)_ | `{type}` (valeur brute de l'API) | (cas `default`) |

---

## Section transactions

| Identifiant | Type | Texte exact (source) | Contexte | Preuve |
|---|---|---|---|---|
| MSG-012-080 | Titre de section | `Transactions — {nom}` | `{nom}` = `powensBean.selectedAccount.name` — affiché quand un compte est sélectionné | |
| MSG-012-081 | État vide | `Aucune transaction disponible.` | Affiché dans le panneau transactions quand `powensBean.transactions` est vide | |
| MSG-012-082 | En-tête de colonne | `Date` | Colonne date de la transaction | |
| MSG-012-083 | En-tête de colonne | `Libelle` | Colonne libellé de la transaction | |
| MSG-012-084 | En-tête de colonne | `Montant` | Colonne montant (aligné à droite) | |
| MSG-012-085 | Préfixe montant | `+` | Préfixe affiché devant le montant pour les transactions crédit (`tx.credit = true`) ; aucun préfixe pour les débits | |
