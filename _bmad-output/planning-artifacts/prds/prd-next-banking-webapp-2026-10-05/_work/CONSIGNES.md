# Consignes de rédaction des sections « fonctionnalité » du PRD

Tu rédiges, en français, une ou plusieurs sections de la partie 4 (Fonctionnalités) du PRD de rénovation de `next-banking-webapp`. Racine du projet : `/home/sebastienrozange/workspace/acn-fs/next-banking-webapp`. Le corpus de rétro-ingénierie est dans `docs/retro/`. Il décrit le comportement de l'application existante (le « legacy »), qui fait foi, sauf pour les bugs à corriger.

## Entrées, par fonctionnalité FEAT-NNN

- `_bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/_work/FEAT-NNN.json` : la liste des règles (`rg`, `status` Confirmé / À confirmer / Suspect, `nature` null / dette-legacy / métier, `nature_detail`), les bugs à corriger (`bugs_corriger`, avec le comportement attendu) et les bugs conservés (`bugs_ne_pas_corriger`).
- `docs/retro/features/FEAT-NNN/RG.md` : le texte des règles. **Lis-le en entier.**
- Si besoin : `VALIDATION.md`, `MESSAGES.md`, `FLOW.md` et `STATES.md` du même dossier, et `docs/retro/00-global/LEGACY-BUGS.md`.
- Scénarios du golden master : `ls docs/retro/golden-master/scenarios/FEAT-NNN/`, si le dossier existe.

## Sortie

Écris un fichier par fonctionnalité : `_bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/_draft/FEAT-NNN.md`. Format exact :

```markdown
### FEAT-NNN — <nom de la fonctionnalité>

**Description :** <3 à 6 phrases : qui l'utilise (rôles CLIENT, CONSEILLER, ADMIN), ce qu'elle fait, ses états ou cas limites marquants.> Scénarios golden master : SC-NNN-NN, … (ou « aucun »).

**Exigences fonctionnelles :**

#### FR-NNN-01 : <nom court de la capacité>

<Exigence en une à trois phrases, en français, testable. Elle reprend les seuils, conditions et messages affichés exacts du corpus (messages entre guillemets, à l'identique).>

Source : RG-NNN-001, RG-NNN-004

#### FR-NNN-02 : …

**Feature-specific NFRs :** <seulement si une contrainte propre à la fonctionnalité existe ; sinon, omets ce bloc.>

**Notes :**
- <Règles non reprises en exigence, bugs conservés, renvois vers une exigence fusionnée d'une autre fonctionnalité.>
```

