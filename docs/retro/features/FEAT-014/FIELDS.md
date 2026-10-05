# FIELDS — FEAT-014 « Callback OAuth Powens »

_Généré par retro-ui depuis powens-callback.xhtml + PowensCallbackBean.java._

## Aucun champ de formulaire

La page est un intermédiaire de redirection sans interface utilisateur interactive.
Elle ne contient aucun `<input>`, `<select>` ni `<textarea>`.

## Paramètres GET (lus par JSF f:viewParam, non affichés)

| Paramètre URL | Mappé sur | Obligatoire | Description |
|---|---|---|---|
| `connection_id` | `powensCallbackBean.connectionId` | Non | Identifiant de connexion retourné par Powens Webview (chemin nominal) |
| `code` | `powensCallbackBean.code` | Non | Code OAuth (flow alternatif — fallback si connection_id absent) |
| `error` | `powensCallbackBean.error` | Non | Code d'erreur Powens (ex. `access_denied`) — prioritaire sur connection_id |

**Source** : (f:viewParam) (champs).

## Comportement

Le seul élément rendu est un spinner CSS animé avec le texte « Connexion en cours... / Recuperation de vos comptes via Powens Open Banking ».
Il n'est jamais visible car la `f:viewAction` processCallback() redirige côté serveur avant tout envoi de réponse HTML.

Tous les chemins de `processCallback()` redirigent vers `/comptes-tiers?faces-redirect=true`.
