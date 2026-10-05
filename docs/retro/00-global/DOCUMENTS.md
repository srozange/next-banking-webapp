---
step: retro-documents
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
generated_at: 2026-10-03
---

# DOCUMENTS — Application bancaire legacy

## Vue d'ensemble

L'application produit exclusivement des **documents PDF** (librairie OpenPDF / iText fork — `com.lowagie.text`).  
**Aucun système d'envoi d'e-mail n'est détecté** : pas de `JavaMailSender`, `MimeMessage`, `nodemailer` ni équivalent. Les notifications aux clients passent par un store in-memory (`NotificationStore`) — elles n'ont pas de trace écrite.

Cinq types de documents sont identifiés, correspondant à l'enum `DocumentType` et à la servlet `DocumentDownloadServlet` (`/docs/download`).

> **Anomalie résolue** : le bug `LazyInitializationException` dans `DocumentService` (`@TransactionAttribute(SUPPORTS)` → `REQUIRED` sur les 4 méthodes de génération) a été corrigé. Les exemplaires réels ont été téléchargés depuis l'instance live le 03/10/2026. DOC-003 reste inaccessible (code orphelin — aucun appelant).

---

## DOC-001 — Relevé mensuel de compte

**Type** : PDF  
**Feature associée** : FEAT-008 (Documents : relevés, attestations, IFU) + JOB-001 (batch nocturne)  
**Déclencheur** :
- (a) Batch automatique JOB-001 à 02h00 (`NightlyBatchService`) → `DocumentService.generateReleveDocument(customerId, month, year)` — persiste une entrée `Document` en base (type `RELEVE_MENSUEL`)
- (b) À la demande du client : `GET /docs/download?type=RELEVE&accountId={accountId}&year={année}&month={mois}` → `DocumentService.genererReleve(accountId, year, month)` — rendu direct, **sans** persistance

**Destinataires** : client titulaire des comptes  
**Conditions d'envoi** : session CLIENT active ; paramètres `accountId`, `year`, `month` fournis  
**Exemplaire** : `retro/00-global/documents/DOC-001.pdf` (téléchargé le 03/10/2026, 1 page, accountId=1 — Jean Dupont, FR76-0001-0001, octobre 2026, solde 2 130,00 EUR, 2 opérations)  
**Nom de fichier** :
- mode batch : `releve_{username}_{year}_{MM}.pdf`
- mode à la demande : `releve_{accountId}_{year}_{MM}.pdf`

### Contenu

En-tête : `Demo Bank — RELEVÉ DE COMPTE — {moisLibellé} {année}` (fond sombre, logo "DB" rouge)

Lignes d'information :
```
Client      {fullName}
Identifiant {username}
Période     {moisLibellé} {année}
Solde actuel {solde} EUR          ← uniquement mode à la demande
```

Pour chaque compte du client, tableau des 50 opérations les plus récentes :

| Date | Description | Type | Montant | Solde après |
|---|---|---|---|---|
| {dd/MM/yyyy} | {description} | CREDIT / DEBIT | +{montant} EUR / -{montant} EUR | {solde} EUR |

Si aucune opération : `Aucune opération sur cette période.` (gris)

Pied de page : `Demo Bank SA — Capital 500 000 000 EUR — RCS Paris {SIRET} — Établissement de crédit agréé par l'ACPR`

### Contraintes de mise en page
- Format : A4 portrait (595 × 842 pts), marges 50/50/60/60 (gauche/droite/haut/bas)
- En-tête : tableau 2 colonnes (logo 1/5, titre 4/5) — fond `#1A1A1A`, rouge `#E3000F`
- Tableau opérations : 5 colonnes, proportions `15/38/12/17/18`, police Helvetica 9pt
- Montants crédit : vert `#2E7D32` ; montants débit : rouge `#E3000F`
- Séparateur : ligne rouge 1pt sur 100% de la largeur

### Dettes legacy
- Mode batch et mode à la demande sont deux implémentations distinctes non factorisées (JIRA-8031, JIRA-8032)
- Checksum MD5 (faible — JIRA-7712)
- Exemplaire réel disponible : `retro/00-global/documents/DOC-001.pdf`

---

## DOC-002 — Attestation de solde

**Type** : PDF  
**Feature associée** : FEAT-008  
**Déclencheur** :
- (a) Action utilisateur dans `documents.xhtml` → `DocumentBean.generateAttestation()` → `DocumentService.generateAttestationDocument(customerId)` — persiste en base (type `ATTESTATION_SOLDE`), prend le solde **cumulé** de tous les comptes
- (b) À la demande : `GET /docs/download?type=ATTESTATION&accountId={accountId}` → `DocumentService.genererAttestationSolde(accountId)` — rendu direct, **sans** persistance, prend le solde du seul compte ciblé

