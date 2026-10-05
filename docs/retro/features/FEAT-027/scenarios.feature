Feature: Détection de fraude en temps réel
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @confirme
  # RG-027-003 — comportement corrigé proposé (BUG-084, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-084 @proposition
  Scenario: RG-027-003 — fraud alerts are detected for a transfer
    Given the legacy context of rule RG-027-003
    When fraud alerts are detected for a transfer
    Then execute fraud detection before the transfer is finally committed; IF the fraud check raises an error THE SYSTEM SHALL log the error and the transfer SHALL NOT proceed silently.

  @confirme
  @confirme
  @confirme
  # RG-027-006 — comportement corrigé proposé (BUG-085, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-085 @proposition
  Scenario: RG-027-006 — the number of transactions executed by the customer on the current day exceeds 8
    Given the legacy context of rule RG-027-006
    And the number of transactions executed by the customer on the current day exceeds 8
    When the action described by rule RG-027-006 is performed
    Then raise a HIGH_FREQUENCY fraud signal with risk score 50.

  @confirme
  @confirme
  @confirme
  @confirme
  @a-confirmer
  @a-confirmer
  @confirme
  @confirme
  @confirme
  @suspect
  # RG-027-018 — comportement corrigé proposé (BUG-086, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-086 @proposition
  Scenario: RG-027-018 — mask the target IBAN in all server log entries (e.g. FR76****1234) to avoid …
    Given the legacy context of rule RG-027-018
    When the action described by rule RG-027-018 is performed
    Then mask the target IBAN in all server log entries (e.g. FR76****1234) to avoid exposing sensitive payment data.

  # RG-027-019 exclu — dette technique legacy, hors scope réécriture
