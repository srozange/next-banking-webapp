### FEAT-010 — Chat avec le conseiller

**Description :** Tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) dialogue avec « Sophie », une conseillère virtuelle animée par un service d'IA externe (Azure OpenAI), depuis l'écran « Mon Conseiller ». À l'ouverture, une conversation neuve démarre avec un message de bienvenue personnalisé au prénom de l'utilisateur et quatre actions rapides. Chaque message envoyé est transmis au service d'IA avec l'historique de la conversation ; la réponse s'affiche comme un message de Sophie. Les indisponibilités du service d'IA se traduisent par un message dégradé affiché dans la conversation, jamais par une page d'erreur. L'utilisateur peut repartir d'une conversation neuve à tout moment. Scénarios golden master : SC-010-01, SC-010-02.

**Exigences fonctionnelles :**

#### FR-010-01 : Accès aux écrans ouverts à tout utilisateur authentifié

L'écran « Mon Conseiller » (chat), l'écran du catalogue des produits et l'écran des bénéficiaires sont accessibles à tout utilisateur authentifié, quel que soit son rôle (CLIENT, CONSEILLER, ADMIN), sans différence de contenu selon le rôle. Le corpus donne la même règle pour les trois écrans (même ensemble de rôles, aucune restriction) ; aucune différence de valeur n'est à arbitrer. L'utilisateur non authentifié est redirigé vers la page de connexion (exigence fusionnée de FEAT-008).

Source : RG-010-002, RG-011-001, RG-013-002

#### FR-010-02 : Message de bienvenue personnalisé

À l'ouverture de l'écran, le système affiche l'en-tête « Sophie — Conseillère Demo Bank », l'indicateur « En ligne · Répond en quelques secondes », et un premier message de Sophie : « Bonjour {prenom} ! Je suis Sophie, votre conseillere Demo Bank. Je suis la pour repondre a vos questions sur vos comptes, vos placements ou tout projet financier. Comment puis-je vous aider aujourd'hui ? », où {prenom} est le premier mot du nom complet du client connecté. Si aucun client n'est associé à la session de l'utilisateur, {prenom} est remplacé par « cher client » (« Bonjour cher client ! … »).

Source : RG-010-003, RG-010-004

#### FR-010-03 : Conversation neuve à chaque ouverture

À chaque ouverture de l'écran, la conversation est réinitialisée et ne contient que le message de bienvenue, avant toute interaction de l'utilisateur.

Source : RG-010-005

#### FR-010-04 : Actions rapides

Tant que la conversation ne contient que le message de bienvenue, quatre boutons d'action rapide sont affichés. Un clic sur un bouton envoie, comme message de l'utilisateur, le texte prédéfini associé, puis masque les quatre boutons pour le reste de la conversation :
- « Optimiser mon epargne » → « Je voudrais optimiser mon epargne, que me conseillez-vous ? »
- « Projet immobilier » → « Je souhaite acheter un appartement, comment preparer mon dossier ? »
- « Investir en bourse » → « Je veux commencer a investir en bourse, par ou commencer ? »
- « Preparer ma retraite » → « Comment preparer ma retraite efficacement ? »

Le message envoyé suit ensuite le même traitement que FR-010-05, y compris la gestion des indisponibilités de FR-010-09.

Source : RG-010-006, RG-010-007

#### FR-010-05 : Envoi d'un message

La zone de saisie porte le texte indicatif « Posez votre question a Sophie... » et le bouton « Envoyer ». À l'envoi d'un message non vide, le système ajoute le message de l'utilisateur à la conversation, interroge le service d'IA, puis ajoute la réponse comme message de Sophie. Un message vide ou composé uniquement d'espaces est ignoré, sans message d'erreur ni appel au service d'IA.

Source : RG-010-008, RG-010-009

#### FR-010-06 : Historique transmis au service d'IA

Pour le premier message de la conversation, le service d'IA est interrogé avec un historique vide. Pour les messages suivants, l'historique transmis contient tous les échanges précédents, à l'exclusion du message de bienvenue et du message en cours (envoyé séparément).

Source : RG-010-011, RG-010-012

#### FR-010-07 : Consignes de comportement de Sophie

