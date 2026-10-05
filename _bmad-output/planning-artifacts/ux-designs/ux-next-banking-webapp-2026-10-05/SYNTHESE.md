# Synthèse de la séance UX : next-banking-webapp

*Séance `bmad-ux` du 2026-10-05, en mode Fast path (démo mercredi). Version finale.*

## 1. Ce que vous avez fourni et demandé

| Quoi | Détail | Où c'est repris |
|---|---|---|
| Le design system | `inputs/DESIGN.md` (« Purple Consulting ») : tokens et 40 composants | Copié dans `imports/`. `DESIGN.md` en reprend les tokens tels quels, par programme. |
| Un niveau d'accessibilité | « Répondre à une norme si possible, sans prendre de temps » ; puis « ce que coûterait le niveau réglementaire au-dessus » | §3 : plancher WCAG 2.1 AA, et un tableau des cadres et des coûts |
| Le délai | Démo mercredi | Fast path : choix marqués `[ASSUMPTION]`, outils créatifs et relecteurs sautés (§7.5) |
| **Wording intouchable** | « Le golden master intervient à la fin, donc ne pas toucher au wording » | Règle en tête de « Voice and Tone » dans `EXPERIENCE.md`. Aucun texte du legacy ne change, sauf les écarts déjà décidés dans le PRD. |
| Les textes des écrans nouveaux | Option (i) : un seul lot à valider, vérifié contre le golden master, avec des clés stables | `textes-nouveaux.md`, validé avec 5 corrections |
| Correction 1 : orthographe | Textes nouveaux en orthographe correcte, textes du legacy à l'identique | Appliquée, y compris pour l'avis de suspension. Aucun scénario n'est touché (vérifié). |
| Correction 2 : B6 | Libération sur solde devenu insuffisant : refusée, virement rejeté sans débit | Décision inscrite au PRD (FR-016-19, FR-027-16) |
| Correction 3 : cohérence de la libération | Ne pas annoncer « débité » à la libération | « Accepté - pas encore débité ». Le débit passe ensuite par le traitement asynchrone. |
| Correction 4 : chiffres | Expliquer 28 / 26 / 31 | Corrigé : 33 capturés = 28 d'écran + 5 HTTP. Moins 2 exclus, **31 applicables = 26 d'écran + 5 HTTP**. |
| Correction 5 : BUG-009 | Textes PENDING et ALERTE du legacy gardés pour la démo ; reformulation en story après la démo | PRD : FR-003-29 (story suivante), risque R-4, BUG-009 conservé provisoirement |
| Lignes « touché » | Option (2) : une seule entrée groupée dans la correspondance des textes à G2 | Écrite dans `EXPERIENCE.md` |
| Divergences du golden master | La liste, avant le commit | `divergences-golden-master.md` : **un seul scénario, SC-003-04, sous condition**. C'est une prévision, à confirmer au premier rejeu. |
| B7 (extension de B6) | Solde devenu insuffisant entre la libération et le débit : rejeté sans débit, transition LIBÉRÉ → REJETÉ, motif d'audit distinct, état visible sans notification, motif visible par l'administrateur | Décision inscrite au PRD et dans l'UX |

Contraintes héritées du PRD : parité avec le legacy, attributs de test stables (NFR-UI-02), interface en français, front React et TypeScript hexagonal.

## 2. Ce qui a été fait

