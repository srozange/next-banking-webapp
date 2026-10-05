---
feature: FEAT-009
name: Bourse et watchlist
status: current
generated_by: retro-rules
date: 2026-10-03
---

# Règles de gestion — FEAT-009 « Bourse et watchlist »

Périmètre : `bourse.xhtml` + `BourseBean.java` + `AlphaVantageService.java`
+ `WatchlistService.java` + `WatchlistItem.java` + `AppConfig.java` (clé API)
+ `AppFilter.java` (contrôle d'accès)

---

## Chargement et navigation

RG-009-001 [À confirmer]
WHEN the user navigates to `/bourse.xhtml` THE SYSTEM SHALL load the watchlist,
automatically select the first item (ordered by market then symbol), and fetch its
historical data before rendering the page.

RG-009-002 [À confirmer]
WHEN the user changes the selected symbol or the selected period THE SYSTEM SHALL fetch
and render updated historical data via AJAX without a full-page reload.

---

## Affichage des données de marché

RG-009-003 [À confirmer]
WHEN historical data is returned THE SYSTEM SHALL compute and display:
(a) the current price as the closing price of the most recent data point,
(b) the absolute variation as (last closing price − first closing price) over the period,
(c) the percentage performance as (last − first) / first × 100.

RG-009-004 [À confirmer]
WHEN the percentage performance is greater than or equal to zero THE SYSTEM SHALL render
the price chart and performance indicators in green; WHEN it is strictly negative THE
SYSTEM SHALL render them in red.
            couleurs : vert `#2e7d32`, rouge `#E3000F`

RG-009-005 [À confirmer]
WHEN historical data is available THE SYSTEM SHALL display a price chart as an inline SVG
generated server-side, with five evenly-spaced date labels on the X-axis in MM-DD format
and five horizontal grid lines.
            format date : `dateStr.substring(5)` sur ISO YYYY-MM-DD

RG-009-006 [À confirmer]
WHEN historical data is available THE SYSTEM SHALL display the last 10 data points in
reverse chronological order (most recent first) with open, high, low, and close prices
(2 decimal places) and volume; THE SYSTEM SHALL color each row green when close ≥ open
and red when close < open.

---

## Stratégie d'appel AlphaVantage

RG-009-007 [À confirmer]
WHEN the selected period is "1W" or "1M" THE SYSTEM SHALL call the AlphaVantage
TIME_SERIES_DAILY compact endpoint (last 100 days) and include only data points whose
date is on or after the computed cutoff (today minus 1 week or today minus 1 month).
            `getCutoff()` :

RG-009-008 [À confirmer]
WHEN the selected period is "1Y" or "5Y" THE SYSTEM SHALL call the AlphaVantage
TIME_SERIES_WEEKLY endpoint and include only data points whose date is on or after
today minus 1 year or today minus 5 years respectively.
            `getCutoff()` :

RG-009-009 [À confirmer]
IF the AlphaVantage API returns an HTTP status other than 200, or a JSON response
containing an "Information", "Note", or "Error Message" field, or an empty time series,
THEN THE SYSTEM SHALL suppress the chart and data table and display a contextual error
message naming the selected symbol and period.

---

## Watchlist — consultation

RG-009-010 [À confirmer]
THE SYSTEM SHALL maintain a single watchlist shared across all authenticated users
regardless of role (CLIENT, CONSEILLER, ADMIN), with no per-user filtering.

RG-009-011 [À confirmer]
WHEN the watchlist is fetched THE SYSTEM SHALL return items ordered by market ascending,
then by symbol ascending.

---

## Watchlist — ajout

RG-009-012 [À confirmer]
IF the user submits the add form with an empty or blank symbol
THEN THE SYSTEM SHALL reject the submission and display the error
"Le symbole est obligatoire (ex: AMZN, SG.PA)" without closing the form.
  Champ   : newSymbole (FEAT-009/SCR-009-03)
  Message : "Le symbole est obligatoire (ex: AMZN, SG.PA)"

RG-009-013 [À confirmer]
IF the user submits the add form with an empty or blank name
THEN THE SYSTEM SHALL reject the submission and display the error
"Le nom de la valeur est obligatoire" without closing the form.
  Champ   : newNom (FEAT-009/SCR-009-03)
  Message : "Le nom de la valeur est obligatoire"

RG-009-014 [À confirmer]
IF the user submits the add form with a symbol that already exists in the watchlist
(case-insensitive) THEN THE SYSTEM SHALL reject the submission and display the error
"Le symbole « {SYMBOL} » existe déjà dans votre watchlist" without closing the form.
  Champ   : newSymbole (FEAT-009/SCR-009-03)
            `UPPER(w.symbol) = UPPER(:s)`

RG-009-015 [À confirmer]
WHEN a new watchlist item is successfully added THE SYSTEM SHALL store the symbol
uppercased and trimmed (max 20 chars), the name trimmed (max 100 chars), the market
trimmed (max 50 chars), and the currency uppercased and trimmed (max 10 chars).

RG-009-016 [À confirmer]
WHEN a new watchlist item is successfully added THE SYSTEM SHALL automatically select
the newly added symbol, close the add form, and fetch historical data for the new symbol.

---

## Watchlist — suppression

RG-009-017 [À confirmer]
WHEN the user clicks the delete button on a watchlist item THE SYSTEM SHALL immediately
remove the item without displaying a confirmation dialog.

RG-009-018 [À confirmer]
WHEN the currently selected symbol is deleted THE SYSTEM SHALL automatically select the
first item remaining in the watchlist and fetch its data; IF the watchlist becomes empty
after deletion THE SYSTEM SHALL clear the selected symbol and suppress chart and table.

---

## Contrôle d'accès

RG-009-019 [À confirmer]
THE SYSTEM SHALL allow access to `/bourse.xhtml` for any authenticated user regardless
of role (CLIENT, CONSEILLER, ADMIN); no role restriction beyond the general
authentication check is applied to this page.

---

## Dettes techniques legacy

RG-009-020 [Suspect]
WHEN the AlphaVantage HTTP connection is initiated THE SYSTEM SHALL complete the request
within a bounded time to prevent indefinite thread blocking.

RG-009-021 [Suspect]
THE SYSTEM SHALL retrieve the AlphaVantage API key exclusively from a secure
configuration source (environment variable or system property) and never fall back to
a value committed to source code.
            valeur par défaut hardcodée `"NH55CH0ZLQH8RFIH"`
            active en l'absence de variable d'environnement — FIXME JIRA-9300
