---
feature: FEAT-008
title: "Documents (relevés, attestations, IFU)"
status: current
generated_by: retro-rules
---

# RG — FEAT-008 « Documents (relevés, attestations, IFU) »

---

## Contrôle d'accès

RG-008-001 [Confirmé]
WHEN an unauthenticated user requests `documents.xhtml`
THE SYSTEM SHALL redirect to the login page.

RG-008-002 [Confirmé]
WHEN a download request arrives at `/docs/download` without an authenticated session (no `currentCustomer` attribute in the HTTP session)
THE SYSTEM SHALL redirect to the login page.

RG-008-003 [Confirmé]
THE SYSTEM SHALL allow any authenticated user regardless of role (CLIENT, CONSEILLER, ADMIN) to access the documents page and download their own documents.

RG-008-004 [Suspect]
IF a logged-in user requests a RELEVE, RIB, or ATTESTATION document with an `accountId` that does not belong to them
THEN THE SYSTEM SHALL reject the request.

---

## Affichage de la page

RG-008-005 [Confirmé]
WHEN an authenticated user views the documents page
THE SYSTEM SHALL display, for each of their accounts, one RIB download link, one attestation de solde download link, and one download link per month for the 12 most recent months.

RG-008-006 [Confirmé]
WHEN an authenticated user views the documents page
THE SYSTEM SHALL display IFU download links for fiscal years N-1 and N-2 (N being the current calendar year).

---

## Génération des documents — téléchargement direct (servlet)

RG-008-007 [Confirmé]
WHEN a RELEVE is requested for a given accountId, year, and month
THE SYSTEM SHALL generate and serve a PDF containing all transactions for that account in the specified period, ordered by transaction date ascending.

RG-008-008 [Confirmé]
WHEN a RIB is requested for a given accountId
THE SYSTEM SHALL generate and serve a PDF containing the account number, IBAN, BIC/SWIFT, bank code, branch code, and account holder name.

RG-008-009 [Confirmé]
WHEN an ATTESTATION is requested for a given accountId
THE SYSTEM SHALL generate and serve a PDF certifying the current balance of that single account, dated at the time of the request.

RG-008-010 [Confirmé]
WHEN an IFU is requested for a given year
THE SYSTEM SHALL generate the IFU for the authenticated user's customerId sourced from the session, not from request parameters.

RG-008-011 [Confirmé]
WHEN an IFU is generated for a given year
THE SYSTEM SHALL compute the total of credit transactions whose description contains "interet" (LIKE '%interet%') as the declared interest income, and apply a 30% Prélèvement Forfaitaire Unique (PFU) rate to that amount.

---

## Gestion des erreurs

RG-008-012 [Confirmé]
IF the `type` request parameter is missing from the download request
THEN THE SYSTEM SHALL return HTTP 400.

RG-008-013 [Confirmé]
IF a required parameter (`accountId`) is missing for document types that require it (RELEVE, RIB, ATTESTATION)
THEN THE SYSTEM SHALL return HTTP 400.

RG-008-014 [Confirmé]
IF the `type` request parameter does not match a known document type (RELEVE, RIB, ATTESTATION, IFU)
THEN THE SYSTEM SHALL return HTTP 400.

RG-008-015 [Confirmé]
IF the requested account does not exist
THEN THE SYSTEM SHALL return HTTP 404.

---

## Validation des paramètres

RG-008-016 [Suspect]
IF a RELEVE is requested with an invalid or out-of-range year or month parameter
THEN THE SYSTEM SHALL reject the request.

---

## Configuration banque

RG-008-017 [Suspect]
WHEN any document is generated that includes the bank's SIRET
THE SYSTEM SHALL include the bank's SIRET number sourced from an external configuration.

---

## Comptage des téléchargements

RG-008-018 [À confirmer]
WHEN a document is downloaded
THE SYSTEM SHALL increment the document's download counter.
  (intention-code-mort) La méthode `incrementDownloadCount()` et le champ `downloadCount` sont déclarés dans DocumentService et Document.java mais jamais appelés depuis DocumentDownloadServlet ni depuis DocumentBean : le compteur reste toujours à 0. La fonctionnalité de comptage des téléchargements doit-elle être implémentée dans la réécriture ?
    Portée : information