- Les FR sont numérotées `FR-NNN-01`, `FR-NNN-02`, …, où NNN est le numéro de la fonctionnalité. Pas d'autre forme. Aucun titre de niveau `#` ou `##` dans ton fichier.
- La ligne `Source :` est obligatoire dans chaque FR. Elle cite tous les identifiants RG-NNN-NNN (et BUG-NNN) couverts, écrits en entier. Une plage « RG-003-010 à RG-003-014 » est acceptée.
- Ne mentionne aucune technologie (pas de JSF, de page `.xhtml`, d'EJB, de JMS, de nom de classe). Décris la capacité : « la page de connexion », « le tableau de bord ». Les règles d'accès par page deviennent des règles d'accès par écran ou par fonction.
- Le golden master compare des textes affichés : reprends tous les libellés et messages exacts que donne le corpus.

## Règles de couverture (vérifiées ensuite par un script : sois exhaustif)

1. **Chaque RG de la fonctionnalité apparaît dans ton fichier.** Contrôle-le toi-même à la fin, avec un script qui compare `_work/FEAT-NNN.json` à ton fichier.
2. **RG Confirmé ou À confirmer, nature null ou métier :** citée sur la ligne `Source :` d'au moins une FR. Les règles « À confirmer » sont reprises telles quelles, sans marqueur : une supposition globale du PRD couvre ce choix.
3. **Bug à corriger (`bugs_corriger`) :** une FR dédiée (ou une FR qui porte la règle concernée) cite le BUG-NNN et sa RG sur sa ligne `Source :`. Son texte énonce le comportement attendu, et une ligne `Critère d'acceptation (test) :` le reformule en test vérifiable. Le comportement legacy n'est pas reproduit.
4. **RG de nature dette-legacy :** si un bug à corriger la couvre, cite-la dans la FR du bug (« remplace le comportement legacy RG-… »). Sinon, ne crée pas de FR qui la reproduit : liste-la sous **Notes** (« RG-… (dette legacy) : non reproduite. »).
5. **RG Suspect :** ne crée jamais une FR qui ne cite que des RG Suspect. Deux cas :
   - le comportement est cohérent avec une RG Confirmé ou À confirmer, et en est le prolongement : intègre-le dans cette FR en citant les deux ;
   - sinon, liste-la sous **Notes** : « RG-… (Suspect) : non reprise — <raison courte>. [ASSUMPTION A-NNN-k : <ce qu'on suppose>] ».
6. **Bug conservé (`bugs_ne_pas_corriger`) :** ne cite **jamais** son identifiant BUG-NNN dans une FR. Tu peux le citer sous **Notes** uniquement. Si sa RG est Confirmé ou À confirmer, la RG reste citée dans une FR, qui décrit le comportement legacy conservé.

## Règles voisines entre fonctionnalités : fusions décidées par l'utilisateur

Une seule FR cite toutes les RG voisines. Elle est portée par la fonctionnalité de plus petit numéro (« porteuse »).
- **Si tu es porteuse :** lis les RG des autres fonctionnalités et rédige la FR fusionnée. **Ne lisse rien** : toute différence de valeur, de seuil, de condition ou d'effet reste visible dans la FR, et le choix fait est marqué `[ASSUMPTION A-NNN-k : …]`.
- **Si tu n'es pas porteuse :** ne crée pas de FR pour ces RG. Écris sous **Notes** : « RG-… : couverte par l'exigence fusionnée de FEAT-XXX (règles voisines). »

Liste des fusions :
- RG-001-010 + RG-030-010 (déconnexion) → porteuse FEAT-001
- RG-001-011 + RG-030-003 (non-ADMIN sur l'administration) → porteuse FEAT-001
- RG-003-020 + RG-023-007 (motif dans la notification de confirmation) → porteuse FEAT-003
- RG-003-025 + RG-003-026 + RG-027-005 (alerte LARGE_AMOUNT : 40 à partir de 5 000 €, 80 à partir de 15 000 €) → porteuse FEAT-003
- RG-003-030 + RG-027-009 (ROUND_AMOUNT) → porteuse FEAT-003
- RG-003-032 + RG-027-011 (RAPID_SUCCESSION) → porteuse FEAT-003
- RG-004-013 + RG-007-003 + RG-007-012 (« Montant invalide ») → porteuse FEAT-004
- RG-004-061 + RG-021-004 (solde insuffisant à l'exécution d'un virement programmé ; attention, BUG-014 change RG-004-061) → porteuse FEAT-004
- RG-008-001 + RG-010-001 + RG-015-001 + RG-016-001 (utilisateur non authentifié redirigé vers la connexion) → porteuse FEAT-008
- RG-010-002 + RG-011-001 + RG-013-002 (accès pour tout utilisateur authentifié) → porteuse FEAT-010
- RG-014-011 + RG-028-018 (20 transactions maximum par compte externe) → porteuse FEAT-014

## Règles qui se recouvrent

Il arrive que deux règles d'une même fonctionnalité, ou deux FR, puissent être vraies en même temps avec des effets opposés (par exemple, rejeter et exécuter). Rends alors leur lien explicite :
- soit une seule FR avec « sauf si … », qui cite les deux RG ;
- soit deux FR qui se renvoient l'une à l'autre.

Si l'ordre de priorité n'est pas écrit dans le corpus, marque ton choix `[ASSUMPTION A-NNN-k : …]`.

## Décision ouverte RG-027-003 (BUG-084) : contrôle de fraude d'un virement

- **Acquis :** le contrôle de fraude s'exécute **avant la validation finale** du virement, et toute erreur du contrôle est **journalisée**.
- **Non tranché, à ne jamais trancher, ni par une [ASSUMPTION], ni indirectement :**
  - (a) quand le contrôle détecte une suspicion, le virement est-il bloqué, ou seulement signalé ?
  - (b) quand le contrôle tombe en erreur, « ne pas se poursuivre silencieusement » veut-il dire virement refusé, ou virement exécuté avec alerte ?
- Toute FR dont le comportement dépend de (a) ou de (b) porte, à la fin de son titre, `[EN ATTENTE RG-027-003 (a)]`, `[EN ATTENTE RG-027-003 (b)]` ou `[EN ATTENTE RG-027-003 (a)(b)]`. Son texte énonce la partie acquise et renvoie à la question ouverte QO-1 du PRD pour le reste.
  - Exemples de dépendances : statut du virement après une alerte, notification FRAUDE à la place de CONFIRMATION, suspension au-delà du seuil de fraude (BUG-076), débit ou non du solde, traitement asynchrone du virement après le contrôle.
  - Ne marque pas ainsi une règle qui ne fait que calculer un signal (score, type d'alerte) sans décider du sort du virement.
- Les seuils de fraude, d'alerte et de mise en attente (10 000 €, 5 000 € et 5 000 € par défaut) sont une configuration de déploiement, avec les valeurs du code. C'est une décision prise : pas d'[ASSUMPTION] là-dessus. Cela ne dit rien de (a) ni de (b).

## Décisions déjà prises (à respecter, sans [ASSUMPTION])

- Démonstrateur réaliste, avec les exigences de sécurité d'une vraie banque. Les données de référence sont rechargées, sans migration.
- Connexion par identifiant et mot de passe gérés par l'application.
- Notifications uniquement dans l'application.
- Fournisseurs externes : Azure OpenAI (chat conseiller), Powens (open banking), AlphaVantage (bourse).
  - Chacun est derrière un port, avec un adaptateur simulé actif par défaut en local, dans les tests et dans les contrôles. Les vrais appels sont une option de configuration.
  - Délai d'attente, coupe-circuit et cache.
  - Dans les FR, renvoie à la NFR transverse « NFR-INT » au lieu de réécrire ces points.

## Suppositions

- Marque chaque supposition `[ASSUMPTION A-NNN-k : <texte court>]`, où NNN est le numéro de la fonctionnalité et k un compteur.
- N'en mets que là où tu infères vraiment quelque chose que le corpus ne dit pas.

## Compte rendu final (ta seule réponse au parent, court)

Pour chaque fonctionnalité :
- le nombre de FR, et le nombre de RG citées sur le nombre de RG du JSON (doit être égal) ;
- les bugs à corriger couverts ;
- la liste des [ASSUMPTION], avec leur identifiant, le texte et un niveau de risque (élevé / moyen / faible) ;
- les FR marquées EN ATTENTE RG-027-003 ;
- les contradictions entre règles rencontrées.

Ne recopie pas les sections dans ta réponse.
