---
name: next-banking-webapp
description: >-
  Identité visuelle de la rénovation next-banking-webapp. Reprend tel quel le
  design system importé « Purple Consulting » (imports/design-system-purple-consulting.md,
  source unique) et n'ajoute que quatre composants bancaires construits à partir de ses tokens.
status: final
created: 2026-10-05
updated: 2026-10-05
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
    fontFamily: Inter, Arial, sans-serif
    fontSize: 3.5rem
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: -0.02em
  h1:
    fontFamily: Inter, Arial, sans-serif
    fontSize: 2.5rem
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: -0.015em
  h2:
    fontFamily: Inter, Arial, sans-serif
    fontSize: 2rem
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: -0.01em
  h3:
    fontFamily: Inter, Arial, sans-serif
    fontSize: 1.5rem
    fontWeight: 600
    lineHeight: 1.2
  h4:
    fontFamily: Inter, Arial, sans-serif
    fontSize: 1.25rem
    fontWeight: 600
    lineHeight: 1.3
  body-lg:
    fontFamily: Inter, Arial, sans-serif
    fontSize: 1.125rem
    fontWeight: 400
    lineHeight: 1.6
  body-md:
    fontFamily: Inter, Arial, sans-serif
    fontSize: 1rem
    fontWeight: 400
    lineHeight: 1.5
  body-sm:
    fontFamily: Inter, Arial, sans-serif
    fontSize: 0.875rem
    fontWeight: 400
    lineHeight: 1.45
  label:
    fontFamily: Inter, Arial, sans-serif
    fontSize: 0.875rem
    fontWeight: 600
    lineHeight: 1.3
  button:
    fontFamily: Inter, Arial, sans-serif
    fontSize: 1rem
    fontWeight: 600
    lineHeight: 1.25
  eyebrow:
    fontFamily: Inter, Arial, sans-serif
    fontSize: 0.75rem
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: 0.08em
  caption:
    fontFamily: Inter, Arial, sans-serif
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
  # ── Extensions next-banking-webapp (absentes du design system, aucune couleur nouvelle) ──
  transfer-status-badge:
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: 4px 12px
    note: >-
      Badge d'état d'un virement. Toujours un libellé texte (jamais la couleur seule) ;
      icône optionnelle 16px à gauche. Variantes ci-dessous.
  transfer-status-badge-executed:
    backgroundColor: "{colors.success-container}"
    textColor: "{colors.success}"
    note: "EXÉCUTÉ. Contraste 4,78:1."
  transfer-status-badge-suspended:
    backgroundColor: "{colors.warning-container}"
    textColor: "{colors.warning}"
    note: "SUSPENDU. Contraste 4,98:1."
  transfer-status-badge-released:
    backgroundColor: "{colors.info-container}"
    textColor: "{colors.info}"
    note: "LIBÉRÉ. Contraste 6,98:1."
  transfer-status-badge-rejected:
    backgroundColor: "{colors.error-container}"
    textColor: "{colors.error}"
    note: "REJETÉ. Contraste 5,15:1."
  transfer-status-badge-control-error:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.error}"
    borderColor: "{colors.error}"
    borderWidth: 1px
    note: >-
      REFUSÉ_ERREUR_CONTRÔLE. Fond blanc + bordure 1px pour se distinguer de REJETÉ.
      Contraste texte 6,04:1 ; bordure/fond 6,04:1 (seuil 3:1).
  amount:
    typography: "{typography.body-md}"
    fontVariantNumeric: tabular-nums
    textAlign: right
    note: >-
      Montant monétaire : chiffres tabulaires, aligné à droite, devise après le nombre
      (format exact du legacy, écran par écran, par exemple « 10,548.72 EUR » : le golden master compare ce texte, ne pas le normaliser), signe toujours explicite.
  amount-debit:
    textColor: "{colors.error}"
    fontWeight: 600
    note: "Préfixe « - » (tiret ASCII, comme le legacy, A-005-1 du PRD) obligatoire. 6,04:1 sur surface, 5,52:1 sur surface-alt."
  amount-credit:
    textColor: "{colors.success}"
    fontWeight: 600
    note: "Préfixe « + » obligatoire. 5,43:1 sur surface, 4,96:1 sur surface-alt."
  amount-neutral:
    textColor: "{colors.ink}"
    note: "Solde et totaux (sans signe). 21:1 sur surface."
  notification:
    typography: "{typography.body-sm}"
    padding: 12px 16px
    borderLeft: "4px solid currentColor"
    rounded: "{rounded.none}"
    meta-typography: "{typography.caption}"
    unread-indicator-color: "{colors.primary}"
    unread-indicator-size: 8px
    note: >-
      Ligne du centre de notifications interne. Icône 20px + type en `label` + texte.
      Pastille « non lu » `primary` (infobulle « Non lu »), 3:1 minimum vérifié sur
      les quatre fonds (4,52:1 à 4,84:1). Les emoji du legacy (✅ ⚠️ 🚨) restent dans le texte affiché, car le golden master compare ce texte ; une icône du design system peut les accompagner. [ASSUMPTION]
  notification-confirmation:
    backgroundColor: "{colors.success-container}"
    textColor: "{colors.success}"
    note: "CONFIRMATION. Contraste 4,78:1."
  notification-alert:
    backgroundColor: "{colors.warning-container}"
    textColor: "{colors.warning}"
    note: "ALERTE. Contraste 4,98:1."
  notification-fraud:
    backgroundColor: "{colors.error-container}"
    textColor: "{colors.error}"
    fontWeight: 600
    note: "FRAUDE. Contraste 5,15:1. Libellé en semi-gras."
  notification-low-balance:
    backgroundColor: "{colors.warning-container}"
    textColor: "{colors.warning}"
    note: "Solde faible. Contraste 4,98:1 ; distingué d'ALERTE par son icône et son libellé."
  app-nav:
    header: "{components.header}"
    sub-header: "{components.sub-header}"
    item-typography: "{typography.label}"
    item-gap: 24px
    header-item-textColor: "{colors.ink}"
    header-item-active-borderBottom: "2px solid {colors.primary}"
    sub-header-item-textColor: "{colors.on-primary}"
    sub-header-item-active-borderBottom: "2px solid {colors.on-primary}"
    max-levels: 2
    note: >-
      Barre applicative : header 72px (niveau 1 : domaines) + sub-header 48px (niveau 2 :
      pages du domaine). Jamais plus de 2 niveaux. Entrées filtrées par rôle. Contrastes :
      ink/surface 21:1, blanc/primary 5,3:1. Sous 768px, le header se replie en menu
      déroulant (niveau 1 puis niveau 2).
