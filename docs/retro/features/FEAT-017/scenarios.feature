Feature: Téléchargement documents PDF
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  # Authentification et accès
  @confirme
  Scenario: RG-017-001 — a request to GET /docs/download is received and the HTTP session does not …
    # Capture golden master : SC-017-01
    Given un utilisateur sans rôle déclaré dans la capture
    And l'état initial de la capture SC-017-01 : Un accès sans session au endpoint /docs/download est redirigé vers la page de connexion (HTTP 302)
    And a request to `GET /docs/download` is received and the HTTP session does not contain a valid `currentCustomer` attribute
    When la requête GET /banking-legacy/docs/download?type=RIB&accountId=1 est envoyée
    Then redirect the client to the login page.

  @a-confirmer
  Scenario: RG-017-002 — protect the document download endpoint through servlet-level session validation …
    # Capture golden master : SC-017-01
    Given un utilisateur sans rôle déclaré dans la capture
    And l'état initial de la capture SC-017-01 : Un accès sans session au endpoint /docs/download est redirigé vers la page de connexion (HTTP 302)
    When la requête GET /banking-legacy/docs/download?type=RIB&accountId=1 est envoyée
    Then protect the document download endpoint through servlet-level session validation, independently of the JAAS container security filter (AppFilter covers `*.xhtml` only).

  @a-confirmer
  @a-confirmer
  # RG-017-006 exclu — dette technique legacy, hors scope réécriture

  @a-confirmer
  Scenario: RG-017-008 — type=RIB
    # Capture golden master : SC-017-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-017-02 : Un utilisateur authentifié obtient un PDF (Content-Type: application/pdf) pour le type RIB d'un compte valide
    When type=RIB
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/documents.xhtml
    And il attend le texte « Telecharger PDF »
    Then generate a PDF Relevé d'Identité Bancaire for the given account including the account holder's full name, IBAN, BIC/SWIFT, bank code, branch code, and RIB key, and return it as a file attachment named

  @a-confirmer
  @a-confirmer
  # Contrôle d'accès aux données
  # RG-017-011 — comportement corrigé proposé (BUG-058, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-058 @proposition
  Scenario: RG-017-011 — an authenticated session customer requests a document for an accountId that does …
    Given the legacy context of rule RG-017-011
    And an authenticated session customer requests a document for an `accountId` that does not belong to them
    When the action described by rule RG-017-011 is performed
    Then reject the request.

  # RG-017-013 — comportement corrigé proposé (BUG-059, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-059 @proposition
  Scenario: RG-017-013 — an unexpected error occurs during PDF generation
    Given the legacy context of rule RG-017-013
    And an unexpected error occurs during PDF generation
    When the action described by rule RG-017-013 is performed
    Then inform the client that document generation failed, without exposing internal error details.

  # Format de la réponse
  @a-confirmer
  Scenario: RG-017-014 — generating any PDF document
    # Capture golden master : SC-017-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-017-02 : Un utilisateur authentifié obtient un PDF (Content-Type: application/pdf) pour le type RIB d'un compte valide
    When generating any PDF document
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/documents.xhtml
    And il attend le texte « Telecharger PDF »
    Then set Content-Type to `application/pdf`, Content-Disposition to `attachment; filename="{filename}"`, and Cache-Control to `no-cache, no-store, must-revalidate`.

  # Configuration
  # RG-017-015 exclu — dette technique legacy, hors scope réécriture
