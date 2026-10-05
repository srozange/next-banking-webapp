Feature: Traitement asynchrone des virements (JMS)
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  # RG-023-002 — comportement corrigé proposé (BUG-073, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-073 @proposition
  Scenario: RG-023-002 — VirementProducer cannot establish a JMS connection …
    Given the legacy context of rule RG-023-002
    And VirementProducer cannot establish a JMS connection (ConnectionFactory or Queue is null)
    When the action described by rule RG-023-002 is performed
    Then reject the transfer and inform the customer instead of reporting it as submitted.

  # Traitement dans le MDB
  # RG-023-003 exclu — dette technique legacy, hors scope réécriture

  @confirme
  @confirme
  @confirme
  # RG-023-008 exclu — dette technique legacy, hors scope réécriture

  # Comportement suspect — notification FRAUDE vs exécution effective
  # RG-023-009 — comportement corrigé proposé (BUG-076, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-076 @proposition
  Scenario: RG-023-009 — virement amount >= seuilFraudeEur (default: 10 000 EUR)
    Given the legacy context of rule RG-023-009
    And virement amount >= seuilFraudeEur (default: 10 000 EUR)
    When the action described by rule RG-023-009 is performed
    Then suspend the transfer pending manual fraud verification before applying any balance change.

  @confirme