---

## Brand & Style

L'identité visuelle de next-banking-webapp EST celle du design system importé (`imports/design-system-purple-consulting.md`) : noir / blanc, un seul accent violet, typographie grasse, angles vifs. Ce fichier ne le recopie pas ; il en fixe l'usage et ajoute quatre composants bancaires.

> **Réserve de marque.** Le design system est une interprétation non officielle et non affiliée d'une marque. Il doit être validé avec l'équipe marque avant tout usage sous cette marque.

Le ton est sobre et rassurant : pas de décor, l'information financière d'abord.

## Colors

Palette inchangée (voir le design system). Rappels bancaires : violet `{colors.primary}` pour l'action principale et la navigation, jamais pour un état d'erreur ou d'avertissement. Les états métier utilisent uniquement les paires sémantiques `success`, `warning`, `error`, `info` avec leur `-container`. Aucune couleur nouvelle n'est introduite. Un état n'est jamais porté par la couleur seule (libellé, signe ou icône obligatoires).

## Typography

Familles et échelle du design system, police Inter seule, auto-hébergée (décision D11 : Graphik non utilisée, faute de licence d'auto-hébergement). Extension : tous les montants et colonnes numériques utilisent des chiffres tabulaires (`tabular-nums`) pour l'alignement vertical des décimales.

## Layout & Spacing

Échelle d'espacement, breakpoints et mouvement du design system. Montants alignés à droite dans les tableaux. Respect de `prefers-reduced-motion` (transitions à 0ms).

## Elevation & Depth

Plat, comme dans le design system : pas d'ombre sur les cartes ; ombre réservée aux modales et tooltips.

## Shapes

Angles vifs (`{rounded.none}`) ; pilule `{rounded.pill}` pour tags et badges d'état.

## Components

Tous les composants du design system s'appliquent (boutons, champs, tableaux, cartes, onglets, alertes, modales, état vide…). Couverture du kit legacy : navigation (CMP-001) -> `app-nav` ; tableau (CMP-003) -> `table-*` ; combobox (CMP-004) -> `select` ; badge (CMP-006) -> `tag` ou `transfer-status-badge` ; KPI (CMP-007) -> `card` + `amount-neutral` ; formulaire (CMP-008) -> `input`, `select`, `alert-error` ; notifications (CMP-009) -> `notification` ; cartes document (CMP-010) -> `card` + `tag`.

Extensions (tokens en frontmatter) :
- **Badge d'état de virement** : EXÉCUTÉ (succès), SUSPENDU (avertissement), LIBÉRÉ (info), REJETÉ (erreur), REFUSÉ_ERREUR_CONTRÔLE (blanc bordé d'erreur).
- **Montant** : débit en `error` avec « - » (tiret ASCII du legacy), crédit en `success` avec « + », solde en `ink` ; chiffres tabulaires.
- **Notification interne** : CONFIRMATION, ALERTE, FRAUDE, solde faible ; barre gauche 4px, pastille « non lu » violette.
- **Barre de navigation applicative** : header + sub-header du design system, 2 niveaux maximum.

Contraste WCAG 2.1 AA (4,5:1 texte, 3:1 composants) vérifié par calcul pour chaque paire ajoutée ; ratios dans les `note` des tokens.

## Do's and Don'ts

**À faire**
- Réutiliser les tokens du design system ; toute évolution passe par ce fichier.
- Doubler la couleur d'un état par un libellé, un signe ou une icône.
- Un seul bouton primaire par vue ; focus visible sur tout élément interactif.

**À ne pas faire**
- Ne pas recréer de logo, de wordmark ni d'assets de marque ; utiliser uniquement les assets officiels fournis par l'équipe marque, si l'usage est autorisé.
- Le style ne change jamais les textes affichés ni les attributs de test (`data-testid`) : le golden master du legacy les compare.
- Pas de nouvelle couleur, de dégradé ni d'ombre sur les cartes.
- Pas plus de 2 niveaux de navigation.
- Pas de violet pour les erreurs ou avertissements.
