Feature: Traitement asynchrone approbation prêts (JMS)
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

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
  # RG-024-020 — comportement corrigé proposé (BUG-077, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-077 @proposition
  Scenario: RG-024-020 — publishing a loan approval request THEN
    Given the legacy context of rule RG-024-020
    When publishing a loan approval request THEN
    Then reject the request and inform the customer instead of leaving it pending forever.

  # RG-024-021 — comportement corrigé proposé (BUG-078, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-078 @proposition
  Scenario: RG-024-021 — the loan type is not recognized by the credit scorer
    Given the legacy context of rule RG-024-021
    And the loan type is not recognized by the credit scorer
    When the action described by rule RG-024-021 is performed
    Then refuse the loan request.
