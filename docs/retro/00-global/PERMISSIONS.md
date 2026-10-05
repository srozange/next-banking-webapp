---
step: retro-permissions
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
generated_at: 2026-10-03
---

# PERMISSIONS — Matrice d'accès rôle × feature

---

## 1. Mécanisme d'authentification

| Attribut | Valeur |
|---|---|
| Type | JAAS formulaire JSF — `j_security_check` géré par WildFly 14 |
| Filtre centralisé | `web/AppFilter.java` — `@WebFilter(urlPatterns={"*.xhtml"})` |
| Stockage session | `HttpSession.getAttribute("currentCustomer")` — objet `Customer` complet |
| Timeout session | 30 minutes ( `<session-timeout>30</session-timeout>`) |
| Comportement à expiration | session null → redirect `/login.xhtml` |
| Déconnexion | `invalidateSession()` |
| Dette sécurité | Session fixation non corrigée : la session n'est pas invalidée avant la connexion (commentaire SECURITY DEBT) |

---

## 2. Grille rôle × feature

Légende : ✅ autorisé · 🔸 autorisé avec restriction de niveau ligne · ❌ refusé (redirect) · ? non observé

| ID | Feature | Page | ANONYME | ADMIN | CLIENT | CONSEILLER | Restriction niveau ligne |
|---|---|---|---|---|---|---|---|
| FEAT-001 | Connexion | login.xhtml | ✅ | ✅ | ✅ | ✅ | — (page publique) |
| FEAT-002 | Tableau de bord | dashboard.xhtml | ❌→login | 🔸 | 🔸 | 🔸 | Données filtrées par `customer_id` (session) |
| FEAT-003 | Effectuer un virement | virement.xhtml | ❌→login | 🔸 | 🔸 | 🔸 | Comptes source/cible appartenant au `customer_id` de session |
| FEAT-004 | Virements programmés | virement-programme.xhtml | ❌→login | 🔸 | 🔸 | 🔸 | Virements programmés filtrés par `customer_id` |
| FEAT-005 | Relevé de compte | releve.xhtml | ❌→login | 🔸 | 🔸 | 🔸 | Comptes filtrés par `customer_id` |
| FEAT-006 | Budget et catégories de dépenses | budget.xhtml | ❌→login | 🔸 | 🔸 | 🔸 | Budget filtré par `customer_id` |
| FEAT-007 | Demande et suivi de prêts | pret.xhtml | ❌→login | 🔸 | 🔸 | 🔸 | Prêts filtrés par `customer_id` |
| FEAT-008 | Documents | documents.xhtml | ❌→login | 🔸 | 🔸 | 🔸 | Comptes documentaires filtrés par `customer_id` |
| FEAT-009 | Bourse et watchlist | bourse.xhtml | ❌→login | 🔸 | 🔸 | 🔸 | Watchlist filtrée par `customer_id` |
| FEAT-010 | Chat avec le conseiller | conseiller.xhtml | ❌→login | 🔸 | 🔸 | 🔸 | Historique de conversation scopé à la session (`@SessionScoped`) |
| FEAT-011 | Catalogue produits bancaires | produits.xhtml | ❌→login | ✅ | ✅ | ✅ | — (catalogue commun, lecture seule) |
| FEAT-012 | Comptes Tiers — Open Banking Powens | comptes-tiers.xhtml | ❌→login | 🔸 | 🔸 | 🔸 | Comptes Powens filtrés par `customer_id` |
| FEAT-013 | Gestion des bénéficiaires externes | beneficiaires.xhtml | ❌→login | 🔸 | 🔸 | 🔸 | Bénéficiaires filtrés par `customer_id` |
| FEAT-014 | Callback OAuth Powens | powens-callback.xhtml | ✅ | ✅ | ✅ | ✅ | — (page transit publique) |
| FEAT-015 | Espace Conseiller | conseiller-workspace.xhtml | ❌→login | ✅ | ❌→dashboard | 🔸 | CONSEILLER : clients filtrés par `advisor_id` ; ADMIN : tous les clients |
| FEAT-016 | Administration — Gestion globale | admin.xhtml | ❌→login | ✅ | ❌→dashboard | ❌→dashboard | — |
| FEAT-017 | Téléchargement documents PDF | /docs/download | ? | 🔸 | 🔸 | 🔸 | `accountId` doit appartenir au customer en session (contrôle applicatif servlet) |

### Notes

- **FEAT-014** : ExcludeD d'AppFilter. Toute requête entrante reçoit le callback Powens, même sans session active.
- **FEAT-017** : Servlet `DocumentDownloadServlet` non couverte par AppFilter (`*.xhtml` uniquement). La session est vérifiée par le servlet lui-même (VAL-017-01-session-B1). Le comportement ANONYME n'a pas été observé en direct — marqué `?`.
- **ADMIN sur pages 🔸** : ADMIN a son propre compte client (rôle ADMIN) ; les pages dashboard, virement, etc. affichent ses données personnelles, pas celles de tous les clients. La gestion globale se fait via FEAT-016 (admin.xhtml).

