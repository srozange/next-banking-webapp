---
feature: FEAT-017
feature_name: Téléchargement documents PDF
extraction_date: 2026-10-03
sources:
  - src/main/java/com/accenture/bank/web/DocumentDownloadServlet.java
  - src/main/java/com/accenture/bank/ejb/DocumentService.java
  - src/main/webapp/WEB-INF/web.xml
  - src/main/java/com/accenture/bank/web/DocumentBean.java
  - src/main/webapp/documents.xhtml
---

# RG — FEAT-017 Téléchargement documents PDF

_Interface : API — `GET /docs/download`_

---

## Authentification et accès

---

RG-017-001 [Confirmé]
IF a request to `GET /docs/download` is received and the HTTP session does not contain a valid `currentCustomer` attribute
THEN THE SYSTEM SHALL redirect the client to the login page.

RG-017-002 [À confirmer]
THE SYSTEM SHALL protect the document download endpoint through servlet-level session validation, independently of the JAAS container security filter (AppFilter covers `*.xhtml` only).

---

## Validation des paramètres

---

RG-017-003 [À confirmer]
IF the `type` query parameter is absent from the request
THEN THE SYSTEM SHALL reject the request with HTTP 400.

RG-017-004 [À confirmer]
IF the `type` query parameter does not match one of the values RELEVE, RIB, ATTESTATION or IFU (comparison is case-insensitive)
THEN THE SYSTEM SHALL reject the request with HTTP 400.

RG-017-005 [À confirmer]
IF type equals RELEVE, RIB, or ATTESTATION and the `accountId` parameter is absent or non-numeric
THEN THE SYSTEM SHALL reject the request with HTTP 400.

RG-017-006 [Suspect]
IF type equals RELEVE and the `year` or `month` parameter is absent or non-numeric
THEN THE SYSTEM SHALL reject the request with a validation error.

---

## Génération des documents

---

RG-017-007 [À confirmer]
WHEN type=RELEVE
THE SYSTEM SHALL generate a PDF account statement containing all transactions for the given account, year, and month, ordered chronologically by transaction date, and return it as a file attachment named `releve_{accountId}_{year}_{month:02d}.pdf`.

RG-017-008 [À confirmer]
WHEN type=RIB
THE SYSTEM SHALL generate a PDF Relevé d'Identité Bancaire for the given account including the account holder's full name, IBAN, BIC/SWIFT, bank code, branch code, and RIB key, and return it as a file attachment named `rib_{accountId}.pdf`.

RG-017-009 [À confirmer]
WHEN type=ATTESTATION
THE SYSTEM SHALL generate a PDF balance attestation for the given account showing the current balance as of the generation date, and return it as a file attachment named `attestation_solde_{accountId}.pdf`.

RG-017-010 [À confirmer]
WHEN type=IFU
THE SYSTEM SHALL generate a PDF Imprimé Fiscal Unique for the authenticated session customer and the given year, computing the total of CREDIT transactions whose description matches `%interet%` (LIKE pattern), applying a Prélèvement Forfaitaire Unique rate of 30% rounded HALF_UP to 2 decimal places, and return it as a file attachment named `ifu_{customerId}_{year}.pdf`.
  Note    : le customerId est extrait de la session (non falsifiable par le client) ;
            le paramètre `year` provient de la requête sans validation de plage

---

## Contrôle d'accès aux données

---

RG-017-011 [Suspect]
IF an authenticated session customer requests a document for an `accountId` that does not belong to them
THEN THE SYSTEM SHALL reject the request.

---

## Gestion des erreurs

---

RG-017-012 [À confirmer]
IF the referenced account or customer does not exist in the database
THEN THE SYSTEM SHALL reject the request with HTTP 404.

RG-017-013 [Suspect]
IF an unexpected error occurs during PDF generation
THEN THE SYSTEM SHALL inform the client that document generation failed, without exposing internal error details.

---

## Format de la réponse

---

RG-017-014 [À confirmer]
WHEN generating any PDF document
THE SYSTEM SHALL set Content-Type to `application/pdf`, Content-Disposition to `attachment; filename="{filename}"`, and Cache-Control to `no-cache, no-store, must-revalidate`.

---

## Configuration

---

RG-017-015 [Suspect]
WHEN generating the IFU document, THE SYSTEM SHALL use the bank's registered SIRET number sourced from application configuration.
