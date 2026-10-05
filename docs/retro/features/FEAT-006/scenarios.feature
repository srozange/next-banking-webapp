Feature: Budget et catégories de dépenses
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  # Périmètre et données
  @a-confirmer
  Scenario: RG-006-001 — scope all budget data (plafonds, spending breakdown, totals) to the …
    # Capture golden master : SC-006-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-006-01 : La page budget charge avec la répartition des dépenses par catégorie pour la période courante
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/budget.xhtml
    Then scope all budget data (plafonds, spending breakdown, totals) to the authenticated user's own customer account only.

  @a-confirmer
  Scenario: RG-006-003 — the budget page initializes AND the authenticated user has transaction history,
    # Capture golden master : SC-006-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-006-01 : La page budget charge avec la répartition des dépenses par catégorie pour la période courante
    When the budget page initializes AND the authenticated user has transaction history,
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/budget.xhtml
    Then set the selected period to the most recent month with transactions.

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
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # Suspect
  # RG-006-028 — comportement corrigé proposé (BUG-023, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-023 @proposition
  Scenario: RG-006-028 — the user defines a plafond for the "Revenus" budget category,
    Given the legacy context of rule RG-006-028
    When the user defines a plafond for the "Revenus" budget category,
    Then exclude income categories such as 'Revenus' from the budget ceiling selection, as income categories cannot meaningfully be capped as spending.
