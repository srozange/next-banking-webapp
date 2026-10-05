---
step: retro-ui / étape 4 consolidation globale
generated_by: retro-ui-agent
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources: retro/features/FEAT-001..FEAT-030/MESSAGES.md (tous produits par retro-rules)
---

# MESSAGES — Dictionnaire global i18n

Inventaire exhaustif de tous les textes visibles ou émis par le système, agrégé depuis les 30
fichiers `MESSAGES.md` par feature. Organisé en quatre parties :

1. **Textes transversaux** — repris à l'identique sur toutes les pages authentifiées (navigation, en-tête)
2. **Messages par feature** — textes propres à chaque fonctionnalité
3. **Textes persistés hors UI** — descriptions de transactions, champs BatchJobLog, logs applicatifs
4. **Formats i18n observés**

> Note d'encodage globale : les templates JSF omettent systématiquement les accents dans plusieurs
> libellés statiques (ex. « Deconnexion », « Prets », « Virements programmes », « Releve »,
> « Beneficiaires »). Les textes ci-dessous reproduisent l'orthographe exacte du source.
> Les messages produits en Java (`FacesMessage`, constructeurs de chaînes) conservent en général
> leurs accents. La preuve `fichier:ligne` de chaque MESSAGES.md par feature fait foi.

---

## Partie 1 — Textes transversaux (navigation et en-tête communs)

Ces textes apparaissent à l'identique sur la quasi-totalité des pages authentifiées.
Ils sont documentés ici une seule fois ; les MESSAGES.md par feature les citent avec leur preuve
locale. Toute divergence notable est signalée dans la section feature concernée.

### 1.1 Identité de l'application

| Texte exact | Contexte | Preuve type |
|---|---|---|
| `BankApp Demo - {titre de page}` | Titre de l'onglet navigateur — pattern commun à toutes les pages | etc. |
| `DB` | Initiales dans le disque rouge (logo) en barre de navigation | etc. |
| `Demo Bank` | Nom de l'application — toujours visible dans la barre de navigation | etc. |

### 1.2 En-tête barre de navigation

