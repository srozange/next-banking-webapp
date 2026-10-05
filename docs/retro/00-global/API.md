# API — Application legacy Banking

> **Note** : Application JSF 2.3 rendue côté serveur. Les échanges réseau sont des soumissions de formulaire HTML (`multipart/form-data` ou `application/x-www-form-urlencoded` + `javax.faces.ViewState`) et non des endpoints REST. Un seul endpoint REST véritable existe : le servlet de téléchargement de documents (`/docs/download`). Les identifiants de champs JSF générés (`j_idt*`) sont instables entre déploiements ; les noms fonctionnels issus des FIELDS.md correspondants sont utilisés ci-dessous lorsque disponibles.

---

## Endpoints REST (tag : api)

### GET /docs/download — Téléchargement de document PDF

**Feature** : FEAT-017 « Téléchargement documents PDF »  
**Implémentation** : `web/DocumentDownloadServlet.java`  
**Authentification** : session JAAS active (cookie JSESSIONID) — rôle CLIENT requis  
**Content-Type réponse** : `application/pdf` (200) ou `text/html` (erreur)

| Paramètre query | Type | Obligatoire | Valeurs admises |
|---|---|---|---|
| `type` | string | oui | `RELEVE`, `RIB`, `ATTESTATION`, `IFU` |
| `accountId` | string (Long) | oui | identifiant numérique d'un compte appartenant au client connecté |
| `year` | string (int) | selon type | année (ex. `2024`) |
| `month` | string (int) | pour RELEVE | mois 1–12 |

| Code HTTP | Situation |
|---|---|
| 200 | Document généré, corps = PDF binaire |
| 302 | Session expirée → redirection login.xhtml |
| 400 | Paramètre `type` absent ou invalide |
| 404 | Compte introuvable ou n'appartient pas au client |
| 500 | Erreur JPA (LazyInitializationException sur account.getCustomer()) — cas RELEVE/RIB/ATTESTATION en prod |

> **Dette** : seul IFU est fonctionnel. RELEVE, RIB, ATTESTATION génèrent systématiquement HTTP 500 en production (navigation lazy sans transaction JTA englobante).

---

### GET /banking-legacy/ — Point d'entrée racine

**Feature** : FEAT-001 « Authentification »  
Redirection 302 vers `login.xhtml` pour tout visiteur. Pas de paramètre.

---

### GET /banking-legacy/powens-callback.xhtml — Callback OAuth Powens

**Feature** : FEAT-014 « Callback OAuth Powens »  
**Méthode** : GET (paramètres transmis par la webview Powens dans la query string)

| Paramètre query | Type | Obligatoire | Description |
|---|---|---|---|
| `connection_id` | string | non | Identifiant connexion Powens (flux principal) |
| `code` | string | non | Code OAuth alternatif |
| `error` | string | non | Code d'erreur Powens (ex. `user_cancel`) |

| Code HTTP | Situation |
|---|---|
| 200 | Page JSF affichée (état selon paramètres) |
| 302 | Redirection vers comptes-tiers.xhtml après traitement |

---

## Formulaires JSF (tag : page — soumissions POST)

Les endpoints suivants reçoivent des soumissions JSF. Chaque POST inclut `javax.faces.ViewState` (token CSRF JSF, opaque, instable). Les noms de champs listés sont les noms fonctionnels ; les identifiants JSF générés (`j_idt*`) varient selon le déploiement.

### POST /banking-legacy/login.xhtml — Authentification

**Feature** : FEAT-001 « Authentification »

| Champ fonctionnel | Obligatoire |
|---|---|
| `username` | oui |
| `password` | oui |

### POST /banking-legacy/virement.xhtml — Virement bancaire

**Feature** : FEAT-002 « Virement bancaire »

| Champ fonctionnel | Obligatoire |
|---|---|
| `compteSource` | oui |
| `compteDestinataire` | oui |
| `montant` | oui |
| `motif` | non |

### POST /banking-legacy/virement-programme.xhtml — Virement programmé

**Feature** : FEAT-003 « Virement programmé »

| Champ fonctionnel | Obligatoire |
|---|---|
| `compteSource` | oui |
| `compteDestinataire` | oui |
| `montant` | oui |
| `frequence` | oui |
| `dateDebut` | oui |
| `motif` | non |

### POST /banking-legacy/releve.xhtml — Paramètres document (redirect servlet)

**Feature** : FEAT-008 « Documents (relevés, attestations, IFU) »  
Ce formulaire POST lance une redirection vers `/docs/download` avec les paramètres type/accountId/year/month.

### POST /banking-legacy/bourse.xhtml — Bourse et watchlist

**Feature** : FEAT-009 « Bourse et watchlist »

| Champ fonctionnel | Obligatoire | Action |
|---|---|---|
| `symboleValeur` | pour cotation | recherche cours |
| `periode` | non | historique (1M / 3M / 6M / 1Y) |
| `newSymbole` | pour ajout | ajout watchlist |
| `newNom` | pour ajout | ajout watchlist |
| `newMarche` | pour ajout | ajout watchlist |
| `newDevise` | pour ajout | ajout watchlist |

### POST /banking-legacy/conseiller.xhtml — Chat avec le conseiller

**Feature** : FEAT-010 « Chat avec le conseiller »

| Champ fonctionnel | Obligatoire |
|---|---|
| `userInput` | oui (rejet silencieux si vide) |

### POST /banking-legacy/pret.xhtml — Demande de prêt

**Feature** : FEAT-006 « Demande de prêt »

| Champ fonctionnel | Obligatoire |
|---|---|
| `montant` | oui |
| `dureeEnMois` | oui |
| `objet` | non |

### POST /banking-legacy/beneficiaires.xhtml — Gestion des bénéficiaires

**Feature** : FEAT-013 « Gestion des bénéficiaires externes »

| Champ fonctionnel | Obligatoire | Action |
|---|---|---|
| `libelle` | pour ajout | ajout bénéficiaire |
| `nomTitulaire` | pour ajout | ajout bénéficiaire |
| `iban` | pour ajout | ajout bénéficiaire |
| `bic` | non | ajout bénéficiaire |
| `banque` | non | ajout bénéficiaire |
| — | — | suppression (id lié JSF) |

### POST /banking-legacy/comptes-tiers.xhtml — Connexion Open Banking Powens

**Feature** : FEAT-012 « Comptes Tiers — Open Banking Powens »  
Aucun champ de saisie libre : action de connexion/déconnexion via boutons JSF (id liés).

### POST /banking-legacy/admin.xhtml — Administration

**Feature** : FEAT-016 « Administration — Gestion globale »  
Champs : `clientFilter` (filtre recherche clients, in-memory). Actions via boutons JSF : approbation/refus prêt, activation/désactivation client, confirmation alerte fraude.

### POST /banking-legacy/budget.xhtml — Budget / analyse dépenses

**Feature** : FEAT-005 « Budget et analyse des dépenses »  
Sélection de période uniquement (pas de saisie de données métier).

### POST /banking-legacy/conseiller-workspace.xhtml — Espace Conseiller

**Feature** : FEAT-015 « Espace Conseiller — Portefeuille clients »  
Lecture seule + sélection client (id lié JSF).

---

## Ressources statiques (hors périmètre)

- `GET /banking-legacy/javax.faces.resource/**` — framework JSF, CSS, JS — ignoré.
