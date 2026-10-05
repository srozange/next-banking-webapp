---
feature: FEAT-010
name: Chat avec le conseiller
status: current
step: retro-rules
date: 2026-10-03
sources:
  - src/main/webapp/conseiller.xhtml
  - src/main/java/com/accenture/bank/conseiller/ConseillerBean.java
  - src/main/java/com/accenture/bank/conseiller/ConseillerService.java
---

# FEAT-010 — Chat avec le conseiller : Messages et libellés

Tous les textes visibles à l'écran (libellés, boutons, messages, états) au mot près, tels qu'ils figurent dans le DOM ou sont émis par le code. Les `{variables}` désignent des valeurs dynamiques.

---

## En-tête et navigation

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Titre de navigateur | BankApp Demo - Mon Conseiller | Onglet du navigateur |
| Marque | Demo Bank | Logo/titre dans la barre d'en-tête |
| Action | Déconnexion | Lien de déconnexion dans la barre d'en-tête |
| Navigation | Tableau de bord | Lien vers dashboard.xhtml |
| Navigation | Virement | Lien vers virement.xhtml |
| Navigation | Virements programmes | Lien vers virement-programme.xhtml |
| Navigation | Releve | Lien vers releve.xhtml |
| Navigation | Budget | Lien vers budget.xhtml |
| Navigation | Prets | Lien vers pret.xhtml |
| Navigation | Documents | Lien vers documents.xhtml |
| Navigation | Bourse | Lien vers bourse.xhtml |
| Navigation | Mon Conseiller | Lien actif (cette page) |
| Navigation | Produits | Lien vers produits.xhtml |
| Navigation | Comptes Tiers | Lien vers comptes-tiers.xhtml |
| Navigation | Beneficiaires | Lien vers beneficiaires.xhtml |
| Navigation | Espace Conseiller | Lien conditionnel (rôle ADMIN ou CONSEILLER) |
| Navigation | Administration | Lien conditionnel (rôle ADMIN uniquement) |

**Note** : cette page n'affiche pas de `Bonjour, {fullName}` dans la barre d'en-tête (contrairement aux autres pages). L'accueil nominatif est porté uniquement par le message de bienvenue de Sophie.

---

## Bandeau IA

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Libellé | Conseiller IA | Titre du bandeau IA |
| Libellé | GPT-5.4-nano | Nom du modèle dans le bandeau |
| Libellé | Azure AI Foundry | Plateforme IA dans le bandeau |
| Libellé | EJB Stateless WildFly 14 | Composant technique dans le bandeau |

---

## En-tête de la carte de chat

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Nom du conseiller | Sophie — Conseillère Demo Bank | En-tête de la carte de chat |
| Statut en ligne | En ligne · Répond en quelques secondes | Indicateur de disponibilité |
| Action | Nouvelle conversation | Bouton de réinitialisation de la conversation |

---

## Message de bienvenue (message initial de l'assistant)

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Message d'accueil (cas normal) | Bonjour {prenom} ! Je suis Sophie, votre conseillere Demo Bank. Je suis la pour repondre a vos questions sur vos comptes, vos placements ou tout projet financier. Comment puis-je vous aider aujourd'hui ? | Premier message de l'assistant — `{prenom}` = premier mot de `customer.getFullName()` |
| Message d'accueil (client introuvable) | Bonjour cher client ! Je suis Sophie, votre conseillere Demo Bank. Je suis la pour repondre a vos questions sur vos comptes, vos placements ou tout projet financier. Comment puis-je vous aider aujourd'hui ? | Substitution quand `currentCustomer` est null |

---

## Actions rapides (suggestions contextuelles)

Affichés uniquement quand la conversation ne contient que le message d'accueil (conversation fraîche). Disparaissent après le premier envoi.

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Libellé bouton | Optimiser mon epargne | Bouton d'action rapide |
| Message envoyé | Je voudrais optimiser mon epargne, que me conseillez-vous ? | Message pré-défini associé au bouton ci-dessus |
| Libellé bouton | Projet immobilier | Bouton d'action rapide |
| Message envoyé | Je souhaite acheter un appartement, comment preparer mon dossier ? | Message pré-défini associé au bouton ci-dessus |
| Libellé bouton | Investir en bourse | Bouton d'action rapide |
| Message envoyé | Je veux commencer a investir en bourse, par ou commencer ? | Message pré-défini associé au bouton ci-dessus |
| Libellé bouton | Preparer ma retraite | Bouton d'action rapide |
| Message envoyé | Comment preparer ma retraite efficacement ? | Message pré-défini associé au bouton ci-dessus |

---

## Zone de saisie et envoi

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Placeholder | Posez votre question a Sophie... | Textarea de saisie du message utilisateur |
| Bouton | Envoyer | Bouton d'envoi du message |
| Bouton (état chargement) | ... | Valeur du bouton Envoyer pendant l'attente AJAX — conseiller.xhtml (script JS):226 |

---

## Messages d'erreur du backend IA (retournés comme réponse de l'assistant)

| Type | Texte exact | Contexte observé |
| --- | --- | --- |
| Erreur HTTP | Je suis momentanement indisponible (HTTP {status}). Veuillez reessayer. | Réponse IA quand Azure OpenAI retourne un statut non-200 ; `{status}` = code HTTP reçu |
| Erreur technique | Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence. | Réponse IA lors d'une exception (timeout, réseau, parsing) |

**Note** : ces messages sont affichés dans une bulle de l'assistant dans la conversation, pas dans une zone d'erreur distincte. Ils sont déclenchés par RG-010-020 et RG-010-021.
