---
version: beta
name: Purple Consulting (inspiré d'accenture.com)
description: >-
  Design system non officiel pour application métier, dérivé de l'identité
  visuelle publique d'accenture.com (violet #A100FF sur noir/blanc, typographie
  grasse, angles vifs). Non affilié à Accenture ni approuvé par elle : à valider
  avec l'équipe marque avant tout usage sous cette marque.
colors:
  primary: "#A100FF"
  on-primary: "#FFFFFF"
  primary-hover: "#7500C0"
  primary-active: "#4E007F"
  primary-container: "#F3E6FF"
  on-primary-container: "#4E007F"
  accent-tint: "#D2A5FF"
  ink: "#000000"
  surface: "#FFFFFF"
  surface-alt: "#F5F4F7"
  surface-inverse: "#000000"
  on-surface-inverse: "#FFFFFF"
  text-secondary: "#66646E"
  border: "#D9D7DE"
  border-strong: "#66646E"
  disabled-bg: "#ECEBEF"
  disabled-text: "#66646E"
  success: "#0B7A3E"
  success-container: "#E5F4EC"
  warning: "#A35200"
  warning-container: "#FDF0E3"
  error: "#C4122F"
  error-container: "#FCE8EB"
  info: "#7500C0"
  info-container: "#F3E6FF"
typography:
  display:
    fontFamily: Graphik, Inter, Arial, sans-serif
    fontSize: 3.5rem
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: -0.02em
  h1:
    fontFamily: Graphik, Inter, Arial, sans-serif
    fontSize: 2.5rem
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: -0.015em
  h2:
    fontFamily: Graphik, Inter, Arial, sans-serif
    fontSize: 2rem
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: -0.01em
  h3:
    fontFamily: Graphik, Inter, Arial, sans-serif
    fontSize: 1.5rem
    fontWeight: 600
    lineHeight: 1.2
  h4:
    fontFamily: Graphik, Inter, Arial, sans-serif
    fontSize: 1.25rem
    fontWeight: 600
    lineHeight: 1.3
  body-lg:
    fontFamily: Graphik, Inter, Arial, sans-serif
    fontSize: 1.125rem
    fontWeight: 400
    lineHeight: 1.6
  body-md:
    fontFamily: Graphik, Inter, Arial, sans-serif
    fontSize: 1rem
    fontWeight: 400
    lineHeight: 1.5
  body-sm:
    fontFamily: Graphik, Inter, Arial, sans-serif
    fontSize: 0.875rem
    fontWeight: 400
    lineHeight: 1.45
  label:
    fontFamily: Graphik, Inter, Arial, sans-serif
    fontSize: 0.875rem
    fontWeight: 600
    lineHeight: 1.3
  button:
    fontFamily: Graphik, Inter, Arial, sans-serif
    fontSize: 1rem
    fontWeight: 600
    lineHeight: 1.25
  eyebrow:
    fontFamily: Graphik, Inter, Arial, sans-serif
    fontSize: 0.75rem
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: 0.08em
  caption:
    fontFamily: Graphik, Inter, Arial, sans-serif
    fontSize: 0.75rem
    fontWeight: 400
    lineHeight: 1.4
rounded:
  none: 0px
  sm: 2px
  md: 4px
  pill: 999px
spacing:
  xxs: 2px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  2xl: 48px
  3xl: 64px
  4xl: 96px
breakpoints:
  sm: 640px
  md: 768px
  lg: 1024px
  xl: 1280px
motion:
  duration-fast: 120ms
  duration-base: 200ms
  duration-slow: 350ms
  easing-standard: cubic-bezier(0.2, 0, 0, 1)
  easing-enter: cubic-bezier(0, 0, 0.2, 1)
  easing-exit: cubic-bezier(0.4, 0, 1, 1)
z-index:
  header: 100
  sub-header: 90
  popover: 200
  modal-backdrop: 300
  modal: 310
  toast: 400
