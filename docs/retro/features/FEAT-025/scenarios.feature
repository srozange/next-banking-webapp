Feature: Système de notifications internes
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @confirme
  @confirme
  @confirme
  @confirme
  @confirme
  @confirme
  @confirme
  @confirme
  @confirme
  # RG-025-011 — comportement corrigé proposé (BUG-079, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-079 @proposition
  Scenario: RG-025-011 — the periodic low-balance scan runs (every 30 minutes, @Schedule …
    Given the legacy context of rule RG-025-011
    When the periodic low-balance scan runs (every 30 minutes, `@Schedule(hour="*", minute="*/30")`) AND an active customer's account balance is strictly less than 100 EUR (hardcoded)
    Then display a low-balance notification to the customer in their account dashboard.

  # RG-025-012 exclu — dette technique legacy, hors scope réécriture

  # RG-025-013 exclu — dette technique legacy, hors scope réécriture

  # RG-025-014 exclu — dette technique legacy, hors scope réécriture