| Texte exact | Contexte | Notes |
|---|---|---|
| `Bonjour, {fullName}` | Salutation personnalisée — toujours affiché sauf `conseiller.xhtml` (qui n'affiche pas cet élément) | `{fullName}` = attribut `fullName` du bean client connecté |
| `Deconnexion` | Lien / bouton de déconnexion — toujours visible | Orthographe sans accent dans le source (JSF statique) |

### 1.3 Menu de navigation principal (liens toujours visibles — tous rôles)

| Texte exact | Destination |
|---|---|
| `Tableau de bord` | `/dashboard.xhtml` |
| `Virement` | `/virement.xhtml` |
| `Virements programmes` | `/virement-programme.xhtml` |
| `Releve` | `/releve.xhtml` |
| `Budget` | `/budget.xhtml` |
| `Prets` | `/pret.xhtml` |
| `Documents` | `/documents.xhtml` |
| `Bourse` | `/bourse.xhtml` |
| `Mon Conseiller` | `/conseiller.xhtml` |
| `Produits` | `/produits.xhtml` |
| `Comptes Tiers` | `/comptes-tiers.xhtml` |
| `Beneficiaires` | `/beneficiaires.xhtml` |

### 1.4 Liens de navigation conditionnels (rôle)

| Texte exact | Condition d'affichage | Règle |
|---|---|---|
| `Espace Conseiller` | `securityBean.adminOrConseiller` = true (rôles CONSEILLER ou ADMIN) | RG-030-008 |
| `Administration` | `securityBean.admin` = true (rôle ADMIN uniquement) | RG-030-009 |

### 1.5 Bandeau technique legacy

Bandeau informatif non fonctionnel, affiché sur la plupart des pages authentifiées.
Texte variable selon la feature ; les variantes sont listées dans chaque section feature.

---

## Partie 2 — Messages par feature

---

### FEAT-001 « Connexion »
_Source : login.xhtml, LoginBean.java_

#### Écran SCR-001-01 — Formulaire (état vide / chargé)

| Type | Texte exact | Preuve |
|---|---|---|
| Titre (onglet) | `BankApp Demo - Connexion` | |
| Logo | `DB` | |
| Titre principal | `Demo Bank` | |
| Sous-titre | `Espace client — Connexion sécurisée` | |
| Libellé | `Identifiant` | |
| Libellé | `Mot de passe` | |
| Action | `Me connecter` | |
| Badge technique | `⚠️ Système Legacy — JEE 8 / WildFly 14` | |

#### Erreurs de validation JSF (champs vides — invisibles en production)

| Texte exact | Condition | Preuve |
|---|---|---|
| `Identifiant requis` | Champ username vide — JSF par-champ, masqué par `globalOnly="true"` en prod | |
| `Mot de passe requis` | Champ password vide — même contrainte | |

#### Erreur d'authentification (identifiants incorrects)

| Texte exact | Rôle | Preuve |
|---|---|---|
| `Identifiants incorrects` | Résumé (`SEVERITY_ERROR`, `clientId=null`) | |
| `Vérifiez votre login et mot de passe` | Détail du message | |

---

### FEAT-002 « Tableau de bord »
_Source : dashboard.xhtml, VirementProcessorMDB.java_

#### En-tête

| Identifiant | Texte exact | Preuve |
|---|---|---|
| MSG-002-001 | `BankApp Demo - Tableau de bord` | |
| MSG-002-002 | `Demo Bank` | |
| MSG-002-003 | `Bonjour, {dashboardBean.currentCustomer.fullName}` | |
| MSG-002-004 | `Deconnexion` | |
| MSG-002-005 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 / ActiveMQ Artemis (MDB)` | |

#### Navigation conditionnelle

| Identifiant | Texte exact | Condition |
|---|---|---|
| MSG-002-022 | `Espace Conseiller` | `securityBean.adminOrConseiller` = true |
| MSG-002-023 | `Administration` | `securityBean.admin` = true |

#### Centre de notifications

| Identifiant | Texte exact | Condition |
|---|---|---|
| MSG-002-030 | `Notifications temps reel (Message Broker)` | Toujours |
| MSG-002-031 | `Centre de notifications` | Toujours |
| MSG-002-032 | `ActiveMQ Artemis - MDB JEE - queue/virements` | Toujours |
| MSG-002-033 | `{dashboardBean.unreadCount} nouvelle(s)` | Si `unreadCount > 0` |
| MSG-002-034 | `Tout marquer lu` | Si `unreadCount > 0` |
| MSG-002-035 | `Effacer tout` | Si liste non vide |
| MSG-002-036 | `Aucune notification - effectuez un virement pour voir les messages publies via ActiveMQ Artemis en temps reel` | État vide |
| MSG-002-037 | `Non lu` | Infobulle point violet |
| MSG-002-038 | `- publie via ActiveMQ Artemis (MDB)` | Suffixe horodatage |

#### Notifications dynamiques (VirementProcessorMDB — affichées dans le centre)

| Identifiant | Type | Gabarit | Condition |
|---|---|---|---|
| MSG-002-040 | `CONFIRMATION` | `Virement de {montant} EUR vers le compte {numCompte} confirme` | `montant < 5 000 EUR`, motif vide |
| MSG-002-041 | `CONFIRMATION` | `Virement de {montant} EUR vers le compte {numCompte} confirme (motif : {motif})` | `montant < 5 000 EUR`, motif non vide |
| MSG-002-042 | `ALERTE` | `Virement de {montant} EUR en attente de validation - transaction importante, verification en cours.` | `5 000 EUR ≤ montant < 10 000 EUR` |
| MSG-002-043 | `FRAUDE` | `Virement de {montant} EUR bloque pour verification (seuil anti-fraude : {seuilFraudeEur} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation.` | `montant ≥ 10 000 EUR` |

#### Comptes Demo Bank

| Identifiant | Texte exact | Condition |
|---|---|---|
| MSG-002-050 | `Mes comptes Demo Bank` | Toujours |
| MSG-002-051 | `Solde total Demo Bank` | Toujours |
| MSG-002-052 | `Tous comptes Demo Bank confondus` | Toujours |
| MSG-002-053 | `N {acc.accountNumber}` | Par compte |

#### Dernières opérations

| Identifiant | Texte exact |
|---|---|
| MSG-002-060 | `Activite recente` |
| MSG-002-061 | `Dernieres operations` |
| MSG-002-062 | `Date` |
| MSG-002-063 | `Description` |
| MSG-002-064 | `Type` |
| MSG-002-065 | `Montant` |
| MSG-002-066 | `Solde apres` |
| MSG-002-067 | `Voir le releve complet` |

#### Section Open Banking (Powens connecté)

| Identifiant | Texte exact | Condition |
|---|---|---|
| MSG-002-070 | `Mes comptes externes (Open Banking)` | `powensBean.connected` = true |
| MSG-002-071 | `Patrimoine total` | idem |
| MSG-002-072 | `Demo Bank + comptes externes` | idem |
| MSG-002-073 | `Comptes externes via Powens Open Banking ({powensBean.externalAccounts.size()} comptes)` | idem |
| MSG-002-074 | `Voir le detail et les transactions` | idem |
| MSG-002-075 | `Open Banking` | `powensBean.connected` = false |
| MSG-002-076 | `Agregez vos comptes d autres banques pour une vue complete de votre patrimoine` | idem |
| MSG-002-077 | `Connecter mes autres banques via Powens` | idem |

---

### FEAT-003 « Effectuer un virement »
_Source : virement.xhtml, VirementBean.java, TransferService.java, VirementProcessorMDB.java_

#### Bandeaux

| Texte exact | Preuve |
|---|---|
| `Système Legacy — Virement géré par EJB CMT (Container Managed Transaction)` | |
| `Après validation, une notification sera publiée sur queue/virements (ActiveMQ Artemis) et visible dans le tableau de bord.` | |
| `Virement SEPA externe — Gérer mes bénéficiaires` | |

#### Formulaire

| Type | Texte exact | Preuve |
|---|---|---|
| Titre de section | `Effectuer un virement` | |
| Libellé | `Compte source` | |
| Option vide | `-- Choisir un compte --` | |
| Libellé | `Compte destinataire` | |
| Option vide | `-- Choisir un compte --` | |
| Libellé | `Montant (€)` | |
| Indice | `Ex : 100,00 — Saisir plus de 5 000 € pour tester l'alerte fraude` | |
| Libellé | `Motif` | |
| Action | `Exécuter le virement` | |

#### Erreurs de validation JSF

| Texte exact | Condition |
|---|---|
| `virementForm:source: Validation Error: Value is required.` | Compte source vide |
| `virementForm:target: Validation Error: Value is required.` | Compte destinataire vide |
| `virementForm:amount: Validation Error: Value is required.` | Montant vide |
| `virementForm:motif: Validation Error: Value is required.` | Motif vide (dette-legacy : le serveur accepte null) |

#### Erreurs serveur

| Texte exact | Condition | Preuve |
|---|---|---|
| `Veuillez sélectionner les deux comptes` | Aucun compte sélectionné | |
| `Le compte source et destination doivent être différents` | Même compte | |
| `Erreur lors du virement : java.lang.IllegalStateException: Solde insuffisant` | Solde insuffisant — dette-legacy (trace Java exposée) | |
| `Erreur lors du virement : java.lang.IllegalArgumentException: Compte introuvable` | Compte absent | |
| `Erreur lors du virement : java.lang.IllegalArgumentException: Le montant doit etre positif` | Montant ≤ 0 | |

> Message mort (jamais affiché) : `Solde insuffisant sur le compte source` (catch `IllegalStateException` inatteignable, l'EJB CMT enveloppe dans `EJBException`).

#### Avertissement fraude

| Texte exact | Condition | Preuve |
|---|---|---|
| `Attention : {N} alerte(s) fraude détectée(s) sur ce virement. Votre conseiller a été notifié.` | Si `alerts.size() > 0` | |

#### Notifications JMS asynchrones (VirementProcessorMDB — visibles dans le tableau de bord)

| Type | Gabarit | Condition | Preuve |
|---|---|---|---|
| `CONFIRMATION` | `Virement de {montant} EUR vers le compte {n°_compte_destinataire} confirme` | `montant < 5 000 EUR`, motif absent | |
| `CONFIRMATION` | `Virement de {montant} EUR vers le compte {n°_compte_destinataire} confirme (motif : {motif})` | `montant < 5 000 EUR`, motif non vide | |
| `ALERTE` | `Virement de {montant} EUR en attente de validation - transaction importante, verification en cours.` | `5 000 EUR ≤ montant < 10 000 EUR` | |
| `FRAUDE` | `Virement de {montant} EUR bloque pour verification (seuil anti-fraude : {seuil} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation.` | `montant ≥ 10 000 EUR` | |

---

### FEAT-004 « Virements programmés »
_Source : virement-programme.xhtml, RecurringTransferBean.java, RecurringTransferService.java_

#### Formulaire de création

| Texte exact | Contexte | Preuve |
|---|---|---|
| `BankApp Demo - Virements programmes` | Titre onglet | |
| `Nouveau virement programme` | Titre section | |
| `Compte debiteur` | Libellé `srcAcc` | |
| `-- Choisir --` | Option vide `srcAcc` | |
| `Montant (EUR)` | Libellé `montant` | |
| `ex: 200.00` | Placeholder `montant` | |
| `Frequence` | Libellé `freq` | |
| `IBAN / N compte beneficiaire` | Libellé `targetNum` | |
| `ex: FR76-0001-0002` | Placeholder `targetNum` | |
| `Libelle beneficiaire` | Libellé `targetLabel` | |
| `ex: Compte epargne` | Placeholder `targetLabel` | |
| `Date de premiere execution` | Libellé `dateExec` | |
| `yyyy-MM-dd` | Placeholder `dateExec` | |
| `Motif (optionnel)` | Libellé `motif` | |
| `ex: Epargne mensuelle` | Placeholder `motif` | |
| `Creer le virement programme` | Bouton soumission | |

#### Options de fréquence (RecurringFrequency)

| Texte exact | Valeur enum |
|---|---|
| `Hebdomadaire` | `HEBDOMADAIRE` |
| `Mensuel` | `MENSUEL` (valeur par défaut) |
| `Bimestriel` | `BIMESTRIEL` |
| `Trimestriel` | `TRIMESTRIEL` |
| `Semestriel` | `SEMESTRIEL` |
| `Annuel` | `ANNUEL` |

#### Tableau des virements existants

| Texte exact | Contexte |
|---|---|
| `Mes virements programmes` | Titre section |
| `Source` | En-tête colonne |
| `Beneficiaire` | En-tête colonne |
| `Montant` | En-tête colonne |
| `Frequence` | En-tête colonne |
| `Prochain` | En-tête colonne |
| `Nb executions` | En-tête colonne |
| `Etat` | En-tête colonne |
| `Actions` | En-tête colonne |
| `Actif` | Badge état actif |
| `Inactif` | Badge état suspendu |
| `Suspendre` | Bouton — virement actif |
| `Reactiver` | Bouton — virement inactif |
| `Supprimer` | Bouton — tout virement |
| `Aucun virement programme configure` | État vide |

#### Messages de retour utilisateur

| Texte exact | Condition | Preuve |
|---|---|---|
| `Veuillez selectionner un compte source` | `srcAcc` null | |
| `Veuillez saisir le numero de compte destinataire` | `targetNum` vide | |
| `Veuillez saisir le libelle du beneficiaire` | `targetLabel` vide | |
| `Montant invalide` | Montant nul ou négatif | |
| `Veuillez saisir la date de premiere execution` | `dateExec` vide | |
| `Format de date invalide (attendu : yyyy-MM-dd)` | Format non conforme | |
| `Virement programme cree avec succes` | Succès création | |
| `Erreur : {message}` | Exception création ou toggle | |
| `Erreur suppression : {message}` | Exception suppression | |

#### Exceptions service propagées (encadrées par `Erreur : `)

| Texte exact | Condition | Preuve |
|---|---|---|
| `Client ou compte introuvable` | Client / compte absent | |
| `Compte non autorise` | Compte non appartenant au client | |
| `Montant invalide` | Double vérification couche service | |

---

### FEAT-005 « Relevé de compte »
_Source : releve.xhtml, ReleveBean.java_

| Texte exact | Contexte | Preuve |
|---|---|---|
| `BankApp Demo - Relevé de compte` | Titre onglet | |
| `Système Legacy — Requête via @NamedQuery JPA` | Bandeau technique | |
| `Relevé de compte` | Titre section | |
| `Compte :` | Label sélecteur | |
| `{accountType} — N° {accountNumber} — Solde actuel : {balance}` | Panneau info compte | |
| `Aucune transaction pour ce compte.` | État vide | |
| `{N} opération(s)` | Compteur | |
| `Date` | En-tête colonne | |
| `Description` | En-tête colonne | |
| `Type` | En-tête colonne | |
| `Montant` | En-tête colonne | |
| `Solde après` | En-tête colonne | |
| `CREDIT` | Badge vert transaction | |
| `DEBIT` | Badge rouge transaction | |
| `En attente` | Badge orange — description débute par `[EN ATTENTE] ` | |
| `+` | Préfixe montant CREDIT | |
| `-` | Préfixe montant DEBIT | |

---

### FEAT-006 « Budget et catégories de dépenses »
_Source : budget.xhtml, BudgetBean.java_

#### En-tête et navigation

| Identifiant | Texte exact | Preuve |
|---|---|---|
| MSG-006-001 | `BankApp Demo - Budget` | |
| MSG-006-005 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 — Categorisation automatique par mots-cles` | |

#### Sélecteur de période

| Texte exact | Preuve |
|---|---|
| `Periode :` | |
| `Afficher` | |

#### Cartes de résumé

| Texte exact |
|---|
| `Total depenses` |
| `Total revenus` |
| `Plafonds definis` |

#### Répartition par catégorie

| Identifiant | Texte exact | Condition |
|---|---|---|
| MSG-006-040 | `Repartition par categorie` | Toujours |
| MSG-006-041 | `Depenses du mois {budgetBean.selectedMonth}` | Toujours |
| MSG-006-042 | `Aucune depense enregistree sur cette periode` | Liste vide |
| MSG-006-043 | `{pourcentage}% du plafond` | Si plafond défini |
| MSG-006-044 | `Aucun plafond defini` | Si pas de plafond |
| MSG-006-045 | `Depassement` | Badge rouge si dépassement |

#### Gestion des plafonds

| Texte exact | Contexte |
|---|---|
| `Plafonds budgetaires` | Séparateur section |
| `Definir un plafond` | Titre formulaire |
| `Categorie` | Label sélecteur |
| `-- Choisir --` | Option vide |
| `Plafond mensuel (EUR)` | Label champ plafond |
| `ex: 500` | Placeholder |
| `Enregistrer le plafond` | Bouton |
| `Plafonds du mois {budgetBean.selectedMonth}` | Titre tableau |
| `Categorie` / `Plafond` / `Depense` / `Ecart` / `Action` | En-têtes colonnes |
| `Supprimer` | Bouton par ligne |

#### Messages dynamiques

| Identifiant | Type | Gabarit | Condition | Preuve |
|---|---|---|---|---|
| MSG-006-070 | Erreur | `Veuillez selectionner une categorie` | Catégorie non sélectionnée | |
| MSG-006-071 | Erreur | `Veuillez saisir un plafond valide` | Plafond ≤ 0 | |
| MSG-006-072 | Info | `Plafond enregistre pour {cat.libelle}` | Succès | |
| MSG-006-073 | Erreur | `Erreur : {e.getMessage()}` | Exception | |
| MSG-006-074 | Erreur | `Erreur suppression : {e.getMessage()}` | Exception | |

---

### FEAT-007 « Demande et suivi de prêts »
_Source : pret.xhtml, LoanBean.java, LoanApprovalMDB.java, LoanStatus.java, LoanType.java_

#### En-tête

| Texte exact | Preuve |
|---|---|
| `BankApp Demo - Prets` | |
| `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 — Scoring credit via ActiveMQ Artemis (MDB) — simule le traitement mainframe` | |

#### Simulateur de prêt

| Texte exact | Contexte |
|---|---|
| `Simulateur de pret` | Séparateur section |
| `Simuler un emprunt` | Titre |
| `Montant (EUR)` | Label `simMontant` |
| `Taux annuel (%)` | Label `simTaux` |
| `Duree (mois)` | Label `simDuree` |
| `Calculer` | Bouton |
| `Mensualite` / `Cout total` / `Total interets` | Libellés cartes résultat (si `simulationResult != null`) |
| `Voir l echeancier complet ({loanBean.simDuree} echeances)` | Lien collapsible |
| `#` / `Capital` / `Interets` / `Total` / `Capital restant` | En-têtes tableau échéancier simulateur |

#### Formulaire de demande

| Texte exact | Contexte |
|---|---|
| `Demande de pret` | Séparateur section |
| `Nouveau pret` | Titre |
| `Type de pret` | Label `typePret` |
| `Montant (EUR)` | Label `montant` |
| `Taux annuel (%)` | Label `taux` |
| `Duree (mois)` | Label `duree` |
| `Objet du pret` | Label `objetPret` |
| `Soumettre la demande` | Bouton |
| `La demande sera envoyee sur queue/pretDemandesQueue et traitee de facon asynchrone par le service de scoring credit (MDB WildFly).` | Texte informatif |

#### Tableau emprunts en cours

| Texte exact | Contexte |
|---|---|
| `Mes emprunts` | Séparateur section |
| `Emprunts en cours` | Titre |
| `Aucun emprunt enregistre` | État vide |
| `Type` / `Montant` / `Taux` / `Duree` / `Mensualite` / `Total interets` / `Statut` / `Echeancier` / `Action` | En-têtes colonnes |
| `Voir` | Bouton — si échéancier dispo |
| `En cours de traitement...` | Inline — si `loan.status == EN_ATTENTE` |
| `Supprimer` | Bouton par ligne |

#### Échéancier du prêt sélectionné

| Texte exact | Contexte |
|---|---|
| `#` / `Date echeance` / `Capital` / `Interets` / `Total` / `Capital restant` / `Statut` / `Action` | En-têtes colonnes |
| `Paye` | Échéance payée |
| `En attente` | Échéance non payée |
| `Marquer paye` | Bouton |

#### Libellés d'énumérations

**LoanStatus :**

| Valeur | Libelle |
|---|---|
| `EN_COURS` | `En cours` |
| `REMBOURSE` | `Rembourse` |
| `DEFAUT` | `En defaut` |
| `EN_ATTENTE` | `En attente d approbation` |
| `REFUSE` | `Refuse par le service credit` |

**LoanType :**

| Valeur | Libelle |
|---|---|
| `IMMOBILIER` | `Immobilier` |
| `AUTO` | `Automobile` |
| `CONSOMMATION` | `Consommation` |
| `TRAVAUX` | `Travaux` |
| `ETUDIANT` | `Etudiant` |

#### Messages FacesMessage (LoanBean)

| Identifiant | Texte exact | Condition | Preuve |
|---|---|---|---|
| MSG-007-120 | `Montant invalide` | `simMontant` null ou ≤ 0 | |
| MSG-007-121 | `Taux invalide (0-30%)` | `simTaux` hors plage | |
| MSG-007-122 | `Duree invalide (1-360 mois)` | `simDuree` hors plage | |
| MSG-007-123 | `Erreur simulation : {e.getMessage()}` | Exception | |
| MSG-007-124 | `Montant invalide` | `formMontant` null ou ≤ 0 | |
| MSG-007-125 | `Taux invalide` | `formTaux` hors plage (sans plage dans le texte) | |
| MSG-007-126 | `Duree invalide` | `formDuree` hors plage (sans plage dans le texte) | |
| MSG-007-127 | `Veuillez indiquer un motif` | Motif vide | |
| MSG-007-128 | `Erreur : {e.getMessage()}` | Exception demande | |
| MSG-007-129 | `Erreur : {e.getMessage()}` | Exception paiement | |
| MSG-007-130 | `Erreur suppression : {e.getMessage()}` | Exception suppression | |
| MSG-007-131 | `Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes)` | Succès soumission | |
| MSG-007-132 | `Echeance marquee comme payee` | Succès paiement | |

#### Notifications asynchrones (LoanApprovalMDB — tableau de bord)

| Identifiant | Type | Gabarit | Condition | Preuve |
|---|---|---|---|---|
| MSG-007-140 | `CONFIRMATION` | `Votre demande de pret {type.libelle} de {montant} EUR a ete approuvee par le service credit.` | Scoring favorable | |
| MSG-007-141 | `ALERTE` | `Votre demande de pret {type.libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive).` | Scoring défavorable | |

---

### FEAT-008 « Documents (relevés, attestations, IFU) »
_Source : documents.xhtml, DocumentBean.java, DocumentDownloadServlet.java_

#### Bandeau technique

| Identifiant | Texte exact | Preuve |
|---|---|---|
| MSG-008-030 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 — Generation PDF via OpenPDF (LGPL) + Servlet de telechargement` | |
| MSG-008-031 | `DocumentDownloadServlet : /banking-legacy/docs/download?type=RELEVE\|RIB\|ATTESTATION\|IFU` | |

#### Section IFU

| Texte exact | Contexte |
|---|---|
| `Documents fiscaux` | Séparateur section |
| `Imprime Fiscal Unique (IFU)` | Titre section |
| `Fiscal` | Badge type |
| `IFU {currentYear - 1}` | Titre carte |
| `IFU {currentYear - 2}` | Titre carte |
| `Revenus de capitaux mobiliers` | Sous-titre carte |
| `Telecharger PDF` | Lien téléchargement |

#### Section par compte (RIB, Attestation, Relevés)

| Identifiant | Texte exact | Contexte |
|---|---|---|
| MSG-008-050 | `Compte {acc.accountType} - {acc.accountNumber}` | Séparateur par compte |
| MSG-008-051 | `Bancaire` | Badge type — carte RIB |
| MSG-008-052 | `RIB` | Titre carte |
| MSG-008-053 | `Releve d Identite Bancaire` | Sous-titre |
| MSG-008-054 | `Telecharger PDF` | Lien RIB |
| MSG-008-055 | `Attestation` | Badge type |
| MSG-008-056 | `Attestation de solde` | Titre carte |
| MSG-008-057 | `Solde au jour de la demande` | Sous-titre |
| MSG-008-058 | `Telecharger PDF` | Lien attestation |
| MSG-008-059 | `Releves mensuels — {acc.accountNumber}` | En-tête liste relevés |
| MSG-008-060 | `{moisLibelle} {year}` | Lien mensuel (12 mois) |

#### Libellés de mois (DocumentBean.getMoisLibelle)

`Janvier`, `Février`, `Mars`, `Avril`, `Mai`, `Juin`, `Juillet`, `Août`, `Septembre`, `Octobre`, `Novembre`, `Décembre`

#### Messages d'erreur HTTP (DocumentDownloadServlet)

| Identifiant | Code HTTP | Texte exact | Condition |
|---|---|---|---|
| MSG-008-070 | 400 | `Parametre type manquant` | Paramètre `type` absent |
| MSG-008-071 | 400 | `accountId manquant` | Paramètre `accountId` absent |
| MSG-008-072 | 400 | `Type inconnu : {type}` | Type non reconnu |
| MSG-008-073 | 404 | _(message de l'`IllegalArgumentException`)_ | Compte introuvable |
| MSG-008-074 | 500 | `Erreur lors de la generation du document` | Exception inattendue |

#### Messages FacesMessage (DocumentBean — flux JSF, non liés au téléchargement direct)

| Sévérité | Texte exact | Condition | Preuve |
|---|---|---|---|
| ERROR | `Session expirée.` | `currentCustomer` null | |
| INFO | `Relevé {moisLibelle} {year} généré avec succès.` | Succès | |
| ERROR | `Impossible de générer le relevé.` | Retour null | |
| ERROR | `Erreur lors de la génération du relevé : {message}` | Exception | |
| INFO | `Attestation de solde générée avec succès.` | Succès | |
| ERROR | `Impossible de générer l'attestation.` | Retour null | |
| ERROR | `Erreur lors de la génération : {message}` | Exception | |

---

### FEAT-009 « Bourse et watchlist »
_Source : bourse.xhtml, BourseBean.java, AlphaVantageService.java_

#### Bandeau

| Texte exact | Preuve |
|---|---|
| `🌐 API externe — Données temps réel via Alpha Vantage REST API — EJB Stateless → HTTP → JSON → SVG \| 📦 Watchlist persistée en base H2 via JPA` | |

#### Barre d'outils

| Texte exact | Contexte |
|---|---|
| `Valeur` | Label sélecteur symbole |
| `({N} valeurs)` | Compteur catalogue |
| `Période` | Label sélecteur période |
| `1 Semaine` / `1 Mois` / `1 An` / `5 Ans` | Options de période |
| `+ Ajouter une valeur` | Bouton ouverture formulaire |

#### Formulaire d'ajout à la watchlist

| Texte exact | Contexte |
|---|---|
| `➕ Ajouter une valeur à la watchlist` | Titre formulaire |
| `Symbole *` / `ex: AMZN` / `Symbole Alpha Vantage` | Label / placeholder / aide |
| `Nom *` / `ex: Amazon` | Label / placeholder |
| `Marché` | Label sélecteur marché |
| `Euronext Paris` / `NASDAQ` / `NYSE` / `London SE` / `Autre` | Options marché |
| `Devise` | Label sélecteur devise |
| `EUR` / `USD` / `GBP` | Options devise |
| `Ajouter` / `Annuler` | Boutons |

#### Messages watchlist

| Texte exact | Condition | Preuve |
|---|---|---|
| `Le symbole est obligatoire (ex: AMZN, SG.PA)` | Symbole vide | |
| `Le nom de la valeur est obligatoire` | Nom vide | |
| `Le symbole « {SYMBOL} » existe déjà dans votre watchlist` | Doublon (casse insensible) | |
| `Valeur ajoutée !` | Succès ajout | |

#### Message d'absence de données

| Texte exact | Condition | Preuve |
|---|---|---|
| `⚠️ Aucune donnée reçue pour « {SYMBOL} » sur {PERIOD_LABEL}. Causes possibles : symbole invalide pour cette place boursière (vérifiez le suffixe, ex: .PA pour Euronext), ou limite de 25 requêtes/jour atteinte (plan gratuit Alpha Vantage). Réessayez dans quelques minutes ou changez de valeur.` | Données absentes | |

**Valeurs de `{PERIOD_LABEL}` :** `1 semaine (données journalières)` / `1 mois (données journalières)` / `1 an (données hebdomadaires)` / `5 ans (données hebdomadaires)`

#### KPI et graphique

| Texte exact | Contexte |
|---|---|
| `{name} — {symbol}` | Label KPI principal |
| `Variation sur la période` / `Performance %` / `Marché` | Labels KPI |
| `▲ +` / `▼ ` | Préfixes variation positive / négative |
| `📈 Évolution du cours` | Titre section graphique |
| `📋 Dernières séances` | Titre section tableau |
| `Date` / `Ouverture` / `Haut` / `Bas` / `Clôture` / `Volume` / `Var. J` | En-têtes colonnes tableau |

#### Watchlist

| Texte exact | Contexte |
|---|---|
| `⭐ Ma Watchlist ({N} valeurs)` | Titre section |
| `Symbole` / `Nom` / `Marché` / `Devise` / `Suppr.` | En-têtes colonnes |
| `✕` | Bouton suppression |
| `Supprimer {name} de la watchlist` | Infobulle |

---

### FEAT-010 « Chat avec le conseiller »
_Source : conseiller.xhtml, ConseillerBean.java, ConseillerService.java_

| Texte exact | Contexte |
|---|---|
| `BankApp Demo - Mon Conseiller` | Titre onglet |
| `Conseiller IA` / `GPT-5.4-nano` / `Azure AI Foundry` / `EJB Stateless WildFly 14` | Bandeau IA |
| `Sophie — Conseillère Demo Bank` | En-tête carte de chat |
| `En ligne · Répond en quelques secondes` | Indicateur disponibilité |
| `Nouvelle conversation` | Bouton réinitialisation |

#### Message de bienvenue

| Texte exact | Condition |
|---|---|
| `Bonjour {prenom} ! Je suis Sophie, votre conseillere Demo Bank. Je suis la pour repondre a vos questions sur vos comptes, vos placements ou tout projet financier. Comment puis-je vous aider aujourd'hui ?` | `currentCustomer` non null |
| `Bonjour cher client ! Je suis Sophie, votre conseillere Demo Bank. Je suis la pour repondre a vos questions sur vos comptes, vos placements ou tout projet financier. Comment puis-je vous aider aujourd'hui ?` | `currentCustomer` null |

#### Actions rapides (conversation fraîche uniquement)

| Libellé bouton | Message envoyé |
|---|---|
| `Optimiser mon epargne` | `Je voudrais optimiser mon epargne, que me conseillez-vous ?` |
| `Projet immobilier` | `Je souhaite acheter un appartement, comment preparer mon dossier ?` |
| `Investir en bourse` | `Je veux commencer a investir en bourse, par ou commencer ?` |
| `Preparer ma retraite` | `Comment preparer ma retraite efficacement ?` |

#### Zone de saisie

| Texte exact | Contexte |
|---|---|
| `Posez votre question a Sophie...` | Placeholder textarea |
| `Envoyer` | Bouton envoi |
| `...` | Valeur bouton pendant chargement AJAX |

#### Messages d'erreur backend IA (affichés comme bulle assistant)

| Texte exact | Condition | Preuve |
|---|---|---|
| `Je suis momentanement indisponible (HTTP {status}). Veuillez reessayer.` | Statut non-200 Azure OpenAI | |
| `Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence.` | Exception (timeout, réseau, parsing) | |

---

### FEAT-011 « Catalogue produits bancaires »
_Source : produits.xhtml, InvestmentProduct.java_

| Texte exact | Contexte |
|---|---|
| `BankApp Demo - Produits bancaires` | Titre onglet |
| `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 — JPA JOINED Inheritance (BankProduct → SavingsProduct, CreditProduct, InvestmentProduct)` | Bandeau technique |
| `Catalogue des produits` | Séparateur et titre section |
| `Rafraichir` | Bouton — toujours visible |

#### Épargne

| Texte exact | Contexte |
|---|---|
| `Produits d epargne` | Titre sous-section |
| `Nom` / `Description` / `Taux` / `Depot min` / `Capital garanti` / `Duree max` | En-têtes colonnes |
| `Aucun produit d epargne disponible` | État vide |
| `Oui` / `Non` | `capitalGuaranteed` |
| `{maxDurationMonths} mois` | Durée non null |
| `Illimite` | `maxDurationMonths` null (épargne) |

#### Crédit

| Texte exact | Contexte |
|---|---|
| `Produits de credit` | Titre sous-section |
| `Nom` / `Description` / `Taux` / `Duree max` / `LTV max` / `Assurance requise` | En-têtes colonnes |
| `Aucun produit de credit disponible` | État vide |
| `Oui` / `Non` | `requiresInsurance` |

#### Investissement

| Texte exact | Contexte |
|---|---|
| `Produits d investissement` | Titre sous-section |
| `Nom` / `Description` / `Secteur` / `Niveau de risque` / `Rendement attendu` | En-têtes colonnes |
| `Aucun produit d investissement disponible` | État vide |
| `{riskLevelLabel} ({riskLevel}/5)` | Gabarit niveau de risque |

#### Libellés de niveau de risque (InvestmentProduct.getRiskLevelLabel)

| Valeur `riskLevel` | Libelle |
|---|---|
| 1 | `Tres faible` |
| 2 | `Faible` |
| 3 | `Moyen` |
| 4 | `Eleve` |
| 5 | `Tres eleve` |
| hors [1-5] | `N/A` |

#### Valeur de substitution null

`—` (U+2014) — pour tout champ optionnel null : `interestRate`, `minOpeningDeposit`, `maxLtv`, `expectedReturn`, `marketSector`, `maxDurationMonths` (crédit uniquement)

#### Historique traitements nocturnes (section visible dans Produits)

| Texte exact | Contexte |
|---|---|
| `Traitements nocturnes (EJB @Schedule)` | Séparateur |
| `Historique des traitements nocturnes (EJB @Schedule)` | Titre |
| `Job` / `Debut` / `Fin` / `Duree (ms)` / `Enregistrements` / `Statut` / `Details` | En-têtes colonnes |
| `Aucun traitement nocturne enregistre — les jobs s executent a 02h00, 02h30 et le 1er du mois a 03h00` | État vide |
| `SUCCESS` / `FAILED` / `PARTIAL` | Valeurs de statut badge |

---

### FEAT-012 « Comptes Tiers — Open Banking Powens »
_Source : comptes-tiers.xhtml, PowensBean.java, PowensCallbackBean.java, ExternalAccount.java_

#### Bandeau et messages d'erreur

| Identifiant | Texte exact | Condition | Preuve |
|---|---|---|---|
| MSG-012-030 | `🏦 Open Banking — Agregation de comptes via Powens API (PSD2) — OAuth2 depuis un EJB WildFly 14` | Toujours | |
| MSG-012-040 | `Impossible d initialiser la connexion Powens.` | Échec init Powens | |
| MSG-012-041 | `Session expiree. Veuillez recommencer la connexion.` | Token absent | |
| MSG-012-042 | `Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes.` | Callback ok, liste vide | |
| MSG-012-043 | `Callback Powens incomplet. Parametres recus: {liste}` | Callback sans `connection_id` ni `code` | |

#### État non-connecté

| Identifiant | Texte exact |
|---|---|
| MSG-012-050 | `Agregez vos comptes bancaires` |
| MSG-012-051 | `Connectez vos comptes d autres banques pour avoir une vue complete de votre patrimoine financier. Connexion securisee via le protocole OAuth2 PSD2.` |
| MSG-012-052-057 | Logos banques : `BNP Paribas`, `Credit Agricole`, `La Banque Postale`, `CIC`, `LCL`, `+ 1800 banques` |
| MSG-012-058 | `Connecter une banque` |

#### État connecté

| Identifiant | Texte exact |
|---|---|
| MSG-012-060 | `Connected via Powens` |
| MSG-012-061 | `Deconnecter` |
| MSG-012-062 | `Patrimoine total (tous comptes)` |
| MSG-012-063 | `via Powens Open Banking API` |
| MSG-012-064 | `Mes comptes externes ({N})` |
| MSG-012-065 | `Banque externe` |

#### Libellés de type de compte externe (ExternalAccount.getTypeLabel)

| Valeur API (`type`) | Libelle affiché |
|---|---|
| `checking` | `Compte courant` |
| `savings` | `Epargne` |
| `card` | `Carte` |
| `loan` | `Credit` |
| `market` | `Bourse` |
| `null` | `Compte` |
| _(toute autre valeur)_ | `{type}` (valeur brute) |

#### Transactions

| Texte exact | Contexte |
|---|---|
| `Transactions — {nom}` | Titre section (compte sélectionné) |
| `Aucune transaction disponible.` | État vide |
| `Date` / `Libelle` / `Montant` | En-têtes colonnes |
| `+` | Préfixe montant crédit |

---

### FEAT-013 « Gestion des bénéficiaires externes »
_Source : beneficiaires.xhtml, BeneficiaireBean.java_

#### Formulaire

| Identifiant | Texte exact | Contexte |
|---|---|---|
| MSG-013-006 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 — Validation IBAN MOD-97 + BIC SEPA (zoneSEPA auto-detectee)` | Bandeau technique |
| MSG-013-030 | `Ajouter un beneficiaire` | Séparateur section |
| MSG-013-031 | `Nouveau beneficiaire SEPA` | Titre |
| MSG-013-032 | `Libelle *` | Label |
| MSG-013-033 | `Nom du titulaire *` | Label |
| MSG-013-034 | `IBAN *` | Label |
| MSG-013-035 | `ex: FR76 3000 1007 9412 3456 7890 185` | Placeholder IBAN |
| MSG-013-036 | `Verifier l'IBAN` | Bouton vérification Ajax |
| MSG-013-037-038 | `BIC` / `(optionnel)` | Label + suffixe |
| MSG-013-039 | `ex: BNPAFRPPXXX` | Placeholder BIC |
| MSG-013-040-041 | `Banque` / `(optionnel)` | Label + suffixe |
| MSG-013-042 | `ex: BNP Paribas` | Placeholder banque |
| MSG-013-043 | `Ajouter le beneficiaire` | Bouton soumission |

#### Messages vérification IBAN (Ajax)

| Identifiant | Type | Texte exact | Condition |
|---|---|---|---|
| MSG-013-060 | Erreur | `Veuillez saisir un IBAN.` | IBAN vide |
| MSG-013-061 | Erreur | `IBAN invalide (echec MOD-97). Verifiez la saisie.` | Échec MOD-97 |
| MSG-013-062 | Succès | `IBAN valide — {pays} (Zone SEPA)` | Valide + pays SEPA |
| MSG-013-063 | Succès | `IBAN valide — {pays} (Hors zone SEPA)` | Valide + hors SEPA |

#### Messages de soumission

| Identifiant | Sévérité | Texte exact | Condition |
|---|---|---|---|
| MSG-013-070 | ERREUR | `Session expirée, veuillez vous reconnecter.` | `currentCustomer` null |
| MSG-013-071 | ERREUR | `Le libellé est obligatoire.` | `libelle` vide |
| MSG-013-072 | ERREUR | `L'IBAN est obligatoire.` | `iban` vide |
| MSG-013-073 | ERREUR | `IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97).` | MOD-97 échoue à la soumission |
| MSG-013-074 | ERREUR | `Le nom du titulaire est obligatoire.` | `nomTitulaire` vide |
| MSG-013-075 | ERREUR | `BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques.` | BIC fourni + pattern invalide |
| MSG-013-076 | INFO | `Bénéficiaire ajouté avec succès.` | Succès |
| MSG-013-077 | ERREUR | `Erreur lors de l'ajout du bénéficiaire : {message}` | Exception |
| MSG-013-080 | ERREUR | `Erreur lors de la suppression.` | Exception suppression |

#### Tableau bénéficiaires

| Texte exact | Contexte |
|---|---|
| `Mes beneficiaires enregistres` | Séparateur section |
| `Beneficiaires ({beneficiaireBean.beneficiaires.size()})` | Titre avec compteur |
| `Libelle` / `Titulaire` / `IBAN` / `BIC` / `Banque` / `Zone SEPA` / `Action` | En-têtes colonnes |
| `Aucun beneficiaire enregistre. Utilisez le formulaire ci-dessus pour en ajouter un.` | État vide |
| `—` | Valeur BIC ou Banque null |
| `✓ SEPA` | Badge zone SEPA (`sepaZone` = true) |
| `Hors SEPA` | Badge hors zone (`sepaZone` = false) |
| `Supprimer` | Bouton suppression par ligne |

---

### FEAT-014 « Callback OAuth Powens »
_Source : powens-callback.xhtml, PowensCallbackBean.java, PowensBean.java_

> Note : la page redirige côté serveur avant tout rendu ; les textes ci-dessous ne sont
> jamais vus par l'utilisateur en conditions normales.

| Texte exact | Contexte |
|---|---|
| `Connexion bancaire en cours...` | Titre onglet (jamais visible) |
| `Connexion en cours...` | `<h2>` (jamais visible) |
| `Recuperation de vos comptes via Powens Open Banking` | `<p>` sous spinner (jamais visible) |

#### Messages d'erreur transmis à /comptes-tiers

| Identifiant | Texte exact | Condition |
|---|---|---|
| MSG-014-001 | `Session expiree. Veuillez recommencer la connexion.` | Token absent à la réception du callback |
| MSG-014-002 | `Callback Powens incomplet. Parametres recus: {params.keySet()}` | Aucun paramètre `error`, `connection_id` ni `code` |
| MSG-014-003 | `Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes.` | Callback ok, liste comptes vide |

---

### FEAT-015 « Espace Conseiller — Portefeuille clients »
_Source : conseiller-workspace.xhtml_

#### En-tête spécifique

| Texte exact | Contexte |
|---|---|
| `BankApp Demo - Espace Conseiller` | Titre onglet |
| `C` | Logo disque vert |
| `Conseiller` | Badge rôle (vert) |
| `Mon portefeuille` | Lien actif menu latéral |
| `Conseiller IA` | Lien menu latéral |
| `Espace Conseiller` | Bandeau accueil (gras) |
| `Bonjour {fullName}, vous gerez {N} client(s)` | Bandeau accueil |

#### KPI

| Texte exact | Contexte |
|---|---|
| `Mes clients` / `clients dans mon portefeuille` | KPI clients |
| `Encours total` / `solde cumulé mes clients` | KPI solde |
| `Prets en attente` / `a traiter` | KPI prêts en attente |

#### État vide portefeuille

| Texte exact |
|---|
| `Aucun client dans votre portefeuille.` |
| `Contactez l administrateur pour l affectation des clients.` |

#### Cartes clients

| Texte exact | Contexte |
|---|---|
| `Actif` / `Inactif` | Badge statut client |
| `Solde total` / `Comptes` / `Prets actifs` / `Prets en attente` | Labels statistiques |
| `⚠ {N} pret(s) en attente` | Indicateur (orange) |
| `✓ {N} pret(s) en cours` | Indicateur (vert) |
| `Voir detail` | Bouton |

#### Panneau de détail client

| Texte exact | Contexte |
|---|---|
| `Fermer` | Bouton fermeture |
| `Comptes bancaires` | Titre sous-section gauche |
| `Aucun compte` | État vide comptes |
| `Numero` / `Type` / `Solde` | En-têtes colonnes comptes |
| `Prets` | Titre sous-section droite |
| `Aucun pret` | État vide prêts |
| `Type` / `Montant` / `Mensualite` / `Statut` | En-têtes colonnes prêts |

#### Valeurs d'énumération (brutes — non localisées)

**Statuts de prêt :** `EN_ATTENTE` (badge jaune), `EN_COURS` (badge vert), `REFUSE` (badge rose), `REMBOURSE` (badge mauve), `DEFAUT` (sans badge)

**Types de compte :** `COURANT` / `EPARGNE` (badge bleu)

---

### FEAT-016 « Administration — Gestion globale »
_Source : admin.xhtml, AdminBean.java, FraudAlertBean.java_

#### En-tête

| Identifiant | Texte exact |
|---|---|
| MSG-016-001 | `BankApp Demo - Administration` |
| MSG-016-004 | `ADMIN` (badge rôle) |
| MSG-016-020 | `Espace Administration — JEE 8 / JSF 2.3 / EJB 3.2 / RBAC custom — Role : ADMIN` |

#### Navigation Admin

| Texte exact |
|---|
| `Administration` / `Espace Conseiller` / `Mon tableau de bord` |

#### KPI

| Texte exact | Contexte |
|---|---|
| `Total clients` / `{n} actifs` | KPI clients |
| `Conseillers` / `actifs` | KPI conseillers |
| `Prets en attente` / `{montantTotalPending EUR} a traiter` | KPI prêts |
| `Total utilisateurs` / `tous roles confondus` | KPI utilisateurs |

#### Pipeline Prêts

| Texte exact | Contexte |
|---|---|
| `Pipeline Prets — En attente d approbation` | Titre section |
| `Rafraichir` | Bouton |
| `Aucun pret en attente` | État vide |
| `ID` / `Client` / `Type` / `Montant` / `Duree` / `Taux` / `Mensualite` / `Motif` / `Actions` | En-têtes colonnes |
| `Approuver` / `Refuser` | Boutons d'action |

#### Gestion clients

| Texte exact | Contexte |
|---|---|
| `Gestion des clients` | Titre section |
| `Rechercher par nom, login, email...` | Placeholder filtre |
| `Filtrer` | Bouton |
| `Aucun client trouve` | État vide |
| `Nom` / `Login` / `Email` / `Role` / `Statut` / `Conseiller` / `Actions` | En-têtes colonnes |
| `Actif` / `Inactif` | Badges statut |
| `Detail` / `Desactiver` / `Activer` | Boutons actions |

#### Panneau détail client (admin)

| Texte exact | Contexte |
|---|---|
| `Detail : {adminBean.selectedClient.fullName}` | Titre panneau |
| `Fermer` | Bouton |
| `Nom complet` / `Identifiant` / `Email` / `Telephone` / `Role` / `Statut` / `Conseiller assigne` / `Client depuis` | Labels champs |

#### Historique prêts

| Texte exact |
|---|
| `Historique de tous les prets` |
| `Aucun pret en base` |
| `ID` / `Client` / `Type` / `Montant` / `Statut` / `Duree` / `Mensualite` / `Date debut` |

#### Tous les utilisateurs

| Texte exact |
|---|
| `Tous les utilisateurs du systeme` |
| `Nom` / `Login` / `Email` / `Role` / `Statut` |

#### Alertes Fraude

| Identifiant | Texte exact | Condition |
|---|---|---|
| MSG-016-090 | `Alertes Fraude — Moteur de détection` | Toujours |
| MSG-016-091 | `{fraudAlertBean.openCount} ouverte(s)` | Si `openCount > 0` |
| MSG-016-092 | `Rafraichir` | Toujours |
| MSG-016-093 | `Aucune alerte fraude enregistrée` | État vide |
| MSG-016-094 | `{alert.riskScore}/100` | Par ligne |
| MSG-016-095 | `Confirmer` | Si `alert.status == OPEN` |
| MSG-016-096 | `Faux positif` | Si `alert.status == OPEN` |
| MSG-016-097 | `Traité par {alert.reviewedBy}` | Si `status != OPEN` |

En-têtes colonnes alertes : `ID` / `Client` / `Règle déclenchée` / `Score risque` / `Statut` / `Description` / `Date` / `Actions`

Noms de client fallback (FraudAlertBean) : `—` (null), `Client #{customerId} (introuvable)`, `Client #{customerId} (erreur)`

#### Traitements nocturnes (section admin)

| Texte exact |
|---|
| `Traitements nocturnes (EJB Timer @Schedule)` |
| `Rafraichir` |
| `Aucun traitement nocturne enregistre` |
| `Job` / `Debut` / `Fin` / `Duree (ms)` / `Enregistrements` / `Statut` / `Details` |

#### Catalogue produits bancaires (section admin — miroir de FEAT-011)

| Texte exact | Contexte |
|---|---|
| `Catalogue produits bancaires` | Titre section |
| `Voir la page Produits` | Lien vers FEAT-011 |
| `Produits d epargne` / `Produits de credit` / `Produits d investissement` | Sous-titres |
| `Aucun produit` | État vide (répété par catégorie) |
| `Oui` / `Non` | Booléens |
| `Illimite` | Durée null |
| `{riskLevelLabel} ({riskLevel}/5)` | Niveau de risque |

#### Messages FacesMessage (AdminBean)

| Identifiant | Sévérité | Texte exact | Condition |
|---|---|---|---|
| MSG-016-130 | INFO | `Client désactivé` | Désactivation réussie |
| MSG-016-131 | INFO | `Client activé` | Activation réussie |
| MSG-016-132 | INFO | `Prêt approuvé` | Approbation réussie |
| MSG-016-133 | INFO | `Prêt refusé` | Refus réussi |
| MSG-016-134 | ERROR | `Erreur : {e.getMessage()}` | Exception — dette-legacy (trace Java exposée) |

---

## Partie 3 — Textes persistés hors UI

### FEAT-017 « Téléchargement documents PDF »
_Contenu des PDF générés par DocumentService_

#### PDF Relevé de compte (type=RELEVE)

| Texte exact | Contexte |
|---|---|
| `Demo Bank — Releve de compte` | Titre PDF |
| `Compte` / `Titulaire` / `Periode` / `Solde actuel` | Labels métadonnées |
| `Date` / `Description` / `Type` / `Montant` / `Solde apres` | En-têtes colonnes |
| `CREDIT` / `DEBIT` | Valeurs type |
| `+{montant} EUR` / `-{montant} EUR` | Format montant |
| `Aucune operation sur cette periode.` | Message vide |

#### PDF RIB (type=RIB)

| Texte exact | Contexte |
|---|---|
| `Demo Bank — Releve d Identite Bancaire (RIB)` | Titre |
| `Titulaire` / `Banque` / `Code banque` / `Code guichet` / `Numero de compte` / `Cle RIB` / `IBAN` / `BIC / SWIFT` / `Domiciliation` | Labels |
| `Demo Bank SA` | Valeur fixe banque |
| `30004` | Code banque (hardcodé) |
| `00001` | Code guichet (hardcodé) |
| `42` | Clé RIB (hardcodée) |
| `DEMOFR21XXX` | BIC/SWIFT (hardcodé) |
| `Demo Bank - Agence Paris Centre` | Domiciliation (hardcodée) |
| `Document genere le {dd/MM/yyyy} - Demo Bank SA, 1 Place Vendome, 75001 Paris` | Note de bas |

#### PDF Attestation de solde (type=ATTESTATION)

| Texte exact | Contexte |
|---|---|
| `Demo Bank — Attestation de Solde` | Titre |
| `Demo Bank SA, dont le siege social est situe 1 Place Vendome, 75001 Paris, atteste par la presente que :` | Texte introductif |
| `M. / Mme {fullName}` | Corps — identité |
| `est titulaire du compte n° {accountNumber}` | Corps |
| `Le solde de ce compte, arrete au {dd MMMM yyyy}, est de :` | Corps |
| `Cette attestation est delivree a la demande de l interesse(e) pour faire valoir ce que de droit.` | Formule de clôture |
| `Fait a Paris, le {dd MMMM yyyy}` | Date |

#### PDF IFU (type=IFU)

| Texte exact | Contexte |
|---|---|
| `Demo Bank — Imprime Fiscal Unique (IFU) {year}` | Titre |
| `Etablissement` / `SIREN` / `Titulaire` / `Adresse` / `Annee fiscale` | Labels |
| `Demo Bank SA` | Valeur établissement (hardcodée) |
| `123 456 789 00012` | SIRET (hardcodé — FIXME JIRA-8021) |
| `1 rue de la Paix, 75001 Paris` | Adresse (hardcodée) |
| `Nature des revenus` / `Montant brut` | En-têtes colonnes |
| `Interets et produits de placement` | tableau fiscal |
| `Prelevement Forfaitaire Unique (30%)` | tableau fiscal |
| `Ce document est fourni a titre informatif. Il recense les revenus de capitaux mobiliers soumis au PFU pour l annee {year}.` | Note de bas |
| `Genere le {dd/MM/yyyy}` | Date |

#### Pied de page commun à tous les PDF

`Demo Bank SA — Capital 500 000 000 EUR — RCS Paris 123 456 789 00012 — Établissement de crédit agréé par l'ACPR`

---

### FEAT-018 « Calcul des intérêts nocturnes »

| Identifiant | Texte exact | Type | Preuve |
|---|---|---|---|
| MSG-018-001 | `Interets mensuels livret epargne (batch nocturne)` | Description transaction CREDIT (visible relevé) | |
| MSG-018-010 | `INTEREST_CALCULATION` | `jobName` dans BATCH_JOB_LOGS | |
| MSG-018-013 | `Interets calcules sur {N} comptes epargne` | `details` — succès | |
| MSG-018-014 | `Erreur: {message}` | `details` — échec | |

---

### FEAT-019 « Détection fraude nocturne par batch »

| Identifiant | Texte exact | Type | Preuve |
|---|---|---|---|
| MSG-019-001 | `FRAUD_DETECTION` | `job_name` BATCH_JOB_LOGS | |
| MSG-019-004 | `Transactions suspectes detectees: {N} transaction(s) > 10000 EUR` | `details` — succès | |
| MSG-019-005 | `Erreur: {message}` | `details` — échec | |

---

### FEAT-020 « Relevés mensuels — batch stub (dette-legacy) »

| Identifiant | Texte exact | Type | Condition | Preuve |
|---|---|---|---|---|
| MSG-020-010 | `Releves mensuels generes pour {N} clients` | `details` BATCH_JOB_LOGS — succès (libellé trompeur : aucun fichier généré) | Statut SUCCESS | |
| MSG-020-011 | `Erreur: {e.getMessage()}` | `details` — échec | Statut FAILED | |

> `MONTHLY_STATEMENTS` est la valeur du `jobName` persisté.

---

### FEAT-021 « Exécution des virements programmés »

#### Descriptions de transactions (user-visible — relevé de compte)

| Identifiant | Gabarit | Contexte |
|---|---|---|
| MSG-021-001 | `Virement programme vers {targetLabel}` | DEBIT — sans motif |
| MSG-021-002 | `Virement programme vers {targetLabel} - {motif}` | DEBIT — avec motif |
| MSG-021-003 | `Virement programme de {sourceAccountNumber}` | CREDIT interne — sans motif |
| MSG-021-004 | `Virement programme de {sourceAccountNumber} - {motif}` | CREDIT interne — avec motif |

---

### FEAT-022 « Surveillance soldes faibles »

#### Notification persistée (user-visible — centre notifications FEAT-002)

| Identifiant | Champ | Texte exact | Preuve |
|---|---|---|---|
| MSG-022-001 | Titre | `Solde insuffisant sur votre compte` | |
| MSG-022-002 | Corps | `Votre solde ({currentBalance} €) est passé sous le seuil d'alerte de {lowBalanceThreshold} €. Veuillez approvisionner votre compte.` | |
| MSG-022-003 | Type | `ACCOUNT_BALANCE` | |
| MSG-022-004 | Priorité | `HIGH` | |

---

### FEAT-023 « Traitement asynchrone des virements (JMS) »

> Les messages user-visible sont identiques à ceux de FEAT-003 (VirementProcessorMDB) et FEAT-002.

#### Icônes et classes CSS de notification (BankNotification)

| Type | Icône | Classe CSS |
|---|---|---|
| `CONFIRMATION` | `✅` | `notif-confirmation` |
| `ALERTE` | `⚠️` | `notif-alerte` |
| `FRAUDE` | `🚨` | `notif-fraude` |

---

### FEAT-024 « Traitement asynchrone approbation prêts (JMS) »

#### Notifications client (LoanApprovalMDB — tableau de bord)

| Identifiant | Type | Gabarit | Condition | Preuve |
|---|---|---|---|---|
| MSG-024-001 | `CONFIRMATION` | `Votre demande de pret {libelle} de {montant} EUR a ete approuvee par le service credit.` | Scoring favorable | |
| MSG-024-002 | `ALERTE` | `Votre demande de pret {libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive).` | Scoring défavorable | |

---

### FEAT-025 « Système de notifications internes »

> Synthèse des pipelines. Le pipeline in-memory (JMS) est le seul actif et visible.
> Le pipeline JPA (NotificationService) est inactif — son consommateur (FEAT-032) est code mort.

#### Pipeline JPA inactif (NotificationService — persisté en base, non affiché)

| Identifiant | Champ | Texte exact | Condition |
|---|---|---|---|
| MSG-025-030-T | Titre | `Solde insuffisant sur votre compte` | Solde < 100 EUR |
| MSG-025-030-M | Corps | `Votre solde ({currentBalance} €) est passé sous le seuil d'alerte de {lowBalanceThreshold} €. Veuillez approvisionner votre compte.` | Solde < 100 EUR |
| MSG-025-031-T | Titre | `Prêt approuvé` | Prêt approuvé JPA |
| MSG-025-031-M | Corps | `Votre demande de prêt (réf. {loanId}) a été approuvée. Votre conseiller vous contactera sous 48 h.` | Prêt approuvé JPA |
| MSG-025-032-T | Titre | `Prêt refusé` | Prêt refusé JPA |
| MSG-025-032-M | Corps | `Votre demande de prêt (réf. {loanId}) a été refusée. Vous pouvez contacter votre conseiller pour plus d'informations.` | Prêt refusé JPA |
| MSG-025-033-T | Titre | `Alerte transaction` | `createTransactionNotification` (jamais appelée) |
| MSG-025-034-T | Titre | `Mise à jour de votre prêt` | `createLoanNotification` (jamais appelée) |

---

### FEAT-026 « Audit trail »

#### Codes d'action (champ `action` de `audit_logs`)

| Code | Déclencheur | Détail persisté |
|---|---|---|
| `LOGIN` | Authentification réussie | `Connexion réussie` |
| `LOGIN_FAILED` | Échec authentification | `Tentative échouée` |
| `LOGOUT` | Déconnexion | `Déconnexion` |
| `ADMIN_DEACTIVATE` | Désactivation client | `Désactivation client id={customerId}` |
| `ADMIN_ACTIVATE` | Activation client | `Activation client id={customerId}` |
| `ADMIN_LOAN_APPROVE` | Approbation prêt | `Approbation prêt id={loanId}` |
| `ADMIN_LOAN_REFUSE` | Refus prêt | `Refus prêt id={loanId}` |
| `VIREMENT` | Virement réussi | `Montant: {amount} - Motif: {motif}` |

---

### FEAT-027 « Détection de fraude en temps réel »

#### Descriptions d'alertes (FraudDetectionService — en anglais, dette identifiée)

| Règle | Texte exact |
|---|---|
| `LARGE_AMOUNT` (≥ 15 000 EUR) | `Transaction amount {amount} exceeds the very-large threshold of {threshold}` |
| `LARGE_AMOUNT` (≥ 5 000 EUR et < 15 000 EUR) | `Transaction amount {amount} exceeds the large threshold of {threshold}` |
| `HIGH_FREQUENCY` | `Customer has {count} transactions today, exceeding the daily limit of {maxDailyTransactions}` |
| `NEW_BENEFICIARY` | `Target IBAN {iban} is not registered as a trusted beneficiary` |
| `UNUSUAL_HOURS` | `Transaction initiated at hour {hourOfDay} which is outside normal operating hours (06:00-23:00)` |
| `ROUND_AMOUNT` | `Transaction amount {amount} is a suspicious round figure (multiple of 1000)` |
| `FOREIGN_IBAN` | `Target IBAN {iban} does not start with FR (possible foreign transfer)` |
| `RAPID_SUCCESSION` | `Previous transaction was only {elapsedSeconds} seconds ago (threshold: {rapidSuccessionSeconds}s)` |
| `VELOCITY_CHECK` | `Customer executed {highValueCount} transactions above 500 EUR in the last {velocityWindowMinutes} minutes (limit: {velocityMaxCount})` |

> Ces descriptions sont en anglais alors que l'interface est en français — dette à homogénéiser dans la cible.

#### Valeurs d'énumération FraudStatus (affichées dans admin.xhtml)

| Valeur | Badge couleur |
|---|---|
| `OPEN` | Rouge (#E3000F) |
| `CONFIRMED` | Orange (#ff6600) |
| `FALSE_POSITIVE` | Vert (#4caf50) |
| `REVIEWED` | Gris (#888) |

---

### FEAT-028 « Intégration Powens »

> Messages identiques à ceux de FEAT-012 et FEAT-014 (même code source).
> Référence croisée : MSG-012-040 à MSG-012-043, MSG-014-001 à MSG-014-003.

---

### FEAT-029 « Intégration AlphaVantage »

> Messages UI identiques à FEAT-009 (même page bourse.xhtml, même bean BourseBean).
> Référence croisée : FEAT-009.

---

### FEAT-030 « Contrôle d'accès RBAC »

> Aucun texte propre à FEAT-030 affiché à l'utilisateur : les redirections sont silencieuses.
> Messages HTTP de `DocumentDownloadServlet` identiques à FEAT-008 MSG-008-070 à MSG-008-074.
> Liens conditionnels `Espace Conseiller` / `Administration` documentés en Partie 1 §1.4.

---

## Partie 4 — Formats i18n observés

| Domaine | Format observé | Valeur réelle constatée | Preuve |
|---|---|---|---|
| Date (UI) | `dd/MM/yyyy` | `14/01/2024` (date dans FIELDS-016) | project_config.yaml + |
| Date-heure (UI) | `dd/MM/yyyy HH:mm` | Colonnes tableaux transactions | FIELDS-005, FIELDS-016 |
| Date (placeholder formulaire) | `yyyy-MM-dd` | Champ dateExec — FEAT-004 | |
| Date longue (PDF) | `dd MMMM yyyy` | PDF attestation, PDF RIB | |
| Date courte (PDF) | `dd/MM/yyyy` | PDF relevé, IFU | |
| Date fraude (tableau admin) | `dd/MM/yy HH:mm` | Colonne Date alertes fraude | |
| Nombre — séparateur décimal | Virgule (`,`) | `100,00` | (indice champ montant) |
| Nombre — séparateur milliers | Espace | `1 000,00` | project_config.yaml (number_format) |
| Devise | EUR | Symbole `€`, code `EUR`, format `€X,XX` | releve.xhtml |
| Soldes (UI) | `€{montant}` avec 2 décimales min | `€1 250,00` | `f:convertNumber type="currency" currencySymbol="€" minFractionDigits="2"` |
| Soldes (notifications JMS) | `{valeur BigDecimal brute}` | `50.00` (sans formatage) | |
| Langue de l'interface | Français | — | project_config.yaml |
| Langue des descriptions fraude | Anglais | — | FraudDetectionService.java (dette à homogénéiser) |