components:
  # ── Boutons ────────────────────────────────────────────────────────────────
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: 12px 24px
    height: 48px
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
    textColor: "{colors.on-primary}"
  button-primary-active:
    backgroundColor: "{colors.primary-active}"
    textColor: "{colors.on-primary}"
  button-secondary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.on-surface-inverse}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: 12px 24px
    height: 48px
  button-ghost:
    backgroundColor: transparent
    textColor: "{colors.ink}"
    borderColor: "{colors.ink}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: 12px 24px
    height: 48px
  button-ghost-inverse:
    backgroundColor: transparent
    textColor: "{colors.on-surface-inverse}"
    borderColor: "{colors.on-surface-inverse}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: 12px 24px
    height: 48px
  button-icon:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.none}"
    size: 48px
    note: >-
      Carré 48×48px, icône centrée (24×24px). Utilisé pour des actions
      contextuelles (ex. : « + » sur une carte liste). Variante compacte : 40×40px.
  button-disabled:
    backgroundColor: "{colors.disabled-bg}"
    textColor: "{colors.disabled-text}"
    borderColor: transparent
  # ── Lien ───────────────────────────────────────────────────────────────────
  link:
    textColor: "{colors.primary-hover}"
    typography: "{typography.body-md}"
    textDecoration: none
    textDecoration-hover: underline
  # ── Champs de formulaire ───────────────────────────────────────────────────
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.body-md}"
    rounded: "{rounded.none}"
    padding: 12px 16px
    height: 48px
    borderColor: "{colors.border-strong}"
    borderWidth: 1px
  input-helper:
    textColor: "{colors.text-secondary}"
    typography: "{typography.body-sm}"
  input-error:
    backgroundColor: "{colors.error-container}"
    textColor: "{colors.error}"
    borderColor: "{colors.error}"
  input-search-hero:
    backgroundColor: transparent
    textColor: "{colors.ink}"
    typography: "{typography.h2}"
    borderBottom: "2px solid {colors.ink}"
    borderOther: none
    textAlign: center
    note: >-
      Variante grand format pour les pages de recherche / états vides.
      Fond transparent, une seule bordure basse, texte centré en h2.
  checkbox:
    size: 20px
    rounded: "{rounded.sm}"
    borderColor: "{colors.border-strong}"
    borderWidth: 2px
    checked-backgroundColor: "{colors.primary}"
    checked-borderColor: "{colors.primary}"
    checked-iconColor: "{colors.on-primary}"
    label-typography: "{typography.body-md}"
    label-gap: 10px
    note: >-
      Case à cocher custom. L'état checked affiche un ✓ blanc sur fond violet.
      Peut être accompagné d'un compteur `caption` en `text-secondary` aligné à droite du label.
  select:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.body-md}"
    rounded: "{rounded.none}"
    padding: 12px 40px 12px 16px
    height: 48px
    borderColor: "{colors.border-strong}"
    borderWidth: 1px
    icon: chevron-down
    icon-color: "{colors.text-secondary}"
  # ── Cartes ─────────────────────────────────────────────────────────────────
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.none}"
    padding: 24px
    borderColor: "{colors.border}"
    borderWidth: 1px
  card-muted:
    backgroundColor: "{colors.surface-alt}"
    textColor: "{colors.ink}"
    padding: 24px
    borderColor: none
  card-inverse:
    backgroundColor: "{colors.surface-inverse}"
    textColor: "{colors.on-surface-inverse}"
    padding: 32px
  card-result:
    backgroundColor: "{colors.surface-inverse}"
    textColor: "{colors.on-surface-inverse}"
    padding: 20px 24px
    borderBottom: "1px solid rgba(255,255,255,0.1)"
    note: >-
      Carte de liste de résultats (ex. : offres d'emploi, projets).
      Titre en `h4` blanc, métadonnées en `body-sm` `text-secondary` séparées par « | »,
      bouton icon `button-icon` aligné à droite. Hover : fond légèrement éclairci.
  # ── Navigation ─────────────────────────────────────────────────────────────
  header:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    height: 72px
    z-index: "{z-index.header}"
    note: >-
      Header global. Logo à gauche, nav centrée, action principale à droite.
      Élément actif : soulignement 2px `primary`.
  sub-header:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    height: 48px
    z-index: "{z-index.sub-header}"
    note: >-
      Barre de navigation contextuelle (ex. : section Carrières, Projets).
      Fond violet plein, texte blanc. Positionné sous le header global.
      Liens séparés par espacement, aucune bordure.
  tabs:
    backgroundColor: transparent
    textColor: "{colors.text-secondary}"
    typography: "{typography.label}"
    borderBottom: "1px solid {colors.border}"
    tab-active-textColor: "{colors.ink}"
    tab-active-borderColor: "{colors.primary}"
    tab-active-borderWidth: 2px
    tab-padding: "8px 0"
    tab-gap: 32px
    note: >-
      Onglets horizontaux. Indicateur actif : soulignement 2px `primary` sous l'onglet.
      Aucun fond sur les onglets, pas de boîte. Réservé aux vues avec 2 à 6 onglets.
  pagination:
    textColor: "{colors.text-secondary}"
    typography: "{typography.body-md}"
    active-textColor: "{colors.ink}"
    active-borderBottom: "2px solid {colors.primary}"
    arrow-color: "{colors.ink}"
    gap: 8px
    note: >-
      Pagination numérotée. Page active soulignée 2px violet. Flèches ← → aux extrémités.
      Ellipsis « … » pour les longues séquences. Alignement centré.
  filter-panel:
    backgroundColor: "{colors.surface}"
    width: 240px
    section-borderBottom: "1px solid {colors.border}"
    section-padding: "16px 0"
    label-typography: "{typography.label}"
    count-typography: "{typography.caption}"
    count-textColor: "{colors.text-secondary}"
    note: >-
      Panneau de filtres latéral (sidebar gauche). Chaque section est collapsible
      (icône + / −). Les options sont des `checkbox` avec un compteur de résultats
      à droite. Lien « Effacer » en `link` à côté du titre « Filtrer ».
  footer:
    backgroundColor: "{colors.surface-inverse}"
    textColor: "{colors.on-surface-inverse}"
    padding: 48px 24px
  # ── Feedback & statut ──────────────────────────────────────────────────────
  eyebrow:
    textColor: "{colors.primary-hover}"
    typography: "{typography.eyebrow}"
  tag:
    backgroundColor: "{colors.primary-container}"
    textColor: "{colors.on-primary-container}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: 4px 12px
  tag-accent:
    backgroundColor: "{colors.accent-tint}"
    textColor: "{colors.ink}"
    rounded: "{rounded.pill}"
    padding: 4px 12px
  alert-success:
    backgroundColor: "{colors.success-container}"
    textColor: "{colors.success}"
    borderLeft: "4px solid {colors.success}"
    padding: 16px
  alert-warning:
    backgroundColor: "{colors.warning-container}"
    textColor: "{colors.warning}"
    borderLeft: "4px solid {colors.warning}"
    padding: 16px
  alert-error:
    backgroundColor: "{colors.error-container}"
    textColor: "{colors.error}"
    borderLeft: "4px solid {colors.error}"
    padding: 16px
  alert-info:
    backgroundColor: "{colors.info-container}"
    textColor: "{colors.info}"
    borderLeft: "4px solid {colors.info}"
    padding: 16px
  tooltip:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.on-surface-inverse}"
    typography: "{typography.caption}"
    rounded: "{rounded.md}"
    padding: 6px 10px
    maxWidth: 240px
    shadow: "0 4px 16px rgba(0,0,0,0.12)"
    z-index: "{z-index.popover}"
    note: >-
      Apparaît sur :hover/:focus-visible. Disparaît en ≤ 120ms (motion.duration-fast).
      Jamais de contenu interactif à l'intérieur. Déclenché uniquement par une icône
      ou un libellé tronqué.
  # ── Données ────────────────────────────────────────────────────────────────
  table-header:
    backgroundColor: "{colors.surface-alt}"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    padding: 12px 16px
  table-cell:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.body-sm}"
    padding: 12px 16px
    borderBottom: "1px solid {colors.border}"
  table-row-hover:
    backgroundColor: "{colors.surface-alt}"
  table-row-selected:
    backgroundColor: "{colors.primary-container}"
  # ── Superposition ──────────────────────────────────────────────────────────
  modal:
    backgroundColor: "{colors.surface}"
    maxWidth: 560px
    rounded: "{rounded.none}"
    padding: 32px
    shadow: "0 12px 32px rgba(0,0,0,0.18)"
    backdrop: "rgba(0,0,0,0.5)"
    z-index: "{z-index.modal}"
  # ── Accessibilité ──────────────────────────────────────────────────────────
  focus-ring:
    outlineColor: "{colors.primary}"
    outlineWidth: 2px
    outlineOffset: 2px
  # ── États spéciaux ─────────────────────────────────────────────────────────
  empty-state:
    layout: centered
    maxWidth: 480px
    icon-size: 48px
    icon-color: "{colors.text-secondary}"
    title-typography: "{typography.h3}"
    body-typography: "{typography.body-md}"
    body-textColor: "{colors.text-secondary}"
    note: >-
      Centré verticalement et horizontalement dans son conteneur. Icône + titre h3 +
      texte descriptif + un seul bouton primaire (optionnel). Utilisé sur les listes
      vides, les erreurs 404, les états d'onboarding.
