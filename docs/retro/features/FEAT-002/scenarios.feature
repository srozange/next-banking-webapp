Feature: Tableau de bord
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  # RG-002-002 — comportement corrigé proposé (BUG-004, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-004 @proposition
  Scenario: RG-002-002 — the database no longer contains the user whose username is stored in the HTTP …
    Given the legacy context of rule RG-002-002
    And the database no longer contains the user whose username is stored in the HTTP session
    When the action described by rule RG-002-002 is performed
    Then handle the absence gracefully without exposing an error to the user.

  # Soldes et comptes Demo Bank
  @a-confirmer
  Scenario: RG-002-003 — the dashboard loads,
    # Capture golden master : SC-002-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-002-01 : Le tableau de bord CLIENT charge avec les comptes, le solde total et les dernières opérations
    When the dashboard loads,
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/dashboard.xhtml
    Then display the total balance as the arithmetic sum of the balances of all bank accounts belonging to the connected customer.

  @a-confirmer
  @a-confirmer
  Scenario: RG-002-005 — the dashboard loads,
    # Capture golden master : SC-002-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-002-01 : Le tableau de bord CLIENT charge avec les comptes, le solde total et les dernières opérations
    When the dashboard loads,
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/dashboard.xhtml
    Then display, for each account of the connected user, the account type label, its current balance formatted in EUR with 2 decimal places and thousands separator, and its account number.

  # Dernières opérations
  @a-confirmer
  Scenario: RG-002-006 — the dashboard loads and the connected user has at least one account,
    # Capture golden master : SC-002-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-002-01 : Le tableau de bord CLIENT charge avec les comptes, le solde total et les dernières opérations
    When the dashboard loads and the connected user has at least one account,
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/dashboard.xhtml
    Then retrieve and display the 5 most recent transactions of the first account returned by the persistence layer for that user.

  # RG-002-007 — comportement corrigé proposé (BUG-005, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-005 @proposition
  Scenario: RG-002-007 — displaying recent transactions,
    Given the legacy context of rule RG-002-007
    When displaying recent transactions,
    Then draw them from the COURANT account.

  @a-confirmer
  Scenario: RG-002-008 — displaying recent transactions,
    # Capture golden master : SC-002-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-002-01 : Le tableau de bord CLIENT charge avec les comptes, le solde total et les dernières opérations
    When displaying recent transactions,
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/dashboard.xhtml
    Then order them from most recent to oldest (ORDER BY transactionDate DESC).

  @a-confirmer
  Scenario: RG-002-009 — displaying a transaction,
    # Capture golden master : SC-002-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-002-01 : Le tableau de bord CLIENT charge avec les comptes, le solde total et les dernières opérations
    When displaying a transaction,
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/dashboard.xhtml
    Then format its date as dd/MM/yyyy HH:mm.

  @a-confirmer
  Scenario: RG-002-010 — displaying a transaction,
    # Capture golden master : SC-002-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-002-01 : Le tableau de bord CLIENT charge avec les comptes, le solde total et les dernières opérations
    When displaying a transaction,
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/dashboard.xhtml
    Then show the transaction type as either DEBIT or CREDIT.

  # Centre de notifications (ActiveMQ Artemis)
  @a-confirmer
  Scenario: RG-002-011 — the dashboard loads for a logged-in user,
    # Capture golden master : SC-002-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-002-02 : Le centre de notifications affiche les notifications de l'utilisateur avec un badge de décompte
    When the dashboard loads for a logged-in user,
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/dashboard.xhtml
    Then display only the notifications stored for that user's username, in most-recent-first order.

  @a-confirmer
  @a-confirmer
  Scenario: RG-002-013 — the current user has at least one unread notification,
    # Capture golden master : SC-002-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-002-02 : Le centre de notifications affiche les notifications de l'utilisateur avec un badge de décompte
    When the current user has at least one unread notification,
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/dashboard.xhtml
    Then display a badge showing the count of unread notifications.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-002-020 — comportement corrigé proposé (BUG-006, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-006 @proposition
  Scenario: RG-002-020 — persist notification data across server restarts
    Given the legacy context of rule RG-002-020
    When the action described by rule RG-002-020 is performed
    Then persist notification data across server restarts.

  @a-confirmer
  @a-confirmer
  # RG-002-024 — comportement corrigé proposé (BUG-007, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-007 @proposition
  Scenario: RG-002-024 — a transfer is processed,
    Given the legacy context of rule RG-002-024
    When a transfer is processed,
    Then generate exactly one notification per transfer (CONFIRMATION, ALERTE, or FRAUDE — mutually exclusive, never a combination).

  @a-confirmer
  @a-confirmer