**Destinataires** : client titulaire  
**Conditions d'envoi** : session CLIENT active ; `accountId` fourni (mode b)  
**Exemplaire** : `retro/00-global/documents/DOC-002.pdf` (téléchargé le 03/10/2026, 1 page, accountId=1 — Jean Dupont, FR76-0001-0001, solde 2 130,00 EUR au 03/10/2026)  
**Nom de fichier** :
- mode (a) : `attestation_{username}_{yyyyMMdd}.pdf`
- mode (b) : `attestation_solde_{accountId}.pdf`

### Contenu (mode b — mode à la demande)

En-tête : `Demo Bank — Attestation de Solde`

Corps :

```
Demo Bank SA, dont le siège social est situé 1 Place Vendôme, 75001 Paris,
atteste par la présente que :

M. / Mme {fullName}
est titulaire du compte n° {accountNumber}

Le solde de ce compte, arrêté au {dd MMMM yyyy}, est de :

                {solde} EUR

Cette attestation est délivrée à la demande de l'intéressé(e) pour faire valoir
ce que de droit.

Fait à Paris, le {dd MMMM yyyy}
```

### Contraintes de mise en page
- Format : A4 portrait, marges 80/80/80/80 (document formel)
- Montant : centré, Helvetica Bold 18pt rouge `#E3000F`
- Corps : Helvetica 11pt

### Dettes legacy
- Incohérence entre les deux modes : `generateAttestationDocument` somme tous les comptes, `genererAttestationSolde` ne prend qu'un seul compte — résultats différents pour un client multi-comptes
- MD5 pour le checksum (mode a, JIRA-7712)
- Exemplaire réel disponible : `retro/00-global/documents/DOC-002.pdf`

---

## DOC-003 — Tableau d'amortissement

