---
step: retro-ui / étape 4 consolidation globale
generated_by: retro-ui-agent
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources: retro/features/FEAT-001..FEAT-016/FIELDS.md (échantillon : FEAT-002, FEAT-003, FEAT-005, FEAT-016)
         retro/features/FEAT-001..FEAT-030/MESSAGES.md (exhaustif)
---

# UI-KIT — Composants récurrents de l'interface legacy

Inventaire des composants JSF récurrents identifiés sur l'ensemble des 16 features à écran.
Un composant est listé ici s'il apparaît sur **au moins 3 pages différentes** (ou 2 si sa
complexité justifie une entrée dédiée). Chaque entrée décrit le rendu, l'état vide, les
variantes observées et les pages qui l'utilisent — utile pour dimensionner le design system
cible React.

---

## CMP-001 — Barre de navigation principale

**Type JSF :** structure `<nav>` + `<h:form>` + `<h:commandLink>` / `<h:outputLink>`
**Présent sur :** toutes les pages authentifiées (14 pages) — absent de `login.xhtml`

### Structure

```
[DB] Demo Bank          Bonjour, {fullName}  [Deconnexion]
Tableau de bord | Virement | Virements programmes | Releve | Budget | Prets |
Documents | Bourse | Mon Conseiller | Produits | Comptes Tiers | Beneficiaires
[Espace Conseiller]¹   [Administration]²
```

¹ Visible si `securityBean.adminOrConseiller` (rôles CONSEILLER ou ADMIN)
² Visible si `securityBean.admin` (rôle ADMIN uniquement)

### Variantes observées

| Variante | Différence | Pages concernées |
|---|---|---|
| Standard CLIENT | Logo rouge `DB` + liens standards + salutation | 12 pages CLIENT |
| Conseiller | Logo vert `C` + menu latéral dédié (`Mon portefeuille` / `Conseiller IA`) + badge `Conseiller` | `conseiller-workspace.xhtml` |
| Admin | Même logo rouge + badge `ADMIN` rouge + liens `Espace Conseiller` + `Administration` visibles | `admin.xhtml` |
| Sans salutation | Aucune salutation (pas de client connecté dans le bean) | `conseiller-workspace.xhtml` (utilise un autre layout) |

### États

- **Authentifié :** navigation complète + salutation personnalisée
- **Non authentifié :** `login.xhtml` — barre minimale (logo + titre + sous-titre uniquement)

---

## CMP-002 — Bandeau technique legacy

**Type JSF :** `<p>` ou `<div>` statique, classe CSS `tech-info` ou équivalent
**Présent sur :** 13 des 16 pages écran (absent de `login.xhtml` et de `powens-callback.xhtml`)

### Objectif

Rappel informatif (non fonctionnel) de la stack technique de la page. Contenu variable
par page — voir MESSAGES.md §Partie 1.5 et chaque section feature pour le texte exact.

### Exemples de textes

