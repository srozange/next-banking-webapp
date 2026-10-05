Feature: Exécution des virements programmés
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-021-007 — comportement corrigé proposé (BUG-068, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-068 @proposition
  Scenario: RG-021-007 — the target account number does not match any account in the system
    Given the legacy context of rule RG-021-007
    And the target account number does not match any account in the system
    When the action described by rule RG-021-007 is performed
    Then reject the transfer before any debit, and inform the customer with a clear error message.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-021-011 — comportement corrigé proposé (BUG-069, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-069 @proposition
  Scenario: RG-021-011 — computing the next execution date after a successful execution
    Given the legacy context of rule RG-021-011
    When computing the next execution date after a successful execution
    Then advance the next scheduled execution date computed from the originally scheduled date and the transfer frequency.

  @a-confirmer
  # RG-021-013 — comportement corrigé proposé (BUG-070, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-070 @proposition
  Scenario: RG-021-013 — the execution of an individual recurring transfer raises an exception
    Given the legacy context of rule RG-021-013
    When the execution of an individual recurring transfer raises an exception
    Then isolate the failure and continue processing the remaining due transfers in the same batch run.
