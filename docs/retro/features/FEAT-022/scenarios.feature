Feature: Surveillance soldes faibles et génération d'alertes
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
  # RG-022-010 — comportement corrigé proposé (BUG-071, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-071 @proposition
  Scenario: RG-022-010 — a customer's account balance is below the threshold
    Given the legacy context of rule RG-022-010
    When a customer's account balance is below the threshold
    Then generate at most 20 low-balance notifications per account per day.

  # RG-022-011 — comportement corrigé proposé (BUG-072, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-072 @proposition
  Scenario: RG-022-011 — the periodic low-balance scan completes
    Given the legacy context of rule RG-022-011
    When the periodic low-balance scan completes
    Then record the execution result in the batch job execution log.