| Page | Texte |
|---|---|
| `dashboard.xhtml` | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 / ActiveMQ Artemis (MDB)` |
| `virement.xhtml` | `Système Legacy — Virement géré par EJB CMT (Container Managed Transaction)` |
| `bourse.xhtml` | `🌐 API externe — Données temps réel via Alpha Vantage REST API — EJB Stateless → HTTP → JSON → SVG` |
| `admin.xhtml` | `Espace Administration — JEE 8 / JSF 2.3 / EJB 3.2 / RBAC custom — Role : ADMIN` |

### Impact cible React

Ce composant n'a pas de valeur fonctionnelle pour l'utilisateur final. La décision de le
conserver ou de le supprimer dans l'application moderne est une décision de conception (démo vs
production). Si conservé : composant `TechBanner` avec prop `label`.

---

## CMP-003 — Tableau de données (h:dataTable)

**Type JSF :** `<h:dataTable>` avec `<h:column>` et facettes `header`
**Présent sur :** `releve.xhtml`, `virement-programme.xhtml`, `pret.xhtml`, `budget.xhtml`,
`bourse.xhtml`, `beneficiaires.xhtml`, `comptes-tiers.xhtml`, `conseiller-workspace.xhtml`,
`admin.xhtml`, `produits.xhtml` (10 pages)

### Anatomie standard

```
┌─────────────────────────────────────────────────────┐
│  Col 1         Col 2         Col 3         Actions   │  ← en-têtes (facette)
├─────────────────────────────────────────────────────┤
│  valeur        valeur        badge          [Btn]    │  ← ligne de données
│  ...                                                 │
├─────────────────────────────────────────────────────┤
│  (Aucun enregistrement)                             │  ← état vide (rendered=empty)
└─────────────────────────────────────────────────────┘
```

### Variantes

| Variante | Caractéristiques | Pages |
|---|---|---|
| Lecture seule | Pas de bouton d'action | `releve.xhtml`, `comptes-tiers.xhtml` |
| Actions inline | Bouton(s) par ligne (`Supprimer`, `Suspendre/Reactiver`, `Approuver/Refuser`) | `virement-programme.xhtml`, `pret.xhtml`, `admin.xhtml` |
| Ligne sélectionnable | Clic ligne → panneau de détail (collapsible ou inline) | `conseiller-workspace.xhtml`, `admin.xhtml`, `pret.xhtml` |
| Badges intégrés | Colonne statut avec `<h:panelGroup styleClass="badge-*">` | `releve.xhtml`, `virement-programme.xhtml`, `pret.xhtml`, `beneficiaires.xhtml` |
| Sous-tableau | Tableau enfant cliquable pour détail (échéancier dans `pret.xhtml`) | `pret.xhtml` |

### États vides observés

| Page | Texte état vide |
|---|---|
| `releve.xhtml` | `Aucune transaction pour ce compte.` |
| `virement-programme.xhtml` | `Aucun virement programme configure` |
| `pret.xhtml` | `Aucun emprunt enregistre` |
| `beneficiaires.xhtml` | `Aucun beneficiaire enregistre. Utilisez le formulaire ci-dessus pour en ajouter un.` |
| `comptes-tiers.xhtml` | `Aucune transaction disponible.` |
| `admin.xhtml` (prêts pipeline) | `Aucun pret en attente` |
| `admin.xhtml` (historique) | `Aucun pret en base` |
| `produits.xhtml` | `Aucun produit d epargne/credit/investissement disponible` |

---

## CMP-004 — Combobox avec option vide

**Type JSF :** `<h:selectOneMenu>` avec `<f:selectItem itemLabel="…" itemValue="#{null}"`
**Présent sur :** `virement.xhtml` (2×), `virement-programme.xhtml` (2×), `releve.xhtml`,
`budget.xhtml` (2×), `pret.xhtml`, `bourse.xhtml` (marché et devise) — 8+ occurrences

### Texte de l'option vide

| Texte exact | Pages |
|---|---|
| `-- Choisir un compte --` | `virement.xhtml` (source et destinataire) |
| `-- Choisir --` | `virement-programme.xhtml`, `budget.xhtml` |
| _(option vide sans libellé)_ | `releve.xhtml` (sélecteur de compte) |

### Comportement AJAX

Sur `releve.xhtml` : `f:ajax event="change"` → rechargement de la liste de transactions (listener `releveBean.onAccountChange`). Les autres comboboxes soumettent par le bouton de formulaire uniquement (pas de `f:ajax`).

---

## CMP-005 — Panneau de détail client inline

**Type JSF :** `<h:panelGroup rendered="#{bean.selectedXxx != null}">` — affichage conditionnel
**Présent sur :** `conseiller-workspace.xhtml`, `admin.xhtml`

### Anatomie

```
┌─────────────────────────────────────────────────────┐
│  Detail : {nom client}                [Fermer]      │
├────────────────────────┬────────────────────────────┤
│  Comptes bancaires     │  Prets                     │
│  ┌──────────────────┐  │  ┌──────────────────────┐  │
│  │ Numero│Type│Solde│  │  │ Type│Montant│Stat…    │  │
│  └──────────────────┘  │  └──────────────────────┘  │
│  Aucun compte          │  Aucun pret                │
└─────────────────────────────────────────────────────┘
```

### Différences entre pages

| Page | Colonnes comptes | Colonnes prêts | Champs en plus |
|---|---|---|---|
| `conseiller-workspace.xhtml` | Numero / Type / Solde | Type / Montant / Mensualite / Statut | — |
| `admin.xhtml` | Même structure | Même structure | Labels libres (Nom complet, Identifiant, Email, Telephone, Role, Statut, Conseiller assigne, Client depuis) |

---

## CMP-006 — Badge de statut

**Type JSF :** `<h:panelGroup styleClass="badge badge-{variant}">` (CSS inline)
**Présent sur :** `releve.xhtml`, `virement-programme.xhtml`, `pret.xhtml`, `beneficiaires.xhtml`,
`admin.xhtml`, `conseiller-workspace.xhtml`, `produits.xhtml` (7 pages)

### Catalogue des badges

| Valeur affichée | Variante CSS | Couleur approx. | Contexte |
|---|---|---|---|
| `CREDIT` | `badge-credit` | Vert | Type transaction — `releve.xhtml` |
| `DEBIT` | `badge-debit` | Rouge | Type transaction — `releve.xhtml` |
| `En attente` | `badge-pending` | Orange | Transaction EN_ATTENTE — `releve.xhtml` |
| `Actif` | `badge-actif` | Vert | Virement programmé actif — `virement-programme.xhtml` |
| `Inactif` | `badge-inactif` | Gris | Virement programmé suspendu |
| `En cours d approbation` | Jaune/orange | — | LoanStatus EN_ATTENTE |
| `En cours` | Vert | — | LoanStatus EN_COURS |
| `Refuse par le service credit` | Rouge | — | LoanStatus REFUSE |
| `Rembourse` | Mauve | — | LoanStatus REMBOURSE |
| `En defaut` | Rouge | — | LoanStatus DEFAUT |
| `✓ SEPA` | `badge-sepa` | Vert | Zone SEPA — `beneficiaires.xhtml` |
| `Hors SEPA` | `badge-hors-sepa` | Orange | Hors SEPA |
| `Actif` / `Inactif` | Vert / Gris | — | Statut client — `admin.xhtml`, `conseiller-workspace.xhtml` |
| `SUCCESS` | Vert | — | BatchJobStatus — `produits.xhtml`, `admin.xhtml` |
| `FAILED` | Rouge | — | BatchJobStatus |
| `PARTIAL` | Orange | — | BatchJobStatus |
| `OPEN` | `#E3000F` (rouge) | — | FraudStatus — `admin.xhtml` |
| `CONFIRMED` | `#ff6600` (orange) | — | FraudStatus |
| `FALSE_POSITIVE` | `#4caf50` (vert) | — | FraudStatus |
| `REVIEWED` | `#888` (gris) | — | FraudStatus |
| `ADMIN` | Rouge | — | Badge rôle — `admin.xhtml` |
| `Conseiller` | Vert | — | Badge rôle — `conseiller-workspace.xhtml` |
| `COURANT` / `EPARGNE` | Bleu | — | Type de compte — `conseiller-workspace.xhtml` |

