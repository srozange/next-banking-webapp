# Intégrations externes

> Ce fichier documente uniquement les systèmes **extérieurs** à l'application. La messagerie JMS interne (ActiveMQ Artemis embarqué WildFly) est documentée dans `ARCHITECTURE.md` (retro-map) et `INTEGRATIONS-OVERVIEW.md`.

---

## INT-001 — Azure OpenAI (Chat conseiller)

| Attribut | Valeur |
|---|---|
| **Système** | Azure OpenAI Service |
| **Feature** | FEAT-010 « Chat avec le conseiller » |
| **Protocole** | HTTPS / REST — `POST /openai/deployments/{deployment}/chat/completions?api-version={version}` |
| **Authentification** | Header `api-key` — valeur dans variable d'environnement `AZURE_OPENAI_API_KEY` (fallback hardcodé présent dans le code — risque sécurité) |
| **Endpoint** | Configurable via `AZURE_OPENAI_ENDPOINT` (valeur par défaut dans AppConfig) |
| **Déploiement** | Configurable via `AZURE_OPENAI_DEPLOYMENT` |
| **Version API** | Configurable via `AZURE_OPENAI_API_VERSION` |
| **Format request** | JSON `{"messages": [...], "max_tokens": N}` |
| **Format response** | JSON OpenAI Chat Completion — champ `choices[0].message.content` extrait |
| **Propriétaire côté client** | ConseillerService.java (appel synchrone bloquant via HttpURLConnection) |
| **SLA** | |
| **Comportement en indisponibilité** | HTTP ≠ 200 : message d'erreur affiché à l'utilisateur (corps de la réponse erreur loggé en WARN). Pas de retry ni de circuit-breaker. Pas de timeout configuré sur HttpURLConnection. |
| **Environnement de test** | Sandbox Azure disponible (portail Azure) — nécessite un déploiement GPT actif et une clé API valide. En local sans clé valide : HTTP 401 affiché à l'utilisateur. |

---

## INT-002 — Powens (Open Banking — agrégation de comptes)

| Attribut | Valeur |
|---|---|
| **Système** | Powens (ex-Budget Insight) — API PSD2 d'agrégation bancaire |
| **Features** | FEAT-012 « Comptes Tiers — Open Banking Powens », FEAT-014 « Callback OAuth Powens » |
| **Protocole** | HTTPS / REST — appels `PowensService.java` via HttpURLConnection |
| **Authentification** | Basic Auth (client_id + client_secret) → token temporaire utilisateur récupéré via `POST /auth/init`, puis `GET /auth/token/code`. Token transmis dans le header `Authorization: Bearer` pour les appels comptes/transactions. |
| **Endpoints consommés** | `POST /auth/init`, `GET /auth/token/code`, `GET /users/me/accounts`, `GET /users/me/accounts/{id}/transactions?limit=20` |
| **Webview OAuth** | Redirect vers `https://webview.powens.com/connect` avec paramètres `domain`, `client_id`, `redirect_uri`, `connector_ids`. Callback sur `/banking-legacy/powens-callback.xhtml`. |
| **Configuration** | Variables d'environnement : `POWENS_BASE_URL`, `POWENS_DOMAIN`, `POWENS_CLIENT_ID`, `POWENS_REDIRECT_URI`, `POWENS_CONNECTOR_ID` (aucune valeur par défaut fonctionnelle dans AppConfig) |
| **Propriétaire côté client** | PowensService.java + PowensCallbackBean.java |
| **SLA** | |
| **Comportement en indisponibilité** | Erreur HTTP avalée silencieusement dans `getAccounts()` et `getTransactions()` (catch vide ou LOG.warn seulement) — la page affiche une liste vide sans message d'erreur. Thread.sleep(2000) bloquant avant `getAccounts()` (dette). |
| **Environnement de test** | Sandbox Powens disponible (portail développeurs Powens) — nécessite credentials de sandbox. Sans credentials : la webview OAuth échoue, la page « Comptes Tiers » reste en état non connecté. |

---

## INT-003 — AlphaVantage (Cotations boursières)

| Attribut | Valeur |
|---|---|
| **Système** | Alpha Vantage — API de données de marché boursier |
| **Feature** | FEAT-009 « Bourse et watchlist » |
| **Protocole** | HTTPS / REST — endpoints `GLOBAL_QUOTE` et `TIME_SERIES_DAILY` / `WEEKLY` / `MONTHLY` |
| **Authentification** | Paramètre query `apikey` — variable d'environnement `ALPHA_VANTAGE_API_KEY` ; une clé de fallback est présente hardcodée dans le code source (dette sécurité — JIRA-9300) |
| **Endpoints consommés** | `GET /query?function=GLOBAL_QUOTE&symbol={symbol}&apikey={key}`, `GET /query?function=TIME_SERIES_{period}&symbol={symbol}&apikey={key}` |
| **Format response** | JSON — champs `Global Quote."05. price"`, `"Time Series (Daily)"`, etc. |
| **Propriétaire côté client** | `bourse/AlphaVantageService.java` |
| **SLA** | plan gratuit : 25 requêtes/jour, 5 req/min |
| **Comportement en indisponibilité** | Erreur HTTP / timeout → `LOG.error` + retour `null` ou liste vide. Aucun message utilisateur explicite côté JSF. Pas de cache ni de circuit-breaker. Pas de timeout configuré sur HttpURLConnection. |
| **Environnement de test** | Clé API gratuite disponible sur alphavantage.co — sandbox non dédié, données réelles en délai. La clé de fallback hardcodée dans le code est potentiellement révoquée ou limitée. |
