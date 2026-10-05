---
feature: FEAT-017
name: Téléchargement documents PDF
status: current
step: retro-rules
date: 2026-10-03
sources:
  - src/main/webapp/documents.xhtml
  - src/main/java/com/accenture/bank/web/DocumentDownloadServlet.java
  - src/main/java/com/accenture/bank/ejb/DocumentService.java
  - src/main/java/com/accenture/bank/web/DocumentBean.java
---

# FEAT-017 — Téléchargement documents PDF : Messages et libellés

Tous les textes visibles à l'écran (libellés, liens, titres), les messages d'erreur HTTP renvoyés par la servlet, et les textes imprimés dans les PDF générés, au mot près, tels qu'ils figurent dans le source. Les `{variables}` désignent des valeurs dynamiques.

---

## Page documents.xhtml — En-tête, navigation et bannière

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Titre de navigateur | BankApp Demo - Documents | Onglet du navigateur |
| Marque | Demo Bank | Logo/titre de l'application dans la barre d'en-tête |
| Greeting | Bonjour, {fullName} | Nom du client connecté dans la barre d'en-tête |
| Action | Deconnexion | Bouton de déconnexion dans la barre d'en-tête |
| Navigation | Tableau de bord | Lien vers dashboard.xhtml |
| Navigation | Virement | Lien vers virement.xhtml |
| Navigation | Virements programmes | Lien vers virement-programme.xhtml |
| Navigation | Releve | Lien vers releve.xhtml |
| Navigation | Budget | Lien vers budget.xhtml |
| Navigation | Prets | Lien vers pret.xhtml |
| Navigation | Documents | Lien actif (cette page) |
| Navigation | Bourse | Lien vers bourse.xhtml |
| Navigation | Mon Conseiller | Lien vers conseiller.xhtml |
| Navigation | Produits | Lien vers produits.xhtml |
| Navigation | Comptes Tiers | Lien vers comptes-tiers.xhtml |
| Navigation | Beneficiaires | Lien vers beneficiaires.xhtml |
| Navigation | Espace Conseiller | Lien conditionnel (rôle ADMIN ou CONSEILLER) |
| Navigation | Administration | Lien conditionnel (rôle ADMIN uniquement) |
| Bannière legacy | Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 — Generation PDF via OpenPDF (LGPL) + Servlet de telechargement | Bandeau informatif |
| Indication technique | DocumentDownloadServlet : /banking-legacy/docs/download?type=RELEVE\|RIB\|ATTESTATION\|IFU | Texte d'aide dans la bannière |

---

## Page documents.xhtml — Section IFU

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Séparateur de section | Documents fiscaux | Label du séparateur |
| Titre de section | Imprime Fiscal Unique (IFU) | En-tête de la section |
| Type de document | Fiscal | Badge de type sur les cartes IFU |
| Titre de document | IFU {currentYear - 1} | Titre de la carte IFU de l'année précédente |
| Titre de document | IFU {currentYear - 2} | Titre de la carte IFU de l'avant-dernière année |
| Sous-titre | Revenus de capitaux mobiliers | Sous-titre des cartes IFU |
| Action | Telecharger PDF | Lien de téléchargement sur les cartes IFU |

---

## Page documents.xhtml — Section par compte (RIB, Attestation, Relevés)

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Séparateur de section | Compte {accountType} - {accountNumber} | Label du séparateur par compte |
| Type de document | Bancaire | Badge de type sur la carte RIB |
| Titre de document | RIB | Titre de la carte RIB |
| Sous-titre | Releve d Identite Bancaire | Sous-titre de la carte RIB |
| Action | Telecharger PDF | Lien de téléchargement RIB |
| Type de document | Attestation | Badge de type sur la carte attestation |
| Titre de document | Attestation de solde | Titre de la carte attestation |
| Sous-titre | Solde au jour de la demande | Sous-titre de la carte attestation |
| Action | Telecharger PDF | Lien de téléchargement attestation |
| En-tête section relevés | Releves mensuels — {accountNumber} | En-tête de la liste des relevés par compte |
| Lien relevé mensuel | {moisLibelle} {year} | Lien de téléchargement de chaque relevé mensuel (12 mois affichés) |