---

## CMP-007 — Carte KPI

**Type JSF :** `<h:panelGrid>` ou `<div>` avec valeur numérique large + libellé secondaire
**Présent sur :** `dashboard.xhtml` (2 cartes solde), `conseiller-workspace.xhtml` (3 KPI),
`admin.xhtml` (4 KPI), `budget.xhtml` (3 cartes récapitulatif)

### Anatomie standard

```
┌────────────────────────┐
│   {valeur principale}  │  ← grande police
│   {libellé court}      │  ← texte secondaire
│   {sous-libellé}       │  ← (optionnel)
└────────────────────────┘
```

### Inventaire par page

| Page | KPI | Valeur | Libellé | Sous-libellé |
|---|---|---|---|---|
| `dashboard.xhtml` | Solde Demo Bank | `{totalBalance} €` | `Solde total Demo Bank` | `Tous comptes Demo Bank confondus` |
| `dashboard.xhtml` | Patrimoine (si Powens) | `{totalPatrimoine} €` | `Patrimoine total` | `Demo Bank + comptes externes` |
| `conseiller-workspace.xhtml` | Clients | `{N}` | `Mes clients` | `clients dans mon portefeuille` |
| `conseiller-workspace.xhtml` | Encours | `{montant} €` | `Encours total` | `solde cumulé mes clients` |
| `conseiller-workspace.xhtml` | Prêts EN_ATTENTE | `{N}` | `Prets en attente` | `a traiter` |
| `admin.xhtml` | Clients | `{N}` | `Total clients` | `{n} actifs` |
| `admin.xhtml` | Conseillers | `{N}` | `Conseillers` | `actifs` |
| `admin.xhtml` | Prêts pipeline | `{N}` | `Prets en attente` | `{montant} EUR a traiter` |
| `admin.xhtml` | Utilisateurs | `{N}` | `Total utilisateurs` | `tous roles confondus` |
| `budget.xhtml` | Total dépenses | `{montant} €` | `Total depenses` | — |
| `budget.xhtml` | Total revenus | `{montant} €` | `Total revenus` | — |
| `budget.xhtml` | Plafonds | `{N}` | `Plafonds definis` | — |

