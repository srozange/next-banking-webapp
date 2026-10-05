Feature: Documents (relevés, attestations, IFU)
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @confirme
  @confirme
  Scenario: RG-008-003 — allow any authenticated user regardless of role (CLIENT, CONSEILLER, ADMIN) to …
    # Capture golden master : SC-008-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-008-01 : La page documents affiche les liens de téléchargement IFU, RIB, attestation et relevés mensuels
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/documents.xhtml
    Then allow any authenticated user regardless of role (CLIENT, CONSEILLER, ADMIN) to access the documents page and download their own documents.

  # RG-008-004 — comportement corrigé proposé (BUG-027, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-027 @proposition
  Scenario: RG-008-004 — a logged-in user requests a RELEVE, RIB, or ATTESTATION document with an …
    Given the legacy context of rule RG-008-004
    And a logged-in user requests a RELEVE, RIB, or ATTESTATION document with an `accountId` that does not belong to them
    When the action described by rule RG-008-004 is performed
    Then reject the request.

  # Affichage de la page
  @confirme
  Scenario: RG-008-005 — an authenticated user views the documents page
    # Capture golden master : SC-008-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-008-01 : La page documents affiche les liens de téléchargement IFU, RIB, attestation et relevés mensuels
    When an authenticated user views the documents page
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/documents.xhtml
    Then display, for each of their accounts, one RIB download link, one attestation de solde download link, and one download link per month for the 12 most recent months.

  @confirme
  Scenario: RG-008-006 — an authenticated user views the documents page
    # Capture golden master : SC-008-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-008-01 : La page documents affiche les liens de téléchargement IFU, RIB, attestation et relevés mensuels
    When an authenticated user views the documents page
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/documents.xhtml
    Then display IFU download links for fiscal years N-1 and N-2 (N being the current calendar year).

  @confirme
  @confirme
  @confirme
  @confirme
  @confirme
  @confirme
  @confirme
  # Validation des paramètres
  # RG-008-016 exclu — dette technique legacy, hors scope réécriture

  # Configuration banque
  # RG-008-017 exclu — dette technique legacy, hors scope réécriture

