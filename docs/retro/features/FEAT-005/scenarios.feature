Feature: Relevé de compte
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @confirme
  @confirme
  Scenario: RG-005-003 — releve.xhtml is initialised and the customer has at least one account
    # Capture golden master : SC-005-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-005-01 : Le relevé de compte charge avec les transactions du premier compte, ordonnées par date décroissante
    When releve.xhtml is initialised and the customer has at least one account
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/releve.xhtml
    Then auto-select the first account in the list and load its transactions.

  @confirme
  @confirme
  # Panneau d'informations du compte
  @confirme
  Scenario: RG-005-007 — a selected account exists
    # Capture golden master : SC-005-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-005-01 : Le relevé de compte charge avec les transactions du premier compte, ordonnées par date décroissante
    And a selected account exists
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/releve.xhtml
    Then display the account type, account number, and current balance in the accountInfo panel.

  @confirme
  @a-confirmer
  @confirme
  Scenario: RG-005-012 — the transaction list is non-empty
    # Capture golden master : SC-005-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-005-01 : Le relevé de compte charge avec les transactions du premier compte, ordonnées par date décroissante
    When the transaction list is non-empty
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/releve.xhtml
    Then display a counter "N opération(s)" above the transaction table, where N is the total number of transactions loaded.

  @confirme
  Scenario: RG-005-013 — a transaction date is displayed
    # Capture golden master : SC-005-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-005-01 : Le relevé de compte charge avec les transactions du premier compte, ordonnées par date décroissante
    When a transaction date is displayed
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/releve.xhtml
    Then format it as dd/MM/yyyy HH:mm.

  @confirme
  Scenario: RG-005-014 — a transaction amount is displayed
    # Capture golden master : SC-005-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-005-01 : Le relevé de compte charge avec les transactions du premier compte, ordonnées par date décroissante
    When a transaction amount is displayed
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/releve.xhtml
    Then prefix it with "+" for CREDIT transactions and "−" for DEBIT transactions, and format it in EUR with at least 2 decimal places.

  @confirme
  @confirme
  @confirme