---

## CMP-008 — Formulaire standard

**Type JSF :** `<h:form>` + `<h:panelGrid>` ou `<div>` structuré + `<h:commandButton>`
**Présent sur :** `virement.xhtml`, `virement-programme.xhtml`, `pret.xhtml`, `budget.xhtml`,
`beneficiaires.xhtml`, `bourse.xhtml` (ajout watchlist) — 6 pages

### Anatomie

```
┌─────────────────────────────────────────┐
│  [Titre de section]                     │
├─────────────────────────────────────────┤
│  Label *      [input / select]          │
│               [message d'erreur JSF]    │
│  Label *      [input / select]          │
│               [message d'erreur JSF]    │
│  ...                                    │
├─────────────────────────────────────────┤
│                          [Bouton submit]│
└─────────────────────────────────────────┘
```

### Comportements communs

- Les messages d'erreur JSF apparaissent au-dessous du champ concerné via `<h:message for="id">`
- Les messages globaux (FacesMessage) apparaissent via `<h:messages globalOnly="true">` ou
  `<h:messages>` en haut du formulaire (variable selon les pages)
- Le bouton de soumission est un `<h:commandButton action="#{bean.methode}">` (synchrone)
- Aucun formulaire de l'application n'utilise AJAX pour la soumission principale (sauf
  vérification IBAN dans `beneficiaires.xhtml` et sélecteur de compte dans `releve.xhtml`)

---

## CMP-009 — Centre de notifications in-memory (JMS)

**Type JSF :** section dédiée de `dashboard.xhtml` — `<h:form>` + `<h:dataTable rendered=…>`
**Présent sur :** `dashboard.xhtml` uniquement (mais alimenté par tous les virements et prêts)

### Anatomie

```
▶ Notifications temps reel (Message Broker)
┌─────────────────────────────────────────────────────┐
│ Centre de notifications     [N nouvelle(s)]          │
│ ActiveMQ Artemis - MDB JEE - queue/virements        │
│ [Tout marquer lu]  [Effacer tout]                   │
├─────────────────────────────────────────────────────┤
│ ✅ CONFIRMATION  {texte}     {date} - publie via... │  ← ligne notification
│ ⚠️ ALERTE       {texte}     {date} - publie via... │
│ 🚨 FRAUDE       {texte}     {date} - publie via... │
├─────────────────────────────────────────────────────┤
│ (Aucune notification - effectuez un virement…)      │  ← état vide
└─────────────────────────────────────────────────────┘
```

