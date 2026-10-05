---
feature: FEAT-029
title: "Intégration AlphaVantage (cotations boursières)"
interface: Transversal
status: current
---

# RG-029 — Intégration AlphaVantage (cotations boursières)

**Sources principales**
- `src/main/java/com/accenture/bank/bourse/AlphaVantageService.java`
- `src/main/java/com/accenture/bank/bourse/BourseBean.java`
- `src/main/java/com/accenture/bank/bourse/StockPoint.java`
- `src/main/java/com/accenture/bank/ejb/WatchlistService.java`
- `src/main/java/com/accenture/bank/entity/WatchlistItem.java`
- `src/main/java/com/accenture/bank/config/AppConfig.java`
- `src/main/webapp/bourse.xhtml`

---

## Stratégie d'appel API selon la période

RG-029-001 [Confirmé]
WHEN the user requests stock history for period "1Y" or "5Y"
THE SYSTEM SHALL call the AlphaVantage TIME_SERIES_WEEKLY endpoint.

RG-029-002 [Confirmé]
WHEN the user requests stock history for period "1W" or "1M"
THE SYSTEM SHALL call the AlphaVantage TIME_SERIES_DAILY endpoint with outputsize=compact (100 days).

RG-029-003 [Confirmé]
WHEN the requested period is not one of "1W", "1M", "1Y", "5Y"
THE SYSTEM SHALL apply a default cutoff of one month (getCutoff fallback).

---

## Filtrage et tri des données

RG-029-004 [Confirmé]
WHEN stock data points are retrieved from AlphaVantage
THE SYSTEM SHALL exclude any point whose date is strictly before the period cutoff date.

RG-029-005 [Confirmé]
WHEN stock data points are retrieved from AlphaVantage
THE SYSTEM SHALL return the list sorted ascending by date.

RG-029-006 [Confirmé]
WHEN AlphaVantage returns an "Information", "Note", or "Error Message" field in the JSON response
THE SYSTEM SHALL log a warning and return an empty list.

RG-029-007 [Confirmé]
WHEN the AlphaVantage HTTP response status is not 200
THE SYSTEM SHALL log a warning and return null (treated as no data by callers).

RG-029-008 [Confirmé]
WHEN any exception occurs during an AlphaVantage call
THE SYSTEM SHALL log the error and return an empty list without propagating the exception.

---

## Configuration de l'intégration

RG-029-009 [Confirmé]
WHEN the application starts
THE SYSTEM SHALL read the AlphaVantage API key from environment variable ALPHA_VANTAGE_API_KEY, then from System property, then fall back to a hardcoded default value.

RG-029-010 [Confirmé]
WHEN the application starts
THE SYSTEM SHALL read the AlphaVantage base URL from environment variable ALPHA_VANTAGE_BASE_URL, then from System property, then fall back to "https://www.alphavantage.co/query".

RG-029-011 [Suspect]
WHEN no connect or read timeout is configured for the AlphaVantage HTTP call
THE SYSTEM SHALL wait indefinitely for a response, risking thread starvation.

RG-029-012 [Suspect]
THE SYSTEM SHALL protect the AlphaVantage API key from being exposed in the source code.

---

## Affichage et calculs (BourseBean)

RG-029-013 [Confirmé]
WHEN the bourse page is loaded for the first time
THE SYSTEM SHALL pre-select the period "1M" as default.

RG-029-014 [Confirmé]
WHEN the bourse page is loaded and the watchlist catalogue is not empty
THE SYSTEM SHALL automatically load stock data for the first symbol in the catalogue.

RG-029-015 [Confirmé]
WHEN stock data fetch returns an empty or null history
THE SYSTEM SHALL display an informational error message mentioning the symbol, the period label, and possible causes (invalid symbol suffix, 25-requests/day rate limit).

RG-029-016 [Confirmé]
WHEN stock history is available
THE SYSTEM SHALL compute the latest price as the close price of the last data point.

RG-029-017 [Confirmé]
WHEN stock history is available
THE SYSTEM SHALL compute the period variation as (last close − first close).

RG-029-018 [Confirmé]
WHEN stock history is available
THE SYSTEM SHALL compute the period variation percentage as ((last close − first close) / first close) × 100.

RG-029-019 [Confirmé]
WHEN rendering the variation indicator
THE SYSTEM SHALL display the variation as positive (green, "▲") when variationPct >= 0, and as negative (red, "▼") otherwise.

RG-029-020 [Confirmé]
WHEN displaying the last sessions table
THE SYSTEM SHALL show at most the 10 most recent data points, ordered from most recent to oldest.

RG-029-021 [Confirmé]
WHEN stock history is available
THE SYSTEM SHALL generate the price chart as an inline SVG rendered server-side (800×300, with 5 horizontal grid lines and 5 date labels).

---

## Watchlist — gestion des valeurs

RG-029-022 [Confirmé]
IF the user submits an add-to-watchlist request with an empty or null symbol
THEN THE SYSTEM SHALL reject the request and inform the user that the symbol is mandatory.

RG-029-023 [Confirmé]
IF the user submits an add-to-watchlist request with an empty or null name
THEN THE SYSTEM SHALL reject the request and inform the user that the name is mandatory.

RG-029-024 [Confirmé]
IF the submitted symbol already exists in the watchlist (case-insensitive)
THEN THE SYSTEM SHALL reject the request and inform the user that the symbol already exists.

RG-029-025 [Confirmé]
WHEN a watchlist item is persisted
THE SYSTEM SHALL store the symbol in uppercase and trimmed form, and the name and market trimmed.

RG-029-026 [Confirmé]
WHEN a watchlist item is successfully added
THE SYSTEM SHALL set the newly added symbol as the selected symbol and immediately load its data.

RG-029-027 [Confirmé]
WHEN the watchlist catalogue is retrieved
THE SYSTEM SHALL return items ordered by market then by symbol.

RG-029-028 [Confirmé]
WHEN the user removes the currently selected symbol from the watchlist and the catalogue is not empty
THE SYSTEM SHALL automatically select the first remaining symbol and load its data.

RG-029-029 [Confirmé]
WHEN the user removes the last symbol from the watchlist
THE SYSTEM SHALL clear the selected symbol, history, and chart.

---

## Contraintes de champs — WatchlistItem

RG-029-030 [Confirmé]
IF a watchlist item symbol exceeds 20 characters
THEN THE SYSTEM SHALL reject the item.
  Champ : symbol (bourse.xhtml — maxlength=20)

RG-029-031 [Confirmé]
IF a watchlist item name exceeds 100 characters
THEN THE SYSTEM SHALL reject the item.
  Champ : name (bourse.xhtml — maxlength=100)

RG-029-032 [Confirmé]
WHEN adding a watchlist item
THE SYSTEM SHALL accept only one of the following markets: "Euronext Paris", "NASDAQ", "NYSE", "LSE", "Autre".
  Champ : market (selectOneMenu)

RG-029-033 [Confirmé]
WHEN adding a watchlist item
THE SYSTEM SHALL accept only one of the following currencies: "EUR", "USD", "GBP".
  Champ : currency (selectOneMenu)
