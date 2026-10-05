### FEAT-014 — Callback OAuth Powens

**Description :** Point de retour appelé par le navigateur quand l'utilisateur quitte la page web de connexion de Powens, après avoir connecté (ou non) une banque depuis l'écran « Comptes Tiers » (FEAT-012). Il n'a pas d'écran propre ni de champ : il lit les paramètres reçus (`error`, `connection_id`, `code`), déclenche le traitement adapté, puis redirige toujours vers l'écran « Comptes Tiers », où s'affiche le résultat ou le message d'erreur. Il est accessible sans authentification ; les cas limites sont le retour en erreur, le retour incomplet, la session Powens expirée et la connexion réussie sans compte encore disponible. Tous les rôles sont concernés (CLIENT, CONSEILLER, ADMIN), puisque le traitement repose sur la session de l'utilisateur qui a lancé la connexion. Scénarios golden master : SC-014-01.

**Exigences fonctionnelles :**

#### FR-014-01 : Accès sans authentification

Le point de retour de Powens est accessible sans authentification. Quel que soit le cas traité, il répond par une redirection vers l'écran « Comptes Tiers », sans afficher de contenu propre ; appelé sans aucun paramètre, il répond par une redirection (statut 302).

Source : RG-014-001

#### FR-014-02 : Aiguillage par ordre de priorité

Les paramètres sont examinés dans cet ordre, et seul le premier cas qui s'applique est traité ; un paramètre vide compte comme absent :
1. `error` présent : la session Powens est remise à zéro (FR-014-07), le motif est affiché à l'utilisateur (FR-012-07, correction BUG-040), puis redirection vers « Comptes Tiers » ;
2. sinon, `connection_id` présent : le chargement des comptes est lancé avec cet identifiant (FR-014-05, FR-014-06), puis redirection ;
3. sinon, `code` présent : voir FR-014-04 ;
4. sinon : le système pose le message « Callback Powens incomplet. Parametres recus: [<liste des noms de paramètres reçus>] » (par exemple « Callback Powens incomplet. Parametres recus: [] »), puis redirection ; l'écran reste à l'état non connecté.

Source : RG-014-002, RG-014-003, RG-014-005, RG-012-009, BUG-040

#### FR-014-03 : Redirection systématique

Après traitement, succès comme erreur, l'utilisateur est toujours redirigé vers l'écran « Comptes Tiers », qui affiche l'état et les messages issus du traitement.

Source : RG-014-002, RG-014-003, RG-014-005

#### FR-014-04 : Retour portant seulement un code

Le corpus décrit un parcours alternatif où, sans `error` ni `connection_id`, un `code` non vide déclenche le chargement des comptes avec ce code. Son usage réel par Powens n'est pas confirmé (question bloquante du corpus) ; tant qu'il ne l'est pas, la cible ne traite que `connection_id` et la branche « code » n'est pas reprise. [ASSUMPTION A-014-1 : d'ici la confirmation, un retour qui ne porte qu'un `code` est traité comme un retour incomplet (cas 4 de FR-014-02), avec le message « Callback Powens incomplet. Parametres recus: [code] » ; RG-012-011 et RG-028-009, règles jumelles, ne sont pas reprises pour la même raison (FR-012-06, FR-028-04) ; la condition du retour incomplet de RG-014-005 et RG-012-008 est ainsi élargie au retour qui ne porte qu'un `code`]

Source : RG-014-004, RG-014-005, RG-012-008

#### FR-014-05 : Session Powens expirée

Si le chargement des comptes est lancé alors que la session de l'utilisateur ne contient pas le jeton Powens, le chargement est abandonné et le système affiche, sur l'écran « Comptes Tiers », « Session expiree. Veuillez recommencer la connexion. ».

Source : RG-014-006

#### FR-014-06 : Connexion établie

Avec un jeton Powens valide en session : si Powens renvoie au moins un compte, la connexion est marquée établie, le premier compte est sélectionné automatiquement et ses transactions sont chargées ; si Powens ne renvoie aucun compte, la connexion est marquée établie et le système affiche « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. ». Une réponse en erreur de Powens n'est pas une liste vide : elle suit FR-012-14. Aucune attente n'est imposée avant la lecture des comptes : RG-014-010 n'est pas reprise (A-014-2, voir les notes de FEAT-014).

Source : RG-014-007, RG-014-008

#### FR-014-07 : Remise à zéro de la session Powens

La déconnexion Powens remet à zéro tout l'état Powens de la session : jeton d'accès, liste des comptes externes, compte sélectionné, liste des transactions et indicateur de connexion. Le message d'erreur est aussi effacé, comme pour la déconnexion de FR-012-15 [ASSUMPTION A-014-3 : la remise à zéro au retour de Powens efface aussi le message d'erreur, comme la déconnexion manuelle ; RG-014-009 ne le mentionne pas] ; dans le cas `error` de FR-014-02, le message du motif est posé après cette remise à zéro.

Source : RG-014-009

#### FR-014-08 : Vingt transactions au plus par compte externe

Au chargement des transactions d'un compte externe, au plus 20 transactions sont récupérées auprès de Powens. Les deux règles voisines donnent la même valeur (20) ; aucune différence n'est à arbitrer. La même limite figure dans FR-012-13.

Source : RG-014-011, RG-028-018

**Notes :**
- RG-014-010 (Suspect, dette legacy) : non reprise — le legacy attend 2 secondes de façon bloquante avant de lire les comptes, ce qui n'est pas une exigence métier ; le bug associé BUG-051 (pause bloquante au retour de Powens) est classé « ne pas corriger ». [ASSUMPTION A-014-2 : aucune attente de synchronisation n'est imposée avant la lecture des comptes ; une synchronisation non terminée donne le message de FR-014-06 et l'utilisateur rafraîchit (FR-012-16)]
- Les textes de la page de transit du legacy (« Connexion bancaire en cours... », « Connexion en cours... », « Recuperation de vos comptes via Powens Open Banking ») ne sont jamais vus, la redirection ayant lieu avant tout affichage ; ils ne sont pas repris.
- RG-014-007 et RG-028-014 laissent la connexion « établie » même sans compte ; RG-014-009 n'efface pas le message d'erreur alors que RG-012-024 et RG-028-023 l'effacent : FR-014-07 retient l'effacement, cohérent avec la déconnexion de FEAT-012.