### Types et rendu

| Type | Icône | Couleur fond |
|---|---|---|
| `CONFIRMATION` | `✅` | `#e8f5e9` (vert) |
| `ALERTE` | `⚠️` | `#fff8e1` (jaune) |
| `FRAUDE` | `🚨` | `#fce4ec` (rose) |

### État non-lu

Point indicateur violet avec infobulle `Non lu` — supprimé par `Tout marquer lu`

---

## CMP-010 — Cartes de téléchargement de documents

**Type JSF :** `<h:panelGroup styleClass="doc-card">` + `<h:outputLink target="_blank">`
**Présent sur :** `documents.xhtml` uniquement

### Anatomie d'une carte

```
┌────────────────────────────────┐
│  [Badge type]                  │  ← ex. « Fiscal », « Bancaire », « Attestation »
│  {Titre document}              │  ← ex. « IFU 2024 », « RIB »
│  {Sous-titre}                  │  ← ex. « Revenus de capitaux mobiliers »
│  [Telecharger PDF]             │  ← lien GET /docs/download?type=…&accountId=…
└────────────────────────────────┘
```

### Cartes présentes

| Titre | Badge | Paramètre `type` | Paramètre `accountId` |
|---|---|---|---|
| `IFU {N-1}` | `Fiscal` | `IFU` | — (IFU global) |
| `IFU {N-2}` | `Fiscal` | `IFU` | — |
| `RIB` | `Bancaire` | `RIB` | `{acc.id}` par compte |
| `Attestation de solde` | `Attestation` | `ATTESTATION` | `{acc.id}` par compte |
| `{moisLibelle} {year}` × 12 | — (liste) | `RELEVE` | `{acc.id}` par compte |

---

## CMP-011 — Simulateur financier avec résultat collapsible

**Type JSF :** `<h:panelGroup rendered="#{bean.result != null}">` + `<h:commandLink onclick="…">`
**Présent sur :** `pret.xhtml` (simulateur de prêt) uniquement

### Anatomie

```
┌────────────────────────────────────────────┐
│ Simuler un emprunt                         │
│ Montant  [____]  Taux  [____]  Durée [___] │
│ [Calculer]                                 │
├────────────────────────────────────────────┤  ← si simulationResult != null
│ Mensualité {X}    Coût total {Y}           │
│ Total intérêts {Z}                         │
│ [Voir l'échéancier complet (N échéances)]  │
│   ▼ (collapsible : tableau #/Capital/…)    │
└────────────────────────────────────────────┘
```

---

## Notes de conception pour la migration React

| Composant legacy | Composant React suggéré | Lib cible |
|---|---|---|
| CMP-001 Navigation | `<Navbar>` + `<NavLink>` conditionnel par rôle | React Router v6 |
| CMP-002 Bandeau tech | `<TechBanner label="…">` | Composant local (prop) |
| CMP-003 Tableau | `<DataTable>` + `<EmptyState>` | composant local ou TanStack Table |
| CMP-004 Combobox | `<Select>` avec option `null` | React Hook Form + MUI ou shadcn |
| CMP-005 Détail inline | `<SlidePanel>` ou `<Drawer>` conditionnel | MUI Drawer ou Radix |
| CMP-006 Badge | `<Badge variant="…">` | shadcn/ui Badge |
| CMP-007 KPI | `<KpiCard label value subLabel>` | composant local |
| CMP-008 Formulaire | `<form>` + React Hook Form + `<FormField>` | React Hook Form + Zod |
| CMP-009 Notifications | `<NotificationCenter>` WebSocket ou polling | React Query + SSE |
| CMP-010 Doc carte | `<DocumentCard type accountId>` | composant local |
| CMP-011 Simulateur | `<LoanSimulator>` avec accordion | composant local |