| Étape | État |
|---|---|
| Espace de travail, journal des décisions, import du design system | fait |
| `DESIGN.md` : vos tokens + extensions bancaires, contrastes AA calculés, montants au format du legacy (« 10,548.72 EUR », tiret « - »), emoji des notifications conservés | fait, **final** |
| `EXPERIENCE.md` : 17 écrans, 27 correspondances de composants, attributs de test `{écran}-{élément}[-{id}]`, états (dont les 5 d'un virement), accessibilité, parcours UJ-1 à UJ-5, écrans nouveaux, écarts de texte (B1 retirée, reportée après la démo) | fait, **final** |
| `textes-nouveaux.md` : 53 textes avec clés (37 nouveaux, 16 repris), plus la clé du motif B7 | fait, validé |
| `divergences-golden-master.md` : les 31 scénarios applicables examinés un par un | fait. Prévision : 1 divergent sous condition, 30 non divergents. |
| Mise en cohérence du PRD (B6, B7, BUG-009, NFR-PAR-01, A.7) | fait, statut `final` conservé, journalisé |
| Relecteurs UX, maquettes HTML, mise en forme | **sautés** (Fast path), notés dans le journal, à reprendre après la démo |

## 3. Accessibilité

### 3.1 Ce qui est retenu : un plancher WCAG 2.1 niveau AA, sans audit

- **Visuel, déjà couvert par votre design system** :
  - contraste de 4,5:1 pour le texte et de 3:1 pour les composants et les bordures de champs ;
  - anneau de focus de 2 px `primary` ;
  - taille de texte minimale de 12 px ;
  - cibles de 48 px.

  Les extensions bancaires ajoutées ont été vérifiées : de 4,5:1 à 21:1.
- **Comportement, ajouté dans `EXPERIENCE.md`** :
  - tout se fait au clavier, dans un ordre logique ;
  - focus visible (`:focus-visible`) ;
  - libellés reliés aux champs ;
  - champ en erreur signalé par `aria-invalid` et `aria-describedby` ;
  - messages d'erreur et de succès annoncés aux lecteurs d'écran ;
  - tableaux avec de vrais en-têtes ;
  - information jamais portée par la seule couleur (un débit a un signe, un état a un libellé) ;
  - `prefers-reduced-motion` respecté.
- **Coût** : presque nul, si on l'applique dès la première story. Ce sont des règles de construction, pas des écrans en plus.
- **Ce qu'on ne fait pas** : ni audit, ni déclaration d'accessibilité, ni tests avec des utilisateurs de technologies d'assistance.

### 3.2 Le niveau réglementaire au-dessus, et ce qu'il coûterait

*Faits réglementaires à faire confirmer par votre juridique. Les coûts sont des ordres de grandeur à faire valider par un auditeur, pas des devis.*

| Cadre | Ce qu'il impose | Concerne-t-il ce projet ? |
|---|---|---|
| **WCAG 2.2 AA** (W3C, 2023) | WCAG 2.1 AA plus quelques critères : focus non masqué, cibles d'au moins 24 px, alternative au glisser-déposer, aide cohérente, pas de ressaisie inutile, authentification sans test cognitif | Recommandation, pas une loi. C'est la cible naturelle d'un projet neuf. |
| **Acte européen sur l'accessibilité** (directive (UE) 2019/882, transposée en France) | Applicable depuis le **28 juin 2025** aux **services bancaires aux consommateurs**. Conformité présumée à la norme **EN 301 549** (qui reprend WCAG 2.1 AA pour le web), déclaration et information d'accessibilité, contrôle par une autorité de marché | **Oui, dès la mise en production** auprès de vrais clients. Pas pour le démonstrateur. |
| **RGAA 4.1** (référentiel français) | Audit sur un échantillon de pages, déclaration d'accessibilité publiée, schéma pluriannuel et plan d'action annuel | Organismes publics, et entreprises privées au-dessus d'un seuil de chiffre d'affaires (250 M€). Une banque de cette taille est concernée. **À vérifier.** |

**Ce que coûterait le passage du plancher actuel à une conformité EN 301 549 / RGAA :**

| Poste | Ordre de grandeur | Remarque |
|---|---|---|
| Passer à WCAG 2.2 AA dans la spec | Moins d'une journée | Quelques règles en plus dans `EXPERIENCE.md`. Votre design system couvre déjà la taille des cibles. |
| Développement accessible dès le départ | Environ 5 à 15 % d'effort front en plus | Le même travail fait après coup coûte souvent plusieurs fois plus. |
| Contrôle automatique (axe-core dans les tests de bout en bout) | 1 à 2 jours de mise en place | Il ne détecte qu'une partie des défauts (environ un tiers). Il peut devenir un contrôle de plus dans la boucle, mais il n'est pas dans `docs/quality-gates.md` aujourd'hui. |
| Audit de conformité par un auditeur (RGAA ou EN 301 549) | De 5 à 15 jours d'audit selon l'échantillon de pages (environ 20 écrans ici), plus un contre-audit après corrections | C'est le seul moyen de déclarer un taux de conformité. |
| Corrections après l'audit | Variable : faible si le plancher est respecté depuis le début | — |
| Déclaration d'accessibilité, schéma pluriannuel, plan annuel, contact pour les signalements | Quelques jours, puis un suivi annuel | Obligations documentaires, pas techniques. |
| Tests avec des utilisateurs de lecteurs d'écran | Quelques jours | Recommandé, souvent exigé par les équipes accessibilité des banques. |

**Recommandation :**
- garder le plancher WCAG 2.1 AA pour la démo ;
- noter WCAG 2.2 AA comme cible du produit, pour un coût minime ;
- prévoir l'audit EN 301 549 / RGAA seulement si le projet sort du démonstrateur.

## 4. Ce que j'attends de vous

- **Pour la démo : rien de plus.**
- **La marque** : votre design system se déclare « interprétation non officielle » d'une marque. Si la démo se fait sous cette marque, il faut l'accord de l'équipe marque, et ni logo ni wordmark recréé.
- **Facultatif** : une maquette HTML de la file de revue ou de l'avis de suspension, si elle vous sert pour mercredi.
- **À l'étape G2** (pendant `bmad-architecture`) : valider l'exception de SC-003-04 et l'entrée groupée des ajouts de texte avec `corpus-bmad-golden-master`.

## 5. Le corpus du legacy : ce qui est utilisé, et ce qui vaut le coup d'œil

C'est moi qui lis le corpus, par des sous-agents qui en extraient l'utile. Vous n'avez pas à le lire. Si vous voulez regarder vous-même, voici les fichiers les plus utiles pour l'interface :

| Fichier | Pourquoi il compte |
|---|---|
| `docs/retro/00-global/UI-KIT.md` | La charte et les composants du legacy. C'est la base de la correspondance avec votre design system. |
| `docs/retro/screens/navigation-flow.md` | La carte des écrans et de la navigation, par rôle. C'est l'architecture d'information de départ. |
| `docs/retro/00-global/MESSAGES.md` | **Le catalogue exact des textes affichés.** C'est ce que le golden master compare : tout écart de texte se juge contre ce fichier. |
| `docs/retro/features/FEAT-NNN/FIELDS.md` | Les champs de chaque écran (libellé, type, obligatoire, format). |
| `docs/retro/features/FEAT-NNN/STATES.md` | Les états de chaque écran : vide, chargé, erreur, confirmation. |
| `docs/retro/features/FEAT-NNN/FLOW.md` | Les parcours et les transitions entre écrans. |
| `docs/retro/features/FEAT-NNN/screens/` | Les captures d'écran du legacy, utiles pour comparer visuellement. |
| `docs/retro/golden-master/scenarios/` et `docs/retro/golden-master-contract.md` | Ce que le rejeu final vérifiera : les écrans, sélecteurs et textes à préserver. Ils serviront à l'étape G2. |

Les plus importants pour vous, si vous ne regardez que trois choses : **MESSAGES.md** (les textes), **navigation-flow.md** (les écrans) et **les captures de `screens/`**, pour visualiser le point de départ.

## 6. Artefacts produits

Dossier : `_bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/`

| Fichier | Rôle | État |
|---|---|---|
| `DESIGN.md` | L'apparence : tokens et composants (votre design system + extensions bancaires). Format Google Labs. | final |
| `EXPERIENCE.md` | Le fonctionnement : écrans, composants, états, interactions, accessibilité, parcours, écarts de texte | final |
| `textes-nouveaux.md` | Les textes des écrans nouveaux, avec leurs clés stables et leur vérification contre le golden master | validé |
| `divergences-golden-master.md` | La prévision des scénarios qui divergeront au rejeu final, avec raison et traitement à l'étape G2 | prévision validée |
| `imports/design-system-purple-consulting.md` | Copie de votre design system, la source | — |
| `.memlog.md` | Le journal des décisions de la séance | complet |
| `SYNTHESE.md` | Ce document | final |

**Aussi modifiés pendant la séance :** le PRD (`prds/prd-next-banking-webapp-2026-10-05/`, décisions B6, B7, BUG-009, NFR-PAR-01) et le journal des constats du pilote (`_bmad-output/pilote-constats.md`).

**Qui utilisera ces fichiers ensuite :**
- `bmad-architecture` : le thème (tokens), la convention des attributs de test, le placement des sections nouvelles (fenêtre de 5 000 caractères du rejeu) ;
- l'étape G2 : les écarts de texte, l'entrée groupée des ajouts et l'exception de SC-003-04 ;
- les stories : la correspondance des composants, les états, les textes et leurs clés.

## 7. Comment je travaille l'UX/UI (coulisses de la séance)

### 7.1 Ce que produit `bmad-ux` : deux contrats, pas des maquettes

`bmad-ux` ne dessine pas d'écrans. Il écrit deux documents qui servent de **contrat** aux agents de développement. En cas de conflit avec une maquette, ce sont eux qui font foi.

| Fichier | Il répond à | Format | Qui le lit ensuite |
|---|---|---|---|
| `DESIGN.md` | **À quoi ça ressemble** : couleurs, typographie, espacements, angles, composants visuels | Spec Google Labs `design.md` : tokens en YAML (`colors`, `typography`, `spacing`, `components`…) + texte en 8 sections dans un ordre imposé | `bmad-architecture` (thème du front), les agents de dev (composants), l'outil `design.md export` (génération des variables CSS) |
| `EXPERIENCE.md` | **Comment ça marche** : écrans, navigation, comportements, états, accessibilité, parcours | Sections imposées par BMAD. `EXPERIENCE.md` désigne les tokens de `DESIGN.md` par leur nom (`{components.alert-warning}`) sans jamais les recopier | Les epics et stories (une story par écran ou par parcours), les tests (attributs de test, états), l'étape G2 du golden master (écarts de texte) |

Ce découpage a un intérêt : on peut changer l'apparence (`DESIGN.md`) sans toucher au fonctionnement, et inversement.

### 7.2 Comment votre design system est entré dans le projet

1. **Import sans retouche** : votre fichier est copié dans `imports/`. C'est la source de référence.
2. **Reprise des tokens par extraction automatique** : le sous-agent a recopié vos couleurs, typographies, espacements et 40 composants **par programme**, sans les retaper. Il ne peut donc pas les avoir déformés. Il a seulement corrigé deux références cassées de votre fichier (`{header}` en `{components.header}`).
3. **Des extensions bancaires construites uniquement avec vos tokens** : badges des 5 états d'un virement, montants débit et crédit, notifications, barre de navigation applicative. **Aucune couleur nouvelle** : un badge « suspendu », c'est votre `warning` sur votre `warning-container`.
4. **Les contrastes calculés, pas estimés** : pour chaque couple texte et fond ajouté, le ratio WCAG est calculé par formule (de 4,5:1 à 21:1), et noté dans le token.
5. **Une vérification technique** : le YAML est relu par un parseur, et chaque référence `{…}` mène bien à un token existant.

### 7.3 Comment les écrans sont reconstruits depuis le legacy

Je ne pars pas d'une page blanche. Chaque partie d'`EXPERIENCE.md` vient d'un fichier précis du corpus :

| Partie d'`EXPERIENCE.md` | Source dans le corpus | Ce que j'en tire |
|---|---|---|
| Architecture d'information (17 écrans, navigation) | `screens/navigation-flow.md` + droits par rôle du PRD | La liste des écrans par rôle, et une table écran legacy → écran cible |
| Correspondance des composants (27) | `00-global/UI-KIT.md` | Chaque composant du legacy (CMP-001 à CMP-011) → un composant de votre design system |
| Ton et textes | `00-global/MESSAGES.md` + FR du PRD | Les textes exacts à conserver, et les seuls écarts permis |
| États des écrans | `features/FEAT-NNN/STATES.md` | Vide, chargement, erreur, succès, plus les 5 états d'un virement |
| Champs et attributs de test | `features/FEAT-NNN/FIELDS.md` + sélecteurs du golden master | La convention `{écran}-{élément}[-{id}]`, et une table sélecteur du legacy → attribut de test |
| Parcours | UJ-1 à UJ-5 du PRD | Étapes numérotées et moment clé |
| Écrans nouveaux | Décisions RG-027-003 (D1 à D12) et B1 à B7 | Avis de suspension, écran de refus, file de revue, états d'un virement |

### 7.4 Le golden master, la contrainte qui guide tout

C'est **la** contrainte de l'interface. Ma démarche :
- **Comprendre ce qu'il compare.** J'ai lu les fichiers des scénarios et leur configuration. Ils comparent **le texte affiché** en mode « contient », jamais le style. D'où trois conséquences :
  - **le style est libre** : votre design system ne casse rien ;
  - **un texte ajouté ne casse rien** ;
  - **un texte du legacy modifié ou retiré casse le scénario** qui l'attend.
- **Protéger le texte** : votre consigne « wording intouchable » est devenue une règle écrite en tête de `EXPERIENCE.md`. J'ai corrigé tout ce qui touchait au texte sans le dire : le signe « − » au lieu de « - », les emoji remplacés par des icônes, le format des montants.
- **Rendre chaque écart traçable** : la liste des écarts de texte (texte legacy → nouveau texte → exigence qui le justifie) alimente directement la correspondance des textes à l'étape G2.
- **Donner une clé stable à chaque texte nouveau** (`virement.etat.suspendu`…), pour que le libellé puisse changer plus tard sans casser les tests.
- **Calculer les divergences scénario par scénario**, avant le commit, plutôt que de les découvrir au rejeu final : `divergences-golden-master.md`.

### 7.5 Ce que le Fast path a sauté, et ce qu'on peut reprendre

`bmad-ux` propose normalement des outils créatifs. Vu le délai, je ne les ai pas utilisés :

| Outil sauté | Ce qu'il apporte | À reprendre ? |
|---|---|---|
| Thèmes de couleurs et directions de design (pages HTML à comparer) | Choisir une identité visuelle | **Inutile** : votre design system la fixe |
| Wireframes Excalidraw | Valider la disposition d'un écran | Utile pour la file de revue de l'administrateur |
| Maquettes HTML des écrans clés | Une référence visuelle pour les agents de dev, et un support de démo | **Recommandé** pour 2 écrans : la file de revue et l'avis de suspension |
| Relecteurs (rubrique UX, accessibilité) | Une critique indépendante des deux documents | Après la démo |
| Export vers Google Stitch | Génération d'écrans par un outil externe | Non nécessaire |

### 7.6 Mes limites sur l'UI

- **Je ne vois pas le rendu.** Aucun écran n'existe encore. Mes vérifications sont des calculs (contrastes), des relectures et des recherches dans les fichiers, pas des captures. Une maquette HTML est le seul moyen de **voir** avant le développement.
- **Les textes nouveaux sont des propositions.** Je les écris selon vos règles (neutres, avec ce qui a été débité, ce qu'il faut faire), mais leur ton final vous appartient. C'est pourquoi ils sont marqués « à valider », avec une clé.
- **Le plancher d'accessibilité reste déclaratif** tant qu'aucun contrôle automatique (axe-core) ni audit ne le vérifie sur l'application réelle (§3.2).

### 7.7 En deux mots, sur la mécanique générale

Je pilote, et des sous-agents lisent le corpus en profondeur et rédigent. Je ne garde que leurs conclusions, et je **revérifie** leurs résultats : ils se trompent, et les erreurs listées en §7.4 viennent d'eux. Chaque décision est journalisée dans `.memlog.md`. Tout ce que je choisis sans vous est marqué `[ASSUMPTION]` ; ce que vous tranchez devient une décision datée.
