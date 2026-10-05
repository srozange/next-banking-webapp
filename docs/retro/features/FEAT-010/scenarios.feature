Feature: Chat avec le conseiller
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @a-confirmer
  # Initialisation de la session de chat
  @confirme
  Scenario: RG-010-003 — a user opens the chat page,
    # Capture golden master : SC-010-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-010-01 : La page de chat charge avec le message de bienvenue de Sophie adressé au prénom de l'utilisateur
    When a user opens the chat page,
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/conseiller.xhtml
    Then display a personalized welcome message from Sophie, addressing the user by the first name extracted from the authenticated session's customer record.

  @a-confirmer
  @confirme
  Scenario: RG-010-005 — a user opens the chat page,
    # Capture golden master : SC-010-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-010-01 : La page de chat charge avec le message de bienvenue de Sophie adressé au prénom de l'utilisateur
    When a user opens the chat page,
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/conseiller.xhtml
    Then initialize a fresh conversation containing only the welcome message before any user interaction.

  # Actions rapides (suggestions contextuelles)
  @confirme
  Scenario: RG-010-006 — the conversation contains only the welcome message
    # Capture golden master : SC-010-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-010-01 : La page de chat charge avec le message de bienvenue de Sophie adressé au prénom de l'utilisateur
    And the conversation contains only the welcome message
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/conseiller.xhtml
    Then display four quick action buttons.

  @confirme
  Scenario: RG-010-007 — a quick action button is clicked,
    # Capture golden master : SC-010-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-010-02 : Envoyer un message au chat affiche le message de l'utilisateur et une réponse de Sophie (HTTP 401 en local sans Azure)
    And les données saisies : #chatForm\:userInput = « Quel est mon solde courant ? »
    When a quick action button is clicked,
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/conseiller.xhtml
    And il saisit « Quel est mon solde courant ? » dans #chatForm\:userInput
    And il clique sur input[id='chatForm:sendBtn']
    And il attend le texte « None »
    Then send the predefined text associated with that button as the user message and hide all quick action buttons for the remainder of the session. Prédéfinis : - « Optimiser mon epargne »  → « Je voudrais o

  @a-confirmer
  # RG-010-010 exclu — dette technique legacy, hors scope réécriture

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @confirme
  # BUG-038 — comportement corrigé proposé (bug d'origine scénario SC-010-02, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @BUG-038 @proposition
  Scenario: BUG-038 — comportement corrigé (SC-010-02)
    Given the context of golden master capture SC-010-02
    And the AI advisor service returns an authentication error
    When the action of golden master capture SC-010-02 is performed
    Then inform the user that the advisor is temporarily unavailable instead of exposing an authentication failure code.
