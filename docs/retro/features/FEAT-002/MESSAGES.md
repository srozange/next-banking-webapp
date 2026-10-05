---
feature: FEAT-002
name: Tableau de bord
status: current
generated_by: retro-rules
date: 2026-10-03
---

# Messages et libellés — FEAT-002 Tableau de bord

Sources : `src/main/webapp/dashboard.xhtml` (libellés statiques),
`src/main/java/com/accenture/bank/mq/VirementProcessorMDB.java` (messages dynamiques).

> Note d'encodage : le fichier `dashboard.xhtml` ne contient pas de caractères accentués
> (ex. « publies » au lieu de « publiés ») — les textes ci-dessous reproduisent l'orthographe
> réelle du source. Les labels de navigation cités dans `FIELDS.md` (avec accents) reflètent
> le rendu navigateur (UTF-8) ou une correction postérieure au crawl.

---

## Libellés de l'interface (statiques)

### En-tête et navigation

| Identifiant | Texte exact (source) | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-002-001 | `BankApp Demo - Tableau de bord` | Titre du navigateur — toujours | |
| MSG-002-002 | `Demo Bank` | Marque — toujours | |
| MSG-002-003 | `Bonjour, {dashboardBean.currentCustomer.fullName}` | Toujours (utilisateur connecté) | |
| MSG-002-004 | `Deconnexion` | Toujours | |
| MSG-002-005 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 / ActiveMQ Artemis (MDB)` | Toujours (bandeau technique) | |

### Liens de navigation (toujours visibles)

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-002-010 | `Tableau de bord` | |
| MSG-002-011 | `Virement` | |
| MSG-002-012 | `Virements programmes` | |
| MSG-002-013 | `Releve` | |
| MSG-002-014 | `Budget` | |
| MSG-002-015 | `Prets` | |
| MSG-002-016 | `Documents` | |
| MSG-002-017 | `Bourse` | |
| MSG-002-018 | `Mon Conseiller` | |
| MSG-002-019 | `Produits` | |
| MSG-002-020 | `Comptes Tiers` | |
| MSG-002-021 | `Beneficiaires` | |

### Liens de navigation conditionnels

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-002-022 | `Espace Conseiller` | `securityBean.adminOrConseiller` = true (rôles CONSEILLER ou ADMIN) | |
| MSG-002-023 | `Administration` | `securityBean.admin` = true (rôle ADMIN uniquement) | |

---

## Centre de notifications

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-002-030 | `Notifications temps reel (Message Broker)` | Séparateur de section — toujours | |
| MSG-002-031 | `Centre de notifications` | En-tête de section — toujours | 161 |
| MSG-002-032 | `ActiveMQ Artemis - MDB JEE - queue/virements` | Étiquette technique — toujours | |
| MSG-002-033 | `{dashboardBean.unreadCount} nouvelle(s)` | Badge — si `unreadCount > 0` | |
| MSG-002-034 | `Tout marquer lu` | Bouton — si `unreadCount > 0` | 171 |
| MSG-002-035 | `Effacer tout` | Bouton — si `notifications` non vide | 175 |
| MSG-002-036 | `Aucune notification - effectuez un virement pour voir les messages publies via ActiveMQ Artemis en temps reel` | Message état vide — si `notifications` vide (référencé par RG-002-012) | 184 |
| MSG-002-037 | `Non lu` | Infobulle point violet — si `notif.read` = false | |
| MSG-002-038 | `- publie via ActiveMQ Artemis (MDB)` | Suffixe d'horodatage — chaque notification | |

---

## Messages de notification dynamiques (générés par VirementProcessorMDB)

Les messages ci-dessous sont construits en Java à l'exécution ; `{…}` désigne une valeur interpolée.

| Identifiant | Type | Gabarit du message | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-002-040 | `CONFIRMATION` (sans motif) | `Virement de {montant} EUR vers le compte {numCompte} confirme` | `montant < seuilAlerteEur` (défaut 5 000 EUR) ET `motif` vide | 79 |
| MSG-002-041 | `CONFIRMATION` (avec motif) | `Virement de {montant} EUR vers le compte {numCompte} confirme (motif : {motif})` | `montant < seuilAlerteEur` ET `motif` non vide | 80 |
| MSG-002-042 | `ALERTE` | `Virement de {montant} EUR en attente de validation - transaction importante, verification en cours.` | `montant >= seuilAlerteEur` (défaut 5 000 EUR) ET `montant < seuilFraudeEur` (défaut 10 000 EUR) | 70 |
| MSG-002-043 | `FRAUDE` | `Virement de {montant} EUR bloque pour verification (seuil anti-fraude : {seuilFraudeEur} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation.` | `montant >= seuilFraudeEur` (défaut 10 000 EUR) | 62 |

> Règles associées : RG-002-021 (CONFIRMATION), RG-002-022 (ALERTE), RG-002-023 (FRAUDE).

---

## Soldes et comptes Demo Bank

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-002-050 | `Mes comptes Demo Bank` | Séparateur de section — toujours | |
| MSG-002-051 | `Solde total Demo Bank` | En-tête de carte — toujours | |
| MSG-002-052 | `Tous comptes Demo Bank confondus` | Sous-titre de carte — toujours | |
| MSG-002-053 | `N {acc.accountNumber}` | Numéro de compte par carte — par compte | |

---

## Dernières opérations

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-002-060 | `Activite recente` | Séparateur de section — toujours | |
| MSG-002-061 | `Dernieres operations` | En-tête de section — toujours | |
| MSG-002-062 | `Date` | En-tête de colonne | |
| MSG-002-063 | `Description` | En-tête de colonne | |
| MSG-002-064 | `Type` | En-tête de colonne | |
| MSG-002-065 | `Montant` | En-tête de colonne | |
| MSG-002-066 | `Solde apres` | En-tête de colonne | |
| MSG-002-067 | `Voir le releve complet` | Bouton — toujours | |

---

## Open Banking (Powens)

### Si Powens connecté

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-002-070 | `Mes comptes externes (Open Banking)` | Séparateur de section — si `powensBean.connected` | |
| MSG-002-071 | `Patrimoine total` | En-tête de carte — si `powensBean.connected` | |
| MSG-002-072 | `Demo Bank + comptes externes` | Sous-titre de carte — si `powensBean.connected` | |
| MSG-002-073 | `Comptes externes via Powens Open Banking ({powensBean.externalAccounts.size()} comptes)` | En-tête de section — si `powensBean.connected` | 327 |
| MSG-002-074 | `Voir le detail et les transactions` | Bouton — si `powensBean.connected` | 349 |

### Si Powens non connecté

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-002-075 | `Open Banking` | Séparateur de section — si `not powensBean.connected` | |
| MSG-002-076 | `Agregez vos comptes d autres banques pour une vue complete de votre patrimoine` | Texte d'invitation — si `not powensBean.connected` | |
| MSG-002-077 | `Connecter mes autres banques via Powens` | Bouton — si `not powensBean.connected` | 365 |
