---
feature: FEAT-001
name: Connexion
status: current
extracted_at: 2026-10-03
extractor: retro-rules
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# MESSAGES — FEAT-001 « Connexion »

_Sources : `src/main/webapp/login.xhtml`, `src/main/java/com/accenture/bank/web/LoginBean.java`, observé via retro-ui (screens/SCR-001-*)._

---

## Écran SCR-001-01 — Formulaire de connexion (état vide / chargé)

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Titre (onglet) | BankApp Demo - Connexion | Titre de l'onglet navigateur | |
| Logo | DB | Initiales dans le disque rouge (logo) | |
| Titre principal | Demo Bank | Nom de l'application, à côté du logo | |
| Sous-titre | Espace client — Connexion sécurisée | Phrase d'accroche sous le titre | |
| Libellé | Identifiant | Label du champ username | |
| Libellé | Mot de passe | Label du champ password | |
| Action | Me connecter | Bouton de soumission du formulaire | |
| Badge technique | ⚠️ Système Legacy — JEE 8 / WildFly 14 | Pied du formulaire — texte informatif non fonctionnel | |

---

## Écran SCR-001-02 — Erreurs de validation (champs vides)

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Erreur de validation | Identifiant requis | Affiché quand le champ `loginForm:username` est vide à la soumission. En dev uniquement (zone « Unhandled Messages ») — invisible en production via `h:messages globalOnly="true"` | `requiredMessage="Identifiant requis"` |
| Erreur de validation | Mot de passe requis | Affiché quand le champ `loginForm:password` est vide à la soumission. Même contrainte de visibilité dev/prod. | `requiredMessage="Mot de passe requis"` |

> **Dette-legacy** : les deux messages ci-dessus sont des messages JSF par-champ (`clientId ≠ null`). La zone `h:messages globalOnly="true"` de login.xhtml ne les capture pas en mode Production — ils restent invisibles pour l'utilisateur réel. Voir VALIDATION.md VAL-001-01-username-F1 et VAL-001-01-password-F1.

---

## Écran SCR-001-04 — Erreur d'authentification (identifiants incorrects)

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Erreur d'authentification (résumé) | Identifiants incorrects | Message global affiché sous le bouton en cas d'identifiants invalides (`FacesMessage.SEVERITY_ERROR`, `clientId=null`) | |
| Erreur d'authentification (détail) | Vérifiez votre login et mot de passe | Détail du même message affiché conjointement avec le résumé | |
