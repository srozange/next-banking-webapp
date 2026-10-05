Feature: Calcul des intérêts nocturnes
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @a-confirmer
  # RG-018-003 — comportement corrigé proposé (BUG-064, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-064 @proposition
  Scenario: RG-018-003 — the interest calculation batch runs
    Given the legacy context of rule RG-018-003
    When the interest calculation batch runs
    Then apply a monthly interest rate of 0.5% / 12 to each selected EPARGNE account once per month.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
