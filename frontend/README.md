# Front de next-banking-webapp

Application React + TypeScript de la banque en ligne : écrans du client, du conseiller et de l'administrateur.
Elle ne parle qu'aux API des services (`/api/{identifiant}/**`), jamais à une base, et garde le jeton de
session dans `localStorage` (clé `nb.jeton`).

Architecture hexagonale (`docs/quality-gates.md`) :

- `src/domain` : règles pures (TypeScript sans paquet npm) ;
- `src/application` : cas d'usage et ports (TypeScript sans paquet npm) ;
- `src/adapters` : React, routeur, HTTP, stockage, composants d'interface (`src/adapters/ui/`) ;
- `src/infrastructure` : assemblage (`main.tsx`).

## Commandes

| Commande | Rôle |
|---|---|
| `npm ci` | installe les dépendances verrouillées |
| `npm run dev` | serveur de développement Vite, API renvoyées vers les services locaux |
| `npm run tokens` | régénère `src/adapters/ui/theme/tokens.css` depuis `DESIGN.md` (ne jamais l'éditer à la main) |
| `npx vitest run` | tests unitaires et DOM (`-t 'FR-030-04'` pour lancer les tests d'une exigence) |
| `npx vitest run --coverage` | tests et seuils de couverture par couche |
| `npx tsc --noEmit` | vérification des types |
| `npx depcruise --config .dependency-cruiser.cjs src` | sens des dépendances de l'hexagone |

Avant de déclarer un travail terminé : `scripts/gates/build.sh` à la racine du dépôt.

## Conventions

- `data-testid` au format `{écran}-{élément}[-{id}]` sur tout élément interactif, écrits en littéraux (jamais
  composés) ; les valeurs et routes existantes sont figées (golden master). La navigation utilise le préfixe
  `nav-`. `scripts/golden-master/verifier-scenario.sh SC-NNN-MM` vérifie qu'un scénario du golden master trouve
  sa route, ses `data-testid` et ses textes.
- Textes : repris à l'identique du legacy (`docs/retro/00-global/MESSAGES.md`, casse et absence d'accents
  comprises) ou de `textes-nouveaux.md`, rangés dans `src/adapters/textes.ts` ; aucun texte inventé.
- Styles : couleurs, polices et espacements uniquement par les variables `var(--…)` de `tokens.css` ; les
  éléments natifs (`a`, `button`, `input`…) ne sont stylés que dans leur composant de `src/adapters/ui/`.
- Temps : ni `Date.now()` ni `new Date()` dans `src/domain` et `src/application`.
- Un seul fichier de configuration Vite et Vitest : `vite.config.ts`.

## Motifs interdits (UX-DR110)

Motifs écartés, à ne jamais introduire ; `src/motifs-interdits.test.ts` contrôle ce qui peut l'être dans le code.

1. Le défilement infini : les listes se paginent ou s'affichent en entier.
2. Le glisser-déposer : toute action passe par un bouton ou un lien.
3. Une action visible au seul survol : ce qui s'atteint à la souris s'atteint aussi au clavier et au toucher.
4. Jamais plus d'une modale à la fois.
5. Jamais plus de deux niveaux de navigation : en-tête, puis sous-en-tête des espaces réservés.
6. Un contenu interactif dans un tooltip.