---

## Overview

Esthétique « consulting tech » : contraste maximal noir / blanc, **un seul accent violet électrique**, typographie grasse et serrée, angles vifs, beaucoup d'air. L'interface doit paraître affirmée, éditoriale et sûre d'elle, jamais décorative.

Ce fichier est une **interprétation non officielle** de l'identité publique visible sur accenture.com et dans la documentation publique de la marque (violet #A100FF adopté en 2020, noir, blanc). Il n'inclut ni logo, ni wordmark, ni le glyphe « > » de la marque, ni la police propriétaire. Si l'application est livrée sous la marque Accenture, remplacer ce fichier par les assets et tokens officiels fournis par l'équipe marque interne.

Cible : application métier à écrans denses (formulaires, tableaux, listes de résultats). Les grandes tailles (`display`, `h1`) sont réservées aux pages d'accueil, de recherche et aux états vides.

## Colors

- **Primary (#A100FF)** : l'unique couleur d'action. Boutons principaux, focus, sélection, indicateurs actifs, fond du `sub-header`. Contraste ≈ 5,3:1 avec le blanc (AA texte normal).
- **Primary hover (#7500C0)** / **active (#4E007F)** : états d'interaction ; #7500C0 sert aussi pour les liens et les eyebrows (meilleur contraste pour du texte petit, ≈ 8:1).
- **Primary container (#F3E6FF)** / **Accent tint (#D2A5FF)** : fonds de tags, lignes sélectionnées, séries secondaires de graphiques. Jamais pour du texte.
- **Ink (#000000)** : texte principal, bouton secondaire, sections inversées, fond des cartes résultats.
- **Surface (#FFFFFF)** / **Surface alt (#F5F4F7)** : fond de page / zones regroupées, en-têtes de tableaux.
- **Surface inverse (#000000)** : bandeaux forts (hero, footer, cartes mises en avant, listes de résultats) avec texte blanc.
- **Text secondary (#66646E)** : métadonnées, aides de champ, libellés secondaires, compteurs de filtres.
- **Border (#D9D7DE)** / **Border strong (#66646E)** : séparateurs décoratifs / contours de champs (≥ 3:1 exigé pour les composants interactifs).
- **Sémantiques** (success, warning, error, info + containers) : dérivées pour les besoins applicatifs. Le violet n'est jamais utilisé pour signaler une erreur.

Règle d'or : le violet couvre au plus ~10 % d'un écran. S'il est partout, il ne signale plus rien.

## Typography

- Police de marque : **Graphik** (commerciale, licence requise). Repli libre recommandé : **Inter** (Google Fonts), puis Arial.
- Titres en 600–700, interlignage serré (1.05–1.3), letter-spacing légèrement négatif sur les grandes tailles.
- Casse phrase partout (pas de Title Case, pas de MAJUSCULES sauf `eyebrow`).
- `eyebrow` : petit sur-titre en capitales espacées, violet foncé, au-dessus d'un titre de section.
- Corps en `body-md` (16px) par défaut ; `body-sm` pour tableaux, formulaires denses, métadonnées. Jamais en dessous de 12px.

## Layout

- Grille de 12 colonnes, gouttière 24px, largeur de contenu max 1280px ; marges latérales 16px en mobile, 32px en tablette, 48px en desktop.
- Échelle d'espacement en base 4/8 : `xs 4` · `sm 8` · `md 16` · `lg 24` · `xl 32` · `2xl 48` · `3xl 64` · `4xl 96`.
- Espacement vertical généreux entre sections (`3xl`–`4xl`) ; serré à l'intérieur des composants (`sm`–`md`).
- Breakpoints : `sm 640` · `md 768` · `lg 1024` · `xl 1280`.
- Alignement à gauche par défaut ; le centré est réservé aux états vides, aux pages d'erreur et aux hero de pages de recherche.
- **Pattern page de recherche / liste** : header + sub-header | zone de recherche hero centrée | `filter-panel` latéral (240px) + liste de `card-result` | pagination centrée.

## Elevation & Depth

Design plat. La profondeur vient du **contraste de fond** (blanc → gris clair → noir), pas des ombres.

- Niveau 0 : contenu sur `surface`.
- Niveau 1 : cartes = bordure 1px `border`, sans ombre.
- Niveau 2 (menus, popovers, dropdowns, tooltips) : ombre unique `0 4px 16px rgba(0,0,0,0.12)`.
- Niveau 3 (modales) : ombre `0 12px 32px rgba(0,0,0,0.18)` + voile `rgba(0,0,0,0.5)`.

## Motion

Transitions minimalistes. La rapidité signale la maîtrise.

- `duration-fast 120ms` : micro-interactions (hover de bouton, tooltip).
- `duration-base 200ms` : ouverture de dropdown, focus ring.
- `duration-slow 350ms` : entrée de modale, slide de panneau.
- Easing standard : `cubic-bezier(0.2, 0, 0, 1)` (Material Motion standard).
- `prefers-reduced-motion` : toutes les transitions passent à `0ms`, aucune animation de déplacement.

## Shapes

- Angles **vifs** par défaut (`rounded.none`) : boutons, champs, cartes, modales.
- `sm` / `md` (2–4px) tolérés pour petits éléments (checkbox, tooltip).
- `pill` réservé aux tags / chips et aux avatars.
- Traits : bordures 1px ; 2px pour le focus et l'indicateur d'onglet actif.

## Components

- **Boutons** : 48px de haut (40px en variante compacte). Hiérarchie : primaire violet (1 seul par vue), secondaire noir, ghost bordé. Sur fond sombre, utiliser `button-ghost-inverse` (bordure blanche). `button-icon` (48×48px carré) pour les actions contextuelles sur cartes.
- **Liens** : violet foncé #7500C0, soulignés au survol et au focus.
- **Champs de formulaire** : fond blanc, bordure 1px `border-strong`, angles vifs, label au-dessus, aide en dessous. Erreur : bordure + message en `error`, fond `error-container`. Variante `input-search-hero` pour les grandes zones de recherche centrées.
- **Checkbox** : 20×20px, bordure 2px, fond violet à l'état coché. Peut afficher un compteur de résultats à droite du label.
- **Select** : même hauteur que l'input, chevron custom aligné à droite.
- **Cartes** : blanches bordées, `surface-alt`, inversées (noir). `card-result` pour les listes de résultats : fond noir, titre h4 blanc, métadonnées `body-sm` séparées par « | », bouton icon à droite.
- **Header** : 72px, fond blanc, nav centrée, élément actif souligné violet 2px, action principale à droite.
- **Sub-header** : 48px, fond violet `primary`, texte blanc. Placé immédiatement sous le header pour une navigation contextuelle de section.
- **Tabs** : onglets texte, indicateur actif 2px violet, aucun fond. 2 à 6 onglets maximum.
- **Filter panel** : sidebar 240px, sections collapsibles, checkboxes avec compteurs, lien « Effacer ».
- **Pagination** : numérotée, page active soulignée 2px violet, flèches aux extrémités, centrée.
- **Footer** : fond noir, texte blanc, liens gris clair, grille multi-colonnes.
- **Tags / chips** : pilule, fond `primary-container`, texte violet foncé.
- **Alertes** : fond container + texte sémantique + barre gauche 4px + icône.
- **Tooltip** : fond noir, texte blanc caption, angles 4px, déclenché par hover/focus uniquement.
- **Tableaux** : en-tête `surface-alt` en `label`, lignes séparées par `border`, hover `surface-alt`, sélection `primary-container`.
- **Focus** : anneau 2px `primary` avec offset 2px sur tous les éléments interactifs (`:focus-visible`).
- **Modales** : angles vifs, 560px max, titre `h3`, actions alignées à droite (primaire à droite).
- **Empty state** : centré, icône + titre h3 + texte + 1 bouton primaire optionnel.

## Do's and Don'ts

**À faire**
- Un seul accent : le violet. Tout le reste en noir, blanc, gris.
- Un seul bouton primaire par vue.
- Contraste WCAG AA minimum partout (4,5:1 texte, 3:1 composants).
- Utiliser les sections inversées (noir) pour rythmer une page longue.
- Garder les angles vifs et la typographie grasse pour le caractère.
- Sur fond sombre, toujours utiliser `button-ghost-inverse` (jamais `button-ghost`).

**À ne pas faire**
- Pas de dégradés, pas d'ombres portées sur les cartes, pas de glassmorphism.
- Pas de violet pour les erreurs ou avertissements.
- Pas de texte sur `accent-tint` en dessous de 16px gras.
- Ne pas recréer le logo, le wordmark ni le glyphe « > » de la marque ; utiliser uniquement les assets officiels fournis par l'équipe marque, si l'usage est autorisé.
- Ne pas embarquer Graphik sans licence : utiliser Inter.
- Ne pas dépasser 2 niveaux de navigation (header + sub-header).

## Implementation

Le design system est framework-agnostic. Les tokens sont la source de vérité.

- **Tokens → CSS** : générer les variables depuis ce fichier plutôt que les recopier :
  - `npx @google/design.md export --format css-tailwind DESIGN.md > src/styles/theme.css` (Tailwind v4 / UnoCSS)
  - `npx @google/design.md export --format dtcg DESIGN.md > tokens.json` (Style Dictionary)
- Sans Tailwind : exposer les tokens en custom properties sur `:root` (`--color-primary`, `--space-md`, `--duration-fast`…).
- **Composants de base** à créer : `Button` (variants `primary | secondary | ghost | ghost-inverse | icon`, sizes `md | compact`, `disabled`), `Input`, `Select`, `Checkbox`, `Card` (variants `default | muted | inverse | result`), `Tag`, `Alert` (tones `success | warning | error | info`), `Tooltip`, `Tabs`, `Pagination`, `FilterPanel`, `Table`, `Modal`, `AppHeader`, `SubHeader`, `AppFooter`, `EmptyState`. Aucun écran ne style directement un élément natif.
- Si une librairie UI est déjà en place, la **thématiser** avec ces tokens plutôt que d'en ajouter une autre.
- Accessibilité : `:focus-visible` sur tout interactif, libellés reliés (`for`/`id`), `aria-invalid` + `aria-describedby` sur les champs en erreur, `prefers-reduced-motion` respecté.
- Qualité : ajouter `designmd lint DESIGN.md` en script npm / CI ; toute évolution du design passe par ce fichier.