---

## Messages d'erreur HTTP — API `GET /docs/download`

Textes renvoyés dans le corps de la réponse HTTP (via `sendError`) par `DocumentDownloadServlet.java`.

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Erreur HTTP 400 | Parametre type manquant | Paramètre `type` absent |
| Erreur HTTP 400 | accountId manquant | Paramètre `accountId` absent, vide ou non numérique (RELEVE, RIB, ATTESTATION) |
| Erreur HTTP 400 | Type inconnu : {type} | Valeur du paramètre `type` non reconnue |
| Erreur HTTP 500 | Erreur lors de la generation du document | Erreur interne lors de la génération PDF |

---

## Messages d'exception propagés depuis `DocumentService` (via `IllegalArgumentException`)

Ces textes sont renvoyés dans le corps HTTP 404 lorsque l'entité demandée est introuvable. En pratique, pour RELEVE, RIB et ATTESTATION, le défaut `@TransactionAttribute(SUPPORTS)` empêche leur remontée correcte (HTTP 500 au lieu de 404 — voir VALIDATION.md).

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Exception service → HTTP 404 (intent) | Compte introuvable | Compte bancaire non trouvé en base |
| Exception service → HTTP 404 (intent) | Client introuvable | Client non trouvé en base (type IFU) |

---

## Messages JSF — `DocumentBean` (FacesMessage, non liés au téléchargement direct)

Ces messages apparaissent via l'interface JSF et concernent la génération de documents persistés en base (flux différent du téléchargement direct par servlet).

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Erreur | Session expirée. | Session nulle lors de l'appel à `generateReleve` ou `generateAttestation` |
| Succès | Relevé {moisLibelle} {year} généré avec succès. | Génération d'un relevé persisté réussie |
| Erreur | Impossible de générer le relevé. | `generateReleveDocument` retourne null |
| Erreur | Erreur lors de la génération du relevé : {message} | Exception lors de la génération du relevé |
| Succès | Attestation de solde générée avec succès. | Génération d'une attestation persistée réussie |
| Erreur | Impossible de générer l'attestation. | `generateAttestationDocument` retourne null |
| Erreur | Erreur lors de la génération : {message} | Exception lors de la génération de l'attestation |

---

