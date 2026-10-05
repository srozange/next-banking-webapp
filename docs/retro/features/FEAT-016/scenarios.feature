Feature: Administration — Gestion globale
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @confirme
  Scenario: RG-016-002 — a user with role CONSEILLER or CLIENT requests admin.xhtml
    # Capture golden master : SC-016-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-016-02 : Un CLIENT accédant à admin.xhtml est redirigé vers dashboard.xhtml (RBAC - accès refusé sans message d'erreur)
    When a user with role CONSEILLER or CLIENT requests admin.xhtml
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/admin.xhtml
    Then redirect to dashboard.xhtml without displaying the administration page.

  @confirme
  Scenario: RG-016-003 — a user with role ADMIN requests admin.xhtml
    # Capture golden master : SC-016-01
    Given un utilisateur de rôle « ADMIN »
    And l'état initial de la capture SC-016-01 : L'ADMIN accède à la page d'administration avec les KPIs (clients, conseillers, prêts) et la liste des clients
    When a user with role ADMIN requests admin.xhtml
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/admin.xhtml
    Then grant access to all administration sections on the page.

  # 2. Indicateurs KPI de la page d'administration
  @a-confirmer
  Scenario: RG-016-004 — admin.xhtml is loaded
    # Capture golden master : SC-016-01
    Given un utilisateur de rôle « ADMIN »
    And l'état initial de la capture SC-016-01 : L'ADMIN accède à la page d'administration avec les KPIs (clients, conseillers, prêts) et la liste des clients
    When admin.xhtml is loaded
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/admin.xhtml
    Then display the count of all customers whose role is CLIENT as "Total clients".

  @a-confirmer
  Scenario: RG-016-005 — admin.xhtml is loaded
    # Capture golden master : SC-016-01
    Given un utilisateur de rôle « ADMIN »
    And l'état initial de la capture SC-016-01 : L'ADMIN accède à la page d'administration avec les KPIs (clients, conseillers, prêts) et la liste des clients
    When admin.xhtml is loaded
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/admin.xhtml
    Then display the count of active customers whose role is CLIENT as the "actifs" sub-indicator.

  @a-confirmer
  Scenario: RG-016-006 — admin.xhtml is loaded
    # Capture golden master : SC-016-01
    Given un utilisateur de rôle « ADMIN »
    And l'état initial de la capture SC-016-01 : L'ADMIN accède à la page d'administration avec les KPIs (clients, conseillers, prêts) et la liste des clients
    When admin.xhtml is loaded
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/admin.xhtml
    Then display the count of all customers whose role is CONSEILLER.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-016-014 — comportement corrigé proposé (BUG-054, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @a-confirmer
  @BUG-054 @proposition
  Scenario: RG-016-014 — an exception occurs during loan approval or refusal
    Given the legacy context of rule RG-016-014
    And an exception occurs during loan approval or refusal
    When the action described by rule RG-016-014 is performed
    Then display a user-friendly error notification to the admin without exposing internal exception details.

  @confirme
  @confirme
  @a-confirmer
  @confirme
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-016-025 — comportement corrigé proposé (BUG-055, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-055 @proposition
  Scenario: RG-016-025 — a client displayed in the detail panel has an advisor
    Given the legacy context of rule RG-016-025
    When a client displayed in the detail panel has an advisor
    Then display under the 'Client depuis' label the date the customer became a client of the bank, which may differ from the date the advisor was assigned to that customer.

  @confirme
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-016-034 — comportement corrigé proposé (BUG-056, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-056 @proposition
  Scenario: RG-016-034 — the admin views the fraud alert table without a status filter
    Given the legacy context of rule RG-016-034
    When the admin views the fraud alert table without a status filter
    Then display all fraud alerts regardless of status.

  @confirme
