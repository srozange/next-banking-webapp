---
feature: FEAT-010
feature_name: "Chat avec le conseiller"
generated_by: retro-rules
date: 2026-10-03
---

# RG — FEAT-010 « Chat avec le conseiller »

_Règles extraites de : `ConseillerBean.java`, `ConseillerService.java`, `ChatMessage.java`,
`AppConfig.java`, `AppFilter.java`, `conseiller.xhtml` — commit c05512e._

---

## Contrôle d'accès

RG-010-001 [Confirmé]
WHEN an unauthenticated user requests /conseiller.xhtml, THE SYSTEM SHALL redirect to /login.xhtml.

RG-010-002 [À confirmer]
THE SYSTEM SHALL grant access to /conseiller.xhtml to any authenticated user regardless of role (CLIENT, CONSEILLER, ADMIN).

---

## Initialisation de la session de chat

RG-010-003 [Confirmé]
WHEN a user opens the chat page, THE SYSTEM SHALL display a personalized welcome message from Sophie, addressing the user by the first name extracted from the authenticated session's customer record.

RG-010-004 [À confirmer]
IF no customer record is found in the HTTP session THEN THE SYSTEM SHALL substitute the string "cher client" as the display name in the welcome message.

RG-010-005 [Confirmé]
WHEN a user opens the chat page, THE SYSTEM SHALL initialize a fresh conversation containing only the welcome message before any user interaction.

---

## Actions rapides (suggestions contextuelles)

RG-010-006 [Confirmé]
WHILE the conversation contains only the welcome message, THE SYSTEM SHALL display four quick action buttons.

RG-010-007 [Confirmé]
WHEN a quick action button is clicked, THE SYSTEM SHALL send the predefined text associated with that button as the user message and hide all quick action buttons for the remainder of the session.
  Prédéfinis :
  - « Optimiser mon epargne »  → « Je voudrais optimiser mon epargne, que me conseillez-vous ? »
  - « Projet immobilier »      → « Je souhaite acheter un appartement, comment preparer mon dossier ? »
  - « Investir en bourse »     → « Je veux commencer a investir en bourse, par ou commencer ? »
  - « Preparer ma retraite »   → « Comment preparer ma retraite efficacement ? »

---

## Envoi de message utilisateur

RG-010-008 [À confirmer]
IF the user submits an empty or blank message THEN THE SYSTEM SHALL ignore the send action without displaying an error.

RG-010-009 [À confirmer]
WHEN a user sends a message, THE SYSTEM SHALL add the user message to the conversation history, call the IA backend, and append the IA response as an assistant message.

RG-010-010 [Suspect]
WHEN a user submits a message via the send button, THE SYSTEM SHALL clear the message textarea after submission.

---

## Historique de conversation

RG-010-011 [À confirmer]
IF the user's message is the first message in the conversation THEN THE SYSTEM SHALL call the IA backend with an empty conversation history.

RG-010-012 [À confirmer]
WHEN the user sends a message after the first exchange, THE SYSTEM SHALL include all previous conversation turns in the IA context, excluding the initial welcome message and the current user input.

---

## Appel au backend IA (Azure OpenAI)

RG-010-013 [À confirmer]
THE SYSTEM SHALL call the Azure OpenAI Chat Completions API using the endpoint, deployment name, and API version resolved in priority order: environment variable, JVM system property, hardcoded default.
  Variables : AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_DEPLOYMENT, AZURE_OPENAI_API_VERSION, AZURE_OPENAI_API_KEY

RG-010-014 [À confirmer]
THE SYSTEM SHALL send a fixed system prompt with every request, defining the Sophie persona, her six domains of expertise, and her behavioral constraints.

RG-010-015 [À confirmer]
THE SYSTEM SHALL configure the IA to never provide firm investment advice and to mention that investments carry risks, in compliance with MiFID II.

RG-010-016 [À confirmer]
THE SYSTEM SHALL configure the IA to respond exclusively in French.

RG-010-017 [À confirmer]
THE SYSTEM SHALL configure the IA to provide concise responses of 3 to 5 sentences, unless the user explicitly requests a detailed response.

RG-010-018 [À confirmer]
THE SYSTEM SHALL limit the IA response to 500 completion tokens and apply a generation temperature of 0.7 per request.

RG-010-019 [À confirmer]
THE SYSTEM SHALL apply a 15-second connection timeout and a 30-second read timeout on every call to the Azure OpenAI API.

---

## Gestion des erreurs du backend IA

RG-010-020 [Confirmé]
IF the Azure OpenAI API returns a non-200 HTTP status THEN THE SYSTEM SHALL display a degraded message to the user that includes the HTTP status code received.

RG-010-021 [À confirmer]
IF an exception occurs during the Azure OpenAI call THEN THE SYSTEM SHALL display a degraded message inviting the user to retry or contact their advisor in an agency.

---

## Réinitialisation de conversation

RG-010-022 [Confirmé]
WHEN the user clicks « Nouvelle conversation », THE SYSTEM SHALL clear all messages and reinitialize the conversation with a fresh personalized welcome message.

---

## Comportement de l'interface utilisateur

RG-010-023 [Confirmé]
WHEN an AJAX request is in progress, THE SYSTEM SHALL disable the send button and restore it upon completion.

RG-010-024 [À confirmer]
WHEN the Enter key is pressed in the message textarea without the Shift key held, THE SYSTEM SHALL submit the chat form.

RG-010-025 [Confirmé]
WHEN an assistant response is received, THE SYSTEM SHALL scroll the message list to show the latest message.