## Contenu PDF — Relevé de compte (type=RELEVE)

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Titre du document | Demo Bank — Releve de compte | En-tête du PDF |
| Label info | Compte | Ligne de métadonnées |
| Label info | Titulaire | Ligne de métadonnées |
| Label info | Periode | Ligne de métadonnées |
| Label info | Solde actuel | Ligne de métadonnées |
| En-tête colonne | Date | En-tête du tableau des transactions |
| En-tête colonne | Description | En-tête du tableau des transactions |
| En-tête colonne | Type | En-tête du tableau des transactions |
| En-tête colonne | Montant | En-tête du tableau des transactions |
| En-tête colonne | Solde apres | En-tête du tableau des transactions |
| Valeur de type | CREDIT | Type de transaction crédit (valeur d'enum) |
| Valeur de type | DEBIT | Type de transaction débit (valeur d'enum) |
| Format montant CREDIT | +{montant} EUR | Montant crédit dans le tableau |
| Format montant DEBIT | -{montant} EUR | Montant débit dans le tableau |
| Format montant ligne info | {montant} EUR | Solde actuel dans les métadonnées |
| Message vide | Aucune operation sur cette periode. | Aucune transaction pour la période |

---

## Contenu PDF — RIB (type=RIB)

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Titre du document | Demo Bank — Releve d Identite Bancaire (RIB) | En-tête du PDF |
| Label info | Titulaire | Ligne de métadonnées |
| Label info | Banque | Ligne de métadonnées |
| Valeur fixe | Demo Bank SA | Nom de la banque |
| Label info | Code banque | Ligne de métadonnées |
| Valeur fixe | 30004 | Code banque hardcodé |
| Label info | Code guichet | Ligne de métadonnées |
| Valeur fixe | 00001 | Code guichet hardcodé |
| Label info | Numero de compte | Ligne de métadonnées |
| Label info | Cle RIB | Ligne de métadonnées |
| Valeur fixe | 42 | Clé RIB hardcodée |
| Label info | IBAN | Ligne de métadonnées |
| Label info | BIC / SWIFT | Ligne de métadonnées |
| Valeur fixe | DEMOFR21XXX | Code BIC/SWIFT hardcodé |
| Label info | Domiciliation | Ligne de métadonnées |
| Valeur fixe | Demo Bank - Agence Paris Centre | Domiciliation hardcodée |
| Note de bas de document | Document genere le {dd/MM/yyyy} - Demo Bank SA, 1 Place Vendome, 75001 Paris | Note générée dynamiquement |

---

## Contenu PDF — Attestation de solde (type=ATTESTATION)

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Titre du document | Demo Bank — Attestation de Solde | En-tête du PDF |
| Texte introductif | Demo Bank SA, dont le siege social est situe 1 Place Vendome, 75001 Paris, atteste par la presente que : | Paragraphe d'introduction |
| Corps | M. / Mme {fullName} | Identité du titulaire |
| Corps | est titulaire du compte n° {accountNumber} | Référence du compte |
| Corps | Le solde de ce compte, arrete au {dd MMMM yyyy}, est de : | Phrase précédant le montant |
| Montant | {montant} EUR | Solde en gros caractères |
| Clôture | Cette attestation est delivree a la demande de l interesse(e) pour faire valoir ce que de droit. | Formule de clôture |
| Date | Fait a Paris, le {dd MMMM yyyy} | Date de génération |

---

## Contenu PDF — IFU (type=IFU)

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Titre du document | Demo Bank — Imprime Fiscal Unique (IFU) {year} | En-tête du PDF |
| Label info | Etablissement | Ligne de métadonnées |
| Valeur fixe | Demo Bank SA | Nom de l'établissement |
| Label info | SIREN | Ligne de métadonnées |
| Valeur fixe | 123 456 789 00012 | SIRET hardcodé (FIXME JIRA-8021) |
| Label info | Titulaire | Ligne de métadonnées |
| Label info | Adresse | Ligne de métadonnées |
| Valeur fixe | 1 rue de la Paix, 75001 Paris | Adresse hardcodée |
| Label info | Annee fiscale | Ligne de métadonnées |
| En-tête colonne | Nature des revenus | En-tête du tableau fiscal |
| En-tête colonne | Montant brut | En-tête du tableau fiscal |
| Libellé | Interets et produits de placement | Première ligne du tableau |
| Libellé | Prelevement Forfaitaire Unique (30%) | Deuxième ligne du tableau |
| Note de bas de document | Ce document est fourni a titre informatif. Il recense les revenus de capitaux mobiliers soumis au PFU pour l annee {year}. | Note informative |
| Date | Genere le {dd/MM/yyyy} | Date de génération |

---

## Pied de page commun à tous les PDF

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Pied de page | Demo Bank SA — Capital 500 000 000 EUR — RCS Paris 123 456 789 00012 — Établissement de crédit agréé par l'ACPR | Pied de page de tous les PDF |

---

## Valeurs d'énumération — Libellés des mois (DocumentBean / DocumentService)

| Valeur | Libellé | Contexte observé |
| --- | --- | --- |
| 1 | Janvier | Libellé mois — DocumentService.java (moisLibelle) |
| 2 | Février | Libellé mois |
| 3 | Mars | Libellé mois |
| 4 | Avril | Libellé mois |
| 5 | Mai | Libellé mois |
| 6 | Juin | Libellé mois |
| 7 | Juillet | Libellé mois |
| 8 | Août | Libellé mois |
| 9 | Septembre | Libellé mois |
| 10 | Octobre | Libellé mois |
| 11 | Novembre | Libellé mois |
| 12 | Décembre | Libellé mois |