**Type** : PDF  
**Feature associée** : FEAT-007 (Prêts)  
**Déclencheur** : `DocumentService.generateTableauDocument(loanId, customerId)` — méthode définie mais **aucun appelant identifié dans le code actuel** (code orphelin — probable régression lors d'un refactoring). Le type `TABLEAU_AMORTISSEMENT` existe dans `DocumentType.java` mais aucune route ni action JSF ne l'invoque.
**Destinataires** : client titulaire du prêt  
**Exemplaire** : `[NON DISPONIBLE]` — code orphelin, aucun appelant dans le code actuel ; impossible à déclencher depuis l'interface ou la servlet. Aucun exemplaire réel.  
**Nom de fichier** : `tableau_amort_pret{loanId}_{username}.pdf`

### Contenu

En-tête : `Demo Bank — TABLEAU D'AMORTISSEMENT`

Lignes d'information :
```
Client      {fullName}
Montant     {montant} EUR
Taux annuel {taux:.4f}%
Durée       {dureeMois} mois
Mensualité  {mensualité} EUR
```

Tableau des échéances ({dureeMois} lignes) :

| Éch. | Date | Capital | Intérêts | Total | Cap. restant |
|---|---|---|---|---|---|
| {i} | {MM/yyyy} | {capital} € | {intérêts} € | {mensualité} € | {capitalRestant} € |

### Contraintes de mise en page
- Format : A4 paysage (842 × 595 pts), marges 30/30/50/50
- Tableau : 6 colonnes, proportions `8/16/14/14/14/14`, Helvetica 9pt
- Lignes alternées : blanc / gris clair `#F4F4F4`
- Checksum : **SHA-256** (différent des autres documents qui utilisent MD5 — incohérence JIRA-8002)

### Dettes legacy
- Méthode jamais appelée — document non accessible depuis l'interface actuelle
- Duplication de la logique de calcul de `LoanService.simuler()` (JIRA-8050)
- Aucun exemplaire réel — code orphelin, jamais exécuté

---

## DOC-004 — RIB (Relevé d'Identité Bancaire)

**Type** : PDF  
**Feature associée** : FEAT-008  
**Déclencheur** : `GET /docs/download?type=RIB&accountId={accountId}` → `DocumentService.genererRIB(accountId)` — rendu direct, **sans** persistance  
**Destinataires** : client titulaire  
**Conditions d'envoi** : session CLIENT active ; `accountId` fourni  
**Exemplaire** : `retro/00-global/documents/DOC-004.pdf` (téléchargé le 03/10/2026, 2 pages, accountId=1 — Jean Dupont, IBAN FR76-0001-0001, BIC DEMOFR21XXX)  
**Nom de fichier** : `rib_{accountId}.pdf`

### Contenu

En-tête : `Demo Bank — Relevé d'Identité Bancaire (RIB)`

```
Titulaire       {fullName}
Banque          Demo Bank SA
Code banque     30004
Code guichet    00001
Numéro de compte {accountNumber sans préfixe FR76- ni tirets}
Clé RIB         42
IBAN            {accountNumber}
BIC / SWIFT     DEMOFR21XXX
Domiciliation   Demo Bank - Agence Paris Centre
```

Note de bas de document : `Document généré le {dd/MM/yyyy} - Demo Bank SA, 1 Place Vendôme, 75001 Paris`

### Contraintes de mise en page
- Format : rectangle personnalisé 595 × 280 pts (portrait demi-page, format RIB standard), marges 50/50/40/40
- Pas de tableau d'opérations — lecture seule

### Dettes legacy
- Code banque (`30004`), code guichet (`00001`), clé RIB (`42`), BIC (`DEMOFR21XXX`), domiciliation hardcodés — non configurables (JIRA-8020, JIRA-8021)
- Exemplaire réel disponible : `retro/00-global/documents/DOC-004.pdf`

---

## DOC-005 — IFU (Imprimé Fiscal Unique)

**Type** : PDF  
**Feature associée** : FEAT-008  
**Déclencheur** : `GET /docs/download?type=IFU&year={année}` → `DocumentService.genererIFU(customerId, year)` — rendu direct, **sans** persistance ; `customerId` provient de la session courante  
**Destinataires** : client titulaire  
**Conditions d'envoi** : session CLIENT active ; `year` fourni (défaut 0 si absent — comportement non filtrant)  
**Exemplaire** : `retro/00-global/documents/DOC-005.pdf` (téléchargé le 03/10/2026, 1 page, année fiscale 2025 — Jean Dupont, intérêts 0,00 EUR / PFU 0,00 EUR — aucun revenu de placement en 2025)  
**Nom de fichier** : `ifu_{customerId}_{year}.pdf`

### Contenu

En-tête : `Demo Bank — Imprimé Fiscal Unique (IFU) {année}`

```
Établissement   Demo Bank SA
SIREN           123 456 789 00012
Titulaire       {fullName}
Adresse         1 rue de la Paix, 75001 Paris
Année fiscale   {année}
```

Tableau fiscal :

| Nature des revenus | Montant brut |
|---|---|
| Intérêts et produits de placement | {totalIntérêts} EUR |
| Prélèvement Forfaitaire Unique (30%) | {pfu} EUR |

Note de bas : `Ce document est fourni à titre informatif. Il recense les revenus de capitaux mobiliers soumis au PFU pour l'année {année}. Généré le {dd/MM/yyyy}`

### Contraintes de mise en page
- Format : A4 portrait, marges 60/60/60/60
- Tableau fiscal : 2 colonnes, largeur 80% de la page
- PFU calculé : `totalIntérêts × 0,30` arrondi à 2 décimales (méthode HALF_UP)

### Dettes legacy
- Adresse client hardcodée `1 rue de la Paix, 75001 Paris` — non extraite de l'entité Customer (JIRA-8020)
- Calcul des intérêts : requête JPQL filtrée sur `description LIKE '%interet%'` (insensible à la casse non garantie en JPQL standard) et `type = 'CREDIT'` (comparaison chaîne sur enum)
- Exemplaire réel disponible : `retro/00-global/documents/DOC-005.pdf` (intérêts 0,00 EUR — aucun revenu de placement en 2025 pour user1)

---

## Synthèse

| Doc | Type | Feature | Persisté en base | Déclencheur |
|---|---|---|---|---|
| DOC-001 Relevé mensuel | PDF | FEAT-008 | Oui (mode batch) / Non (mode à la demande) | JOB-001 02:00 + GET servlet |
| DOC-002 Attestation de solde | PDF | FEAT-008 | Oui (action JSF) / Non (GET servlet) | Action DocumentBean + GET servlet |
| DOC-003 Tableau d'amortissement | PDF | FEAT-007 | Oui (si appelé) | **Aucun — code orphelin** |
| DOC-004 RIB | PDF | FEAT-008 | Non | GET servlet |
| DOC-005 IFU | PDF | FEAT-008 | Non | GET servlet |

**Emails** : aucun.  
**Librairie PDF** : OpenPDF (`com.lowagie.text`) — fork open-source d'iText 2.1.7.  
**Checksum** : MD5 pour DOC-001 et DOC-002 (batch), SHA-256 pour DOC-003 — incohérence à corriger dans la réécriture (utiliser SHA-256 partout).
