Feature: Détection fraude nocturne par batch
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @a-confirmer
  @confirme
  @a-confirmer
  # RG-019-005 exclu — dette technique legacy, hors scope réécriture

  @confirme
  @confirme
  # RG-019-009 — comportement corrigé proposé (BUG-066, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-066 @proposition
  Scenario: RG-019-009 — the nightly fraud-detection batch computes the 24-hour lookback window
    Given the legacy context of rule RG-019-009
    When the nightly fraud-detection batch computes the 24-hour lookback window
    Then define the boundary using a fixed-offset reference to ensure the window is always exactly 24 hours regardless of daylight saving time transitions.