---

## 3. Règles de niveau ligne

| Feature | Règle | Preuve |
|---|---|---|
| FEAT-002 à FEAT-013 | `customer_id` de session injecté dans toutes les requêtes EJB — chaque service filtre par `customer.getId()` | Pattern transversal dans les EJB (ex. `TransferService.java`, `LoanService.java`) |
| FEAT-013 | Suppression d'un bénéficiaire sans contrôle d'ownership — dette sécurité (RG-013-035) | `ExternalBeneficiaryService.java` — DELETE sans vérification `customer_id` |
| FEAT-015 | CONSEILLER : `ConseillerWorkspaceBean.java` filtre par `advisor.getId()` — chaque CONSEILLER ne voit que ses propres clients affectés | `web/ConseillerWorkspaceBean.java`, `FEAT-015/RG.md` RG-015-001 |
| FEAT-015 | ADMIN : accès à la page sans restriction de données — voit tous les clients (portefeuille vide en pratique pour admin1) | Crawl ADMIN depth=1 ; `FEAT-015/VALIDATION.md` |
| FEAT-017 | `accountId` doit appartenir au customer en session — contrôle implicite via chargement du compte (exception si non trouvé) | `web/DocumentDownloadServlet.java` |

---

## 4. Preuves des contrôles d'accès

| Règle | Preuve code | Preuve crawl |
|---|---|---|
| Toute *.xhtml → auth requise | `customer == null → redirect login.xhtml` | Crawl ADMIN, CONSEILLER, CLIENT : aucune page inaccessible retournée (pages non présentes = non testées en ANONYME) |
| admin.xhtml → ADMIN uniquement | `path.contains("admin.xhtml") && role != ADMIN → redirect dashboard.xhtml` | Crawl CLIENT : admin.xhtml absent ; Crawl CONSEILLER : admin.xhtml absent |
| conseiller-workspace.xhtml → CONSEILLER ou ADMIN | `role != CONSEILLER && role != ADMIN → redirect dashboard.xhtml` | Crawl CLIENT : conseiller-workspace.xhtml absent |
| login.xhtml → public | `path.contains("login.xhtml") → chain.doFilter` (pass-through) | N/A (crawl démarre post-auth) |
| powens-callback.xhtml → public | `path.contains("powens-callback.xhtml") → chain.doFilter` | FEAT-014/VALIDATION.md — vérification live : accès sans session confirmé |

---

## 5. Piste d'audit

| Attribut | Valeur |
|---|---|
| Service | `ejb/AuditService.java` |
| Méthode | `log(username, action, details)` et `log(username, action, details, ipAddress)` |
| Transaction | `@TransactionAttribute(REQUIRES_NEW)` — audit persisté même si la transaction principale est annulée |
| Table | `AUDIT_LOG` (déduit de DATA-DICTIONARY.md) |
| Actions journalisées | Virement exécuté, approbation/refus de prêt (`ADMIN_LOAN_APPROVE`, `ADMIN_LOAN_REFUSE`), activation/désactivation client (`ADMIN_ACTIVATE`, `ADMIN_DEACTIVATE`) |
| Non journalisé | Connexion/déconnexion, lecture de données, accès refusé |
| Exigence légale | exigence légale à traiter au cadrage de la nouvelle application (rien dans le code de l'existant) |

---

## 6. Features Batch et Transversal (sans objet pour les droits UI)

Les features FEAT-018 à FEAT-030 sont des batchs EJB Timer (`@Schedule`) ou des préoccupations transversales (JMS, audit, fraude) sans accès UI direct. Elles s'exécutent sous le compte système WildFly, pas sous un rôle utilisateur applicatif.

| Feature | Déclencheur | Compte d'exécution |
|---|---|---|
| FEAT-018 Calcul intérêts nocturnes | @Schedule 02:00 | JVM WildFly (système) |
| FEAT-019 Détection fraude nocturne | @Schedule 02:30 | JVM WildFly (système) |
| FEAT-020 Relevés mensuels (stub) | @Schedule 03:00/1er mois | JVM WildFly (système) |
| FEAT-021 Exécution virements programmés | @Schedule toutes les 5 min | JVM WildFly (système) |
| FEAT-022 Surveillance soldes faibles | @Schedule toutes les 30 min | JVM WildFly (système) |
| FEAT-023 à FEAT-030 | Événements applicatifs (JMS, HTTP) | N/A — préoccupations transversales |
