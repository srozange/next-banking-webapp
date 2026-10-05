---
feature: FEAT-028
name: "Intégration Powens (Open Banking — agrégation)"
status: current
step: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/java/com/accenture/bank/powens/PowensBean.java
  - src/main/java/com/accenture/bank/powens/PowensCallbackBean.java
---

# Messages et libellés — FEAT-028 Intégration Powens (Open Banking — agrégation)

FEAT-028 est une couche d'intégration backend uniquement (PowensService, PowensBean, PowensCallbackBean) ; elle ne possède pas de page xhtml propre. Les libellés de l'interface utilisateur sont portés par FEAT-012 (comptes-tiers.xhtml) et FEAT-014 (powens-callback.xhtml). Ce fichier inventorie exclusivement les messages textuels émis programmatiquement par les beans Java du périmètre et exposés à la vue via le champ `powensBean.errorMessage`.

> Note d'encodage : les messages sont des littéraux Java ; les caractères accentués sont volontairement absents dans certains textes (ex. « d initialiser » sans apostrophe, « expiree » sans accent, « reessayez » sans accent) — orthographe reproduite telle quelle depuis le source.

---

## Messages d'erreur et d'état (émis par PowensBean)

| Identifiant | Texte exact (source) | Condition d'affichage | Règle associée | Preuve |
|---|---|---|---|---|
| MSG-028-001 | `Impossible d initialiser la connexion Powens.` | Échec de l'appel Powens /auth/init (résultat null) | RG-028-004 | |
| MSG-028-002 | `Session expiree. Veuillez recommencer la connexion.` | Token de session Powens absent à la réception du callback | RG-028-013 | |
| MSG-028-003 | `Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes.` | Callback traité avec succès mais aucun compte retourné par l'API Powens | RG-028-015 | |

---

## Messages d'erreur (émis par PowensCallbackBean)

| Identifiant | Texte exact (source) | Condition d'affichage | Règle associée | Preuve |
|---|---|---|---|---|
| MSG-028-010 | `Callback Powens incomplet. Parametres recus: {params.keySet()}` | Callback reçu sans `connection_id` ni `code` ; `{params.keySet()}` est la liste des noms de paramètres effectivement reçus, interpolée à l'exécution | RG-028-010 | |