Chaque requête au service d'IA porte une consigne fixe qui définit la persona Sophie, ses six domaines d'expertise et ses contraintes de comportement. Cette consigne impose : ne jamais donner de conseil d'investissement ferme et mentionner que les placements comportent des risques (conformité MiFID II) ; répondre exclusivement en français ; répondre de façon concise, en 3 à 5 phrases, sauf si l'utilisateur demande explicitement une réponse détaillée. Chaque réponse est limitée à 500 unités de génération (tokens), avec une température de génération de 0,7.

Source : RG-010-014, RG-010-015, RG-010-016, RG-010-017, RG-010-018

#### FR-010-08 : Raccordement au service d'IA

Le système interroge le service de complétion de conversation d'Azure OpenAI ; l'adresse du service, le nom du déploiement, la version d'API et la clé sont des paramètres de configuration de déploiement (priorité : variable d'environnement, puis paramètre système, puis valeur par défaut). Chaque appel applique un délai de connexion de 15 secondes et un délai de lecture de 30 secondes. Le port, l'adaptateur simulé actif par défaut, le coupe-circuit et le cache relèvent de la NFR transverse NFR-INT.

Source : RG-010-013, RG-010-019

#### FR-010-09 : Indisponibilité du service d'IA (correction BUG-038)

Les erreurs du service d'IA s'affichent dans une bulle de Sophie, dans la conversation, et non dans une zone d'erreur distincte :
- si le service rejette l'authentification de l'application, le système affiche « Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence. », sans code technique (remplace, pour ce cas, le comportement legacy de RG-010-020, qui exposait « (HTTP 401) ») ; [ASSUMPTION A-010-1 : « erreur d'authentification » couvre les statuts 401 et 403, et réutilise le message de l'exception technique]
- pour tout autre statut de réponse en erreur, le système affiche « Je suis momentanement indisponible (HTTP {status}). Veuillez reessayer. », {status} étant le code reçu ;
- si l'appel échoue sans réponse (délai dépassé, réseau, réponse illisible), le système affiche « Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence. ».

Le cas se produit aussi bien après un message saisi qu'après une action rapide.

Critère d'acceptation (test) : avec un service d'IA simulé qui répond par un refus d'authentification (401), l'envoi de « Quel est mon solde courant ? » affiche ce message dans la conversation, suivi d'une bulle de Sophie contenant « Je suis momentanement indisponible » et ne contenant ni « HTTP 401 » ni « 401 ».

Source : BUG-038, RG-010-007, RG-010-020, RG-010-021

#### FR-010-10 : Nouvelle conversation

Un clic sur « Nouvelle conversation » efface tous les messages et réinitialise la conversation avec un nouveau message de bienvenue personnalisé (FR-010-02) ; les actions rapides réapparaissent (FR-010-04).

Source : RG-010-022

#### FR-010-11 : Comportement de la zone de saisie

Pendant le traitement d'un envoi, le bouton « Envoyer » est désactivé et affiche « ... » ; il est rétabli à la fin du traitement. La touche Entrée sans la touche Maj envoie le message ; Maj+Entrée insère un saut de ligne. À la réception d'une réponse de Sophie, la liste des messages défile pour afficher le dernier message.

Source : RG-010-023, RG-010-024, RG-010-025

**Feature-specific NFRs :** chaque appel au service d'IA applique un délai de connexion de 15 secondes et un délai de lecture de 30 secondes (FR-010-08) ; le reste (port, simulation, coupe-circuit, cache) relève de NFR-INT.

**Notes :**
- RG-010-001 : couverte par l'exigence fusionnée de FEAT-008 (règles voisines).
- RG-010-010 (Suspect, dette legacy) : non reprise — la règle décrit un vidage du champ après envoi que le legacy ne fait pas, et le bug associé BUG-037 (champ de saisie non vidé après envoi) est classé « ne pas corriger ». [ASSUMPTION A-010-2 : le champ de saisie n'est pas vidé automatiquement après l'envoi, comme dans le legacy]
- SC-010-02 : le scénario attend « Je suis momentanement indisponible » ; il reste vrai après la correction BUG-038 (FR-010-09), à condition que le rejeu utilise un service d'IA simulé qui renvoie un refus d'authentification, et non l'adaptateur simulé nominal (NFR-INT), qui renverrait une réponse normale.
- Le bandeau technique du legacy (nom du modèle, de la plateforme et du composant serveur) n'est porté par aucune règle et n'est pas repris.
