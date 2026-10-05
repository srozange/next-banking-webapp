# FIELDS — FEAT-010 « Chat avec le conseiller »

_Source : crawl MCP Playwright, rôles CLIENT, CONSEILLER, ADMIN — 2026-10-03._

---

## Champs de formulaire

| Champ | Type | Label | Obligatoire | Max | Pattern | Disabled | Readonly | Défaut |
|---|---|---|---|---|---|---|---|---|
| `chatForm:userInput` | textarea | _(aucun)_ | non | — | — | non | non | — |

**Note label** : le champ de saisie n'a pas de `<label>` HTML associé — identifiable uniquement par sa position visuelle à gauche du bouton Envoyer. Aucun attribut `aria-label` dans l'arbre d'accessibilité.

**Dette-legacy (champ non vidé)** : après envoi du formulaire (POST AJAX), le textbox conserve le texte saisi. Le champ n'est pas réinitialisé par le bean JSF côté serveur.

---

## Boutons d'action

| Bouton | Type | Action déclenchée |
|---|---|---|
| Envoyer | submit | POST `chatForm` — envoi du message au conseiller IA (Azure AI Foundry / GPT-5.4-nano via EJB Stateless) |
| Nouvelle conversation | action AJAX | POST AJAX — réinitialise la conversation côté serveur, envoie une nouvelle salutation Sophie |

---

## Boutons d'actions rapides (suggestions contextuelles)

Affichés uniquement sur la conversation fraîche (Chargé / Vide). Disparaissent après le premier clic (activation dynamique unique par session).

| Label bouton | Message pré-défini envoyé |
|---|---|
| Optimiser mon epargne | `Je voudrais optimiser mon epargne, que me conseillez-vous ?` |
| Projet immobilier | _(observé dans le DOM, message non capturé)_ |
| Investir en bourse | _(observé dans le DOM, message non capturé)_ |
| Preparer ma retraite | _(observé dans le DOM, message non capturé)_ |

---

## Comportement par rôle

| Rôle | Accès | Différence observée |
|---|---|---|
| CLIENT | Complet | Salutation : « Bonjour Jean ! » (prénom de user1) |
| CONSEILLER | Complet | Salutation : « Bonjour Sophie ! » (prénom du compte conseiller) |
| ADMIN | Complet | Salutation : « Bonjour Alice ! » (prénom du compte admin). Nav enrichie : liens Espace Conseiller + Administration |

Aucun état `DroitsRéduits` : tous les rôles ont accès complet à l'interface de chat.

---

## Activations dynamiques

| Élément | Condition d'apparition | Condition de disparition |
|---|---|---|
| Boutons d'actions rapides (4) | Conversation fraîche (Chargé / Vide) | Premier clic sur l'un des boutons — non ré-affichés dans la même session |
| Réponse Sophie (message IA) | Après envoi de tout message (POST chatForm ou action rapide) | — |

---

## États non atteignables dans l'environnement local

| État | Raison |
|---|---|
| Confirmation (réponse IA réussie) | Azure AI Foundry / GPT-5.4-nano retourne HTTP 401 en local — aucune clé API configurée |
| Chargement (spinner IA) | Réponse HTTP 401 instantanée — état transitoire non observable |
| Modale | Aucune boîte de confirmation n'existe dans cette feature |
