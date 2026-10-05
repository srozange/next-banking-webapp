Feature: Espace Conseiller — Portefeuille clients
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @a-confirmer
  Scenario: RG-015-002 — a user with role CLIENT requests conseiller-workspace.xhtml
    # Capture golden master : SC-015-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-015-02 : Un CLIENT accédant à conseiller-workspace.xhtml est redirigé silencieusement vers dashboard.xhtml (RBAC)
    And a user with role CLIENT requests conseiller-workspace.xhtml
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/conseiller-workspace.xhtml
    Then silently redirect to dashboard.xhtml without displaying an error message.

  @a-confirmer
  Scenario: RG-015-003 — a user with role CONSEILLER or role ADMIN requests conseiller-workspace.xhtml
    # Capture golden master : SC-015-01
    Given un utilisateur de rôle « CONSEILLER »
    And l'état initial de la capture SC-015-01 : Le conseiller voit son portefeuille avec les KPIs (clients, encours total, prêts en attente)
    And a user with role CONSEILLER or role ADMIN requests conseiller-workspace.xhtml
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/conseiller-workspace.xhtml
    Then display the Espace Conseiller portfolio page.

  # Chargement du portefeuille
  @a-confirmer
  Scenario: RG-015-004 — a CONSEILLER or ADMIN user opens conseiller-workspace.xhtml for the first time …
    # Capture golden master : SC-015-01
    Given un utilisateur de rôle « CONSEILLER »
    And l'état initial de la capture SC-015-01 : Le conseiller voit son portefeuille avec les KPIs (clients, encours total, prêts en attente)
    When a CONSEILLER or ADMIN user opens conseiller-workspace.xhtml for the first time in a session
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/conseiller-workspace.xhtml
    Then load the list of clients whose assigned adviser matches the authenticated user, ordered alphabetically by fullName.

  @a-confirmer
  # Tableau de bord KPI
  @a-confirmer
  Scenario: RG-015-006 — the portfolio is loaded
    # Capture golden master : SC-015-01
    Given un utilisateur de rôle « CONSEILLER »
    And l'état initial de la capture SC-015-01 : Le conseiller voit son portefeuille avec les KPIs (clients, encours total, prêts en attente)
    When the portfolio is loaded
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/conseiller-workspace.xhtml
    Then display the count of clients in the portfolio as KPI "Mes clients".

  @a-confirmer
  Scenario: RG-015-007 — the portfolio is loaded
    # Capture golden master : SC-015-01
    Given un utilisateur de rôle « CONSEILLER »
    And l'état initial de la capture SC-015-01 : Le conseiller voit son portefeuille avec les KPIs (clients, encours total, prêts en attente)
    When the portfolio is loaded
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/conseiller-workspace.xhtml
    Then display the cumulative total of all account balances of all portfolio clients as KPI "Encours total".

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # Rafraîchissement et dette technique
  # RG-015-025 exclu — dette technique legacy, hors scope réécriture

  # RG-015-026 exclu — dette technique legacy, hors scope réécriture
