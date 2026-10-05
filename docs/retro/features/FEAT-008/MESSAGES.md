---
feature: FEAT-008
name: "Documents (relevés, attestations, IFU)"
status: current
generated_by: retro-rules
date: 2026-10-03
---

# Messages et libellés — FEAT-008 Documents (relevés, attestations, IFU)

Sources : `src/main/webapp/documents.xhtml` (libellés statiques),
`src/main/java/com/accenture/bank/web/DocumentBean.java` (messages FacesMessage — méthodes non liées à un formulaire),
`src/main/java/com/accenture/bank/web/DocumentDownloadServlet.java` (messages d'erreur HTTP).

> Note d'encodage : le fichier `documents.xhtml` omet les accents sur plusieurs mots
> (« Deconnexion », « Telecharger PDF », « Releve d Identite Bancaire », « Imprime Fiscal Unique »,
> « Releves mensuels ») — les textes ci-dessous reproduisent l'orthographe exacte de la source.

---

## En-tête et navigation

| Identifiant | Texte exact (source) | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-008-001 | `BankApp Demo - Documents` | Titre du navigateur — toujours | |
| MSG-008-002 | `Demo Bank` | Marque — toujours | |
| MSG-008-003 | `Bonjour, {documentBean.currentCustomer.fullName}` | Toujours (utilisateur connecté) | |
| MSG-008-004 | `Deconnexion` | Toujours | |

### Liens de navigation (toujours visibles)

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-008-010 | `Tableau de bord` | |
| MSG-008-011 | `Virement` | |
| MSG-008-012 | `Virements programmes` | |
| MSG-008-013 | `Releve` | |
| MSG-008-014 | `Budget` | |
| MSG-008-015 | `Prets` | |
| MSG-008-016 | `Documents` | |
| MSG-008-017 | `Bourse` | |
| MSG-008-018 | `Mon Conseiller` | |
| MSG-008-019 | `Produits` | |
| MSG-008-020 | `Comptes Tiers` | |
| MSG-008-021 | `Beneficiaires` | |

### Liens de navigation conditionnels

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-008-022 | `Espace Conseiller` | `securityBean.adminOrConseiller` = true (rôles CONSEILLER ou ADMIN) | |
| MSG-008-023 | `Administration` | `securityBean.admin` = true (rôle ADMIN uniquement) | |

---

## Bandeau technique

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-008-030 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 — Generation PDF via OpenPDF (LGPL) + Servlet de telechargement` | Toujours | |
| MSG-008-031 | `DocumentDownloadServlet : /banking-legacy/docs/download?type=RELEVE\|RIB\|ATTESTATION\|IFU` | Toujours (infobulle technique) | |

---

## Section IFU (documents fiscaux)

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-008-040 | `Documents fiscaux` | Séparateur de section — toujours | |
| MSG-008-041 | `Imprime Fiscal Unique (IFU)` | Titre de section — toujours | |
| MSG-008-042 | `Fiscal` | Étiquette de type — chaque carte IFU | |
| MSG-008-043 | `IFU {currentYear - 1}` | Titre de carte — année N-1 | |
| MSG-008-044 | `IFU {currentYear - 2}` | Titre de carte — année N-2 | |
| MSG-008-045 | `Revenus de capitaux mobiliers` | Sous-titre de carte — chaque carte IFU | |
| MSG-008-046 | `Telecharger PDF` | Lien de téléchargement — chaque carte IFU | |

---

## Section documents par compte (RIB, Attestation, Relevés mensuels)

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-008-050 | `Compte {acc.accountType} - {acc.accountNumber}` | Séparateur de section — un par compte du client | |
| MSG-008-051 | `Bancaire` | Étiquette de type — carte RIB | |
| MSG-008-052 | `RIB` | Titre de carte — carte RIB | |
| MSG-008-053 | `Releve d Identite Bancaire` | Sous-titre de carte — carte RIB | |
| MSG-008-054 | `Telecharger PDF` | Lien de téléchargement — carte RIB | |
| MSG-008-055 | `Attestation` | Étiquette de type — carte attestation | |
| MSG-008-056 | `Attestation de solde` | Titre de carte — carte attestation | |
| MSG-008-057 | `Solde au jour de la demande` | Sous-titre de carte — carte attestation | |
| MSG-008-058 | `Telecharger PDF` | Lien de téléchargement — carte attestation | |
| MSG-008-059 | `Releves mensuels — {acc.accountNumber}` | En-tête de liste de relevés — par compte | |
| MSG-008-060 | `{moisLibelle} {year}` | Lien mensuel — un par mois sur les 12 derniers mois | |

> Les libellés de mois (`moisLibelle`) sont produits par `DocumentBean.getMoisLibelle()` :
> `Janvier`, `Février`, `Mars`, `Avril`, `Mai`, `Juin`,
> `Juillet`, `Août`, `Septembre`, `Octobre`, `Novembre`, `Décembre` — 144.

---

## Messages d'erreur HTTP (DocumentDownloadServlet)

Les messages ci-dessous sont renvoyés dans la réponse HTTP par `DocumentDownloadServlet`.

| Identifiant | Code HTTP | Texte exact | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-008-070 | 400 | `Parametre type manquant` | Paramètre `type` absent de la requête | |
| MSG-008-071 | 400 | `accountId manquant` | Paramètre `accountId` absent (RELEVE, RIB ou ATTESTATION) | |
| MSG-008-072 | 400 | `Type inconnu : {type}` | Paramètre `type` ne correspond à aucun type connu (RELEVE, RIB, ATTESTATION, IFU) | |
| MSG-008-073 | 404 | _(message de l'`IllegalArgumentException` propagée depuis DocumentService)_ | Compte introuvable (accountId inexistant) | |
| MSG-008-074 | 500 | `Erreur lors de la generation du document` | Exception inattendue lors de la génération PDF | |

> Règles associées : RG-008-012 (400 type manquant), RG-008-013 (400 accountId manquant),
> RG-008-014 (400 type inconnu), RG-008-015 (404 compte introuvable).

---

## Messages FacesMessage (DocumentBean — méthodes sans formulaire lié)

Les messages ci-dessous sont déclarés dans `DocumentBean.generateReleve()` et
`DocumentBean.generateAttestation()`. Ces méthodes ne sont liées à aucun formulaire
dans `documents.xhtml` (page entièrement en liens `<a href>`, aucun `<h:commandButton>`) :
ces messages ne sont pas affichés dans le flux normal décrit par FLOW.md.

| Identifiant | Sévérité | Texte exact (gabarit) | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-008-080 | ERROR | `Session expirée.` | `currentCustomer` null à l'entrée de generateReleve() ou generateAttestation() | |
| MSG-008-081 | INFO | `Relevé {moisLibelle} {year} généré avec succès.` | Génération du relevé persisté réussie | |
| MSG-008-082 | ERROR | `Impossible de générer le relevé.` | `generateReleveDocument()` retourne null | |
| MSG-008-083 | ERROR | `Erreur lors de la génération du relevé : {message}` | Exception lors de la génération du relevé | |
| MSG-008-084 | INFO | `Attestation de solde générée avec succès.` | Génération de l'attestation persistée réussie | |
| MSG-008-085 | ERROR | `Impossible de générer l'attestation.` | `generateAttestationDocument()` retourne null | |
| MSG-008-086 | ERROR | `Erreur lors de la génération : {message}` | Exception lors de la génération de l'attestation | |
