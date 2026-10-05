Feature: Audit trail (journalisation des actions)
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @confirme
  @confirme
  @confirme
  @confirme
  @confirme
  # RG-026-010 — comportement corrigé proposé (BUG-083, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-083 @proposition
  Scenario: RG-026-010 — any user action is audited
    Given the legacy context of rule RG-026-010
    When any user action is audited
    Then record the originating client IP address in the audit entry.

  @confirme
