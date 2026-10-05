---
feature: FEAT-030
name: Contrôle d'accès RBAC
status: current
step: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/java/com/accenture/bank/web/AppFilter.java
  - src/main/java/com/accenture/bank/web/SecurityBean.java
  - src/main/java/com/accenture/bank/web/DocumentDownloadServlet.java
---

# MESSAGES — FEAT-030 « Contrôle d'accès RBAC »

Inventaire exhaustif des textes visibles ou émis par le système dans le périmètre de la
fonctionnalité « Contrôle d'accès RBAC ». FEAT-030 est une préoccupation transversale : elle
n'expose aucune page dédiée. Ses effets sur l'utilisateur se traduisent par des redirections HTTP
silencieuses (aucun texte), des messages d'erreur HTTP sur l'endpoint `/docs/download`, et la
visibilité conditionnelle de liens de navigation communs à tous les gabarits. Les messages de la
page de connexion (`login.xhtml`) sont propriété de FEAT-001 et sont référencés ici, non répétés.

---

## Redirections (aucun texte visible)

`AppFilter` et `DocumentDownloadServlet` redirigent les accès non autorisés par `sendRedirect`
sans afficher de texte à l'utilisateur. Ces comportements sont documentés comme règles dans
`RG.md` (RG-030-002, RG-030-003, RG-030-004, RG-030-012) ; aucun libellé n'est à lister ici.

---

## Messages de la page de connexion — propriété de FEAT-001

Les textes affichés sur `login.xhtml` (titre, libellés, messages d'erreur d'authentification,
messages de validation) sont documentés dans `FEAT-001/MESSAGES.md`. Ils sont atteints par
les redirections de FEAT-030 mais ne sont pas émis par ses composants propres.

---

## Endpoint `/docs/download` — messages d'erreur HTTP (DocumentDownloadServlet)

Ces textes sont envoyés comme corps de réponse HTTP via `resp.sendError(code, texte)`. Ils
ne sont pas directement présentés dans l'interface JSF ; ils peuvent apparaître dans la page
d'erreur du conteneur ou dans l'outil développeur du navigateur.

| Code HTTP | Texte exact | Condition d'émission | Preuve |
|---|---|---|---|
| 400 | `Parametre type manquant` | Paramètre `type` absent de la requête GET `/docs/download` | |
| 400 | `accountId manquant` | Paramètre `accountId` absent pour les types RELEVE, RIB, ATTESTATION | |
| 400 | `Type inconnu : {type}` | Valeur du paramètre `type` non reconnue (`{type}` = valeur reçue) | |
| 500 | `Erreur lors de la generation du document` | Exception inattendue lors de la génération du PDF | |

> **Note** : le code 404 avec le message de l'`IllegalArgumentException` levée par `DocumentService`
> est également possible (ex. compte inexistant) — son texte dépend du service, hors périmètre de
> FEAT-030 (géré par FEAT-015 « Documents »). Preuve :.

---

## Liens de navigation conditionnels (SecurityBean)

Ces libellés apparaissent dans les gabarits communs de toutes les pages authentifiées ; ils sont
documentés dans chaque MESSAGES.md de feature qui utilise le gabarit. La règle d'affichage
conditionnel est propriété de FEAT-030 (RG-030-008, RG-030-009).

| Libellé | Condition d'affichage | Règle | Exemple de preuve |
|---|---|---|---|
| `Espace Conseiller` | `securityBean.adminOrConseiller` vrai (rôle CONSEILLER ou ADMIN) | RG-030-008 | |
| `Administration` | `securityBean.admin` vrai (rôle ADMIN uniquement) | RG-030-009 | |
