---
feature: FEAT-004
name: Virements programmés
status: current
step: retro-rules
date: 2026-10-03
sources:
  - src/main/webapp/virement-programme.xhtml
  - src/main/java/com/accenture/bank/web/RecurringTransferBean.java
  - src/main/java/com/accenture/bank/ejb/RecurringTransferService.java
  - src/main/java/com/accenture/bank/entity/RecurringFrequency.java
---

# FEAT-004 — Virements programmés : Messages et libellés

Tous les textes visibles à l'écran (libellés, boutons, colonnes, erreurs, états) au mot près, tels qu'ils figurent dans le DOM ou sont émis par le code. Les `{variables}` désignent des valeurs dynamiques.

---

## Formulaire de création et tableau des virements

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Titre de navigateur | BankApp Demo - Virements programmes | Onglet du navigateur |
| Titre de section | Nouveau virement programme | Séparateur au-dessus du formulaire de création |
| Titre de section | Mes virements programmes | Séparateur au-dessus du tableau |
| Libellé | Compte debiteur | Champ `srcAcc` |
| Libellé | Montant (EUR) | Champ `montant` |
| Libellé | Frequence | Champ `freq` |
| Libellé | IBAN / N compte beneficiaire | Champ `targetNum` |
| Libellé | Libelle beneficiaire | Champ `targetLabel` |
| Libellé | Date de premiere execution | Champ `dateExec` |
| Libellé | Motif (optionnel) | Champ `motif` |
| Placeholder | ex: 200.00 | Champ `montant` |
| Placeholder | ex: FR76-0001-0002 | Champ `targetNum` |
| Placeholder | ex: Compte epargne | Champ `targetLabel` |
| Placeholder | yyyy-MM-dd | Champ `dateExec` |
| Placeholder | ex: Epargne mensuelle | Champ `motif` |
| Valeur par défaut | -- Choisir -- | Option vide du sélecteur `srcAcc` |
| Libellé dynamique | {accountNumber} ({accountType}) - {balance} EUR | Libellé de chaque option du sélecteur `srcAcc` |
| Option | Hebdomadaire | Fréquence |
| Option | Mensuel | Fréquence (valeur par défaut du sélecteur) |
| Option | Bimestriel | Fréquence |
| Option | Trimestriel | Fréquence |
| Option | Semestriel | Fréquence |
| Option | Annuel | Fréquence |
| Action | Creer le virement programme | Bouton de soumission du formulaire |
| Colonne | Source | En-tête du tableau |
| Colonne | Beneficiaire | En-tête du tableau |
| Colonne | Montant | En-tête du tableau |
| Colonne | Frequence | En-tête du tableau |
| Colonne | Prochain | En-tête du tableau (date de prochaine exécution) |
| Colonne | Nb executions | En-tête du tableau |
| Colonne | Etat | En-tête du tableau |
| Colonne | Actions | En-tête du tableau |
| Valeur d'état | Actif | Virement actif dans la colonne Etat |
| Valeur d'état | Inactif | Virement suspendu dans la colonne Etat |
| Action | Suspendre | Bouton affiché sur un virement actif |
| Action | Reactiver | Bouton affiché sur un virement inactif |
| Action | Supprimer | Bouton affiché sur tout virement |
| Message vide | Aucun virement programme configure | Liste vide |

---

## Messages de validation et de retour utilisateur

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Erreur de validation | Veuillez selectionner un compte source | Compte source non sélectionné |
| Erreur de validation | Veuillez saisir le numero de compte destinataire | Champ `targetNum` vide |
| Erreur de validation | Veuillez saisir le libelle du beneficiaire | Champ `targetLabel` vide |
| Erreur de validation | Montant invalide | Montant nul ou négatif (contrôle UI) |
| Erreur de validation | Veuillez saisir la date de premiere execution | Champ `dateExec` vide |
| Erreur de validation | Format de date invalide (attendu : yyyy-MM-dd) | Format de date non conforme à yyyy-MM-dd |
| Succès | Virement programme cree avec succes | Création réussie |
| Erreur technique | Erreur : {message} | Exception lors de la création ou du toggle (propagation du message de l'exception) |
| Erreur technique | Erreur suppression : {message} | Exception lors de la suppression (propagation du message de l'exception) |

---

## Messages de la couche service propagés via `Erreur : {message}`

Ces textes apparaissent dans l'interface encadrés par le préfixe `Erreur : ` lorsqu'une exception remonte jusqu'au bean.

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Exception service | Client ou compte introuvable | Client ou compte source absent en base |
| Exception service | Compte non autorise | Compte source n'appartient pas au client authentifié |
| Exception service | Montant invalide | Montant invalide détecté à la couche service (double vérification) |

---

## Descriptions de transaction générées par le scheduler (visibles dans le relevé — FEAT-005)

Ces textes ne sont pas affichés sur la page de virements programmés ; ils sont persistés dans la table de transactions et apparaissent dans le relevé de compte.

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Description CREDIT | Virement programme de {sourceIBAN} | Transaction CREDIT créditée sur le compte bénéficiaire interne |
| Description CREDIT (avec motif) | Virement programme de {sourceIBAN} - {motif} | Idem, lorsque le champ motif est renseigné |
| Description DEBIT | Virement programme vers {targetLabel} | Transaction DEBIT débitée sur le compte source |
| Description DEBIT (avec motif) | Virement programme vers {targetLabel} - {motif} | Idem, lorsque le champ motif est renseigné |

---

## Éléments d'en-tête et de navigation visibles sur la page (communs à toutes les pages)

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Marque | Demo Bank | Logo/titre de l'application dans la barre d'en-tête |
| Greeting | Bonjour, {fullName} | Nom du client connecté dans la barre d'en-tête |
| Action | Deconnexion | Bouton de déconnexion dans la barre d'en-tête |
| Navigation | Tableau de bord | Lien vers dashboard.xhtml |
| Navigation | Virement | Lien vers virement.xhtml |
| Navigation | Virements programmes | Lien actif (cette page) |
| Navigation | Releve | Lien vers releve.xhtml |
| Navigation | Budget | Lien vers budget.xhtml |
| Navigation | Prets | Lien vers pret.xhtml |
| Navigation | Documents | Lien vers documents.xhtml |
| Navigation | Bourse | Lien vers bourse.xhtml |
| Navigation | Mon Conseiller | Lien vers conseiller.xhtml |
| Navigation | Produits | Lien vers produits.xhtml |
| Navigation | Comptes Tiers | Lien vers comptes-tiers.xhtml |
| Navigation | Beneficiaires | Lien vers beneficiaires.xhtml |
| Navigation | Espace Conseiller | Lien conditionnel (rôle ADMIN ou CONSEILLER) |
| Navigation | Administration | Lien conditionnel (rôle ADMIN uniquement) |
