# FEATURES

_Inventaire complet des features — généré par retro-features._
_Sources : crawl Playwright BFS multi-rôles (2026-10-02, 3 rôles, 73 pages) + analyse statique (code + web.xml + annotations) + git churn (365 j)._
_Usage réel non disponible — logs d'accès absents._
_Rôles non crawlés : aucun — ADMIN, CONSEILLER, CLIENT tous crawlés._

**⚠️ GATE MÉTIER** : ce fichier doit être validé par un expert métier avant de poursuivre le pipeline.

---

## Features CLIENT (tous les rôles : CLIENT, CONSEILLER, ADMIN)

| ID | Nom métier | Interface | Résumé | Point d'entrée | Fichier principal | Preuve | Source | Statut | Pourquoi source unique |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| FEAT-001 | Connexion | Écran | Le client saisit son identifiant et son mot de passe pour accéder à son espace bancaire. Sans authentification, toute page de l'application redirige vers cet écran. | `/login.xhtml` (web.xml: welcome-file) | `web/LoginBean.java` | redirection login pour tout accès non authentifié welcome-file | Statique | Actif | `ui:lien` — page de connexion non incluse dans le BFS (crawl démarre post-authentification via login-state.py) |
| FEAT-002 | Tableau de bord | Écran | L'utilisateur visualise d'un coup d'œil ses comptes, son solde, ses dernières opérations et ses notifications. C'est la page d'accueil après connexion pour tous les rôles. | `/dashboard.xhtml` (`DashboardBean.init()`) | `web/DashboardBean.java` | Crawl ADMIN depth=1, CONSEILLER depth=0, CLIENT depth=0 | Statique+Dynamique | Actif | |
| FEAT-003 | Effectuer un virement | Écran | Le client saisit un montant, un compte source et un bénéficiaire pour émettre un virement. La transaction est soumise à un contrôle anti-fraude en temps réel avant d'être traitée de façon asynchrone. | `/virement.xhtml` (`VirementBean.executeVirement()`) | `web/VirementBean.java` | Crawl CLIENT depth=1 ; ejb/TransferService.java | Statique+Dynamique | Actif | |
| FEAT-004 | Virements programmés | Écran | Le client programme un virement récurrent (date, fréquence, montant) qui s'exécute automatiquement jusqu'à annulation. Il peut consulter, modifier ou supprimer ses virements programmés depuis cet écran. | `/virement-programme.xhtml` (`RecurringTransferBean`) | `web/RecurringTransferBean.java` | Crawl CLIENT depth=1 ; ejb/RecurringTransferService.java | Statique+Dynamique | Actif | |
| FEAT-005 | Relevé de compte | Écran | Le client consulte l'historique de ses opérations bancaires, filtrable par période et par type. Il peut télécharger son relevé au format PDF. | `/releve.xhtml` (`ReleveBean`) | `web/ReleveBean.java` | Crawl CLIENT depth=1 | Statique+Dynamique | Actif | |
| FEAT-006 | Budget et catégories de dépenses | Écran | Le client visualise ses dépenses regroupées par catégorie (alimentation, loisirs, transport…) et suit son budget mensuel. Il peut ajuster les catégories et fixer des plafonds de dépenses. | `/budget.xhtml` (`BudgetBean`) | `web/BudgetBean.java` | Crawl CLIENT depth=1 ; ejb/BudgetService.java | Statique+Dynamique | Actif | |
| FEAT-007 | Demande et suivi de prêts | Écran | Le client soumet une demande de prêt en indiquant le montant et la durée souhaités. Il suit l'état de sa demande (en attente, approuvée, refusée) depuis cet écran. | `/pret.xhtml` (`LoanBean`) | `web/LoanBean.java` | Crawl CLIENT depth=1 ; ejb/LoanService.java (4 commits/an — zone active) | Statique+Dynamique | Actif | |
| FEAT-008 | Documents (relevés, attestations, IFU) | Écran | Le client accède à ses documents officiels (relevés de compte, attestations bancaires, imprimé fiscal unique) et les télécharge en PDF. | `/documents.xhtml` (`DocumentBean`) | `web/DocumentBean.java` | Crawl CLIENT depth=1 ; ejb/DocumentService.java (4 commits/an — zone active) | Statique+Dynamique | Actif | |
| FEAT-009 | Bourse et watchlist | Écran | Le client consulte les cotations boursières en temps réel (via AlphaVantage) et gère sa liste de valeurs surveillées. Il peut ajouter ou retirer des symboles de sa watchlist. | `/bourse.xhtml` (`BourseBean`) | `bourse/BourseBean.java` | Crawl CLIENT depth=1 ; bourse/AlphaVantageService.java | Statique+Dynamique | Actif | |
| FEAT-010 | Chat avec le conseiller | Écran | Le client échange des messages avec son conseiller bancaire et consulte l'historique de la conversation. Les messages sont envoyés et reçus en temps différé. | `/conseiller.xhtml` (`ConseillerBean`) | `conseiller/` | Crawl CLIENT depth=1 | Statique+Dynamique | Actif | |
| FEAT-011 | Catalogue produits bancaires | Écran | Le client parcourt les produits bancaires proposés par l'établissement (comptes, livrets, assurances…). Il s'agit d'une présentation informative sans souscription en ligne. | `/produits.xhtml` (`BankProductService`) | `ejb/BankProductService.java` | Crawl CLIENT depth=1 | Statique+Dynamique | Actif | |
| FEAT-012 | Comptes Tiers — Open Banking Powens | Écran | Le client connecte ses comptes détenus dans d'autres banques via le service Powens. Une fois la liaison établie, ses comptes externes apparaissent dans son espace avec leur solde et leurs opérations. | `/comptes-tiers.xhtml` (`PowensBean.connectBank()`) | `powens/PowensBean.java` | Crawl CLIENT depth=1 ; powens/PowensService.java | Statique+Dynamique | Actif | |
| FEAT-013 | Gestion des bénéficiaires externes | Écran | Le client ajoute, modifie ou supprime des bénéficiaires externes (IBAN) vers lesquels il peut effectuer des virements. Chaque bénéficiaire est identifié par un libellé et un IBAN. | `/beneficiaires.xhtml` (`BeneficiaireBean`) | `web/BeneficiaireBean.java` | Crawl CLIENT depth=1 ; ejb/ExternalBeneficiaryService.java | Statique+Dynamique | Actif | |
| FEAT-014 | Callback OAuth Powens | Écran | Cette page reçoit le retour de Powens après une connexion Open Banking et enregistre la liaison bancaire. Elle n'est pas accessible depuis le menu : elle n'est atteinte que par redirection OAuth depuis le site Powens. | `/powens-callback.xhtml` (`PowensCallbackBean.processCallback()`) | `powens/PowensCallbackBean.java` | paramètres connection_id, code, error ; page exclue de l'authentification | Statique | Actif | `ui:condition` — page accessible sans authentification (AppFilter bypass) mais uniquement atteinte par redirect OAuth depuis Powens Webview ; non linkée depuis le menu |

---

## Features CONSEILLER (rôles : CONSEILLER, ADMIN)

| ID | Nom métier | Interface | Résumé | Point d'entrée | Fichier principal | Preuve | Source | Statut | Pourquoi source unique |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| FEAT-015 | Espace Conseiller — Portefeuille clients | Écran | Le conseiller accède à la liste de ses clients et consulte leur situation bancaire. Cette page est réservée aux rôles CONSEILLER et ADMIN. | `/conseiller-workspace.xhtml` (`ConseillerWorkspaceBean`) | `web/ConseillerWorkspaceBean.java` | Crawl CONSEILLER depth=0, ADMIN depth=1 ; restriction CONSEILLER ou ADMIN | Statique+Dynamique | Actif | |

---

## Features ADMIN (rôle : ADMIN)

| ID | Nom métier | Interface | Résumé | Point d'entrée | Fichier principal | Preuve | Source | Statut | Pourquoi source unique |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| FEAT-016 | Administration — Gestion globale | Écran | L'administrateur pilote l'ensemble de l'application depuis un espace centralisé : validation des prêts, activation ou désactivation des clients, gestion des alertes fraude, suivi des batchs et mise à jour du catalogue produits. | `/admin.xhtml` (`AdminBean`, `FraudAlertBean`, `BatchAdminBean`) | `web/AdminBean.java` | Crawl ADMIN depth=0 ; restriction ADMIN ; contient : pipeline prêts, gestion clients, alertes fraude, suivi batchs, catalogue produits | Statique+Dynamique | Actif | |

---

## Intégrations externes

| ID | Nom métier | Interface | Résumé | Point d'entrée | Déclencheur | Fichier principal | Preuve | Source | Statut | Pourquoi source unique |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| FEAT-017 | Téléchargement documents PDF | API | Endpoint HTTP qui retourne un fichier PDF (relevé, attestation, IFU) à partir d'un identifiant de document. Il est appelé par les liens de la page Documents et accessible à tout rôle. | `GET /docs/download` | Liens depuis documents.xhtml | `web/DocumentDownloadServlet.java` | Sonde HTTP : FEAT-017 → 200 (mapped) — tous rôles + anonyme → 200 (note : servlet hors du filtre AppFilter, accessible sans session) | Statique+Dynamique | Actif | |

---

## Batchs et jobs invisibles (sans UI directe)

| ID | Nom métier | Interface | Résumé | Effet | Déclencheur | Fichier principal | Preuve | Source | Statut | Pourquoi source unique |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| FEAT-018 | Calcul des intérêts nocturnes | Batch | Chaque nuit à 2h, le système calcule les intérêts sur les comptes éligibles et les inscrit en base. Le résultat est visible dans l'espace d'administration. | Aucun | `@Schedule(hour=2)` — EJB Timer JVM | `ejb/NightlyBatchService.java` | `@Schedule(hour="2", minute="0")` ; journalisé dans BATCH_JOB_LOGS, visible dans admin.xhtml | Statique | Actif | Route batch/cron sans interface UI |
| FEAT-019 | Détection fraude nocturne par batch | Batch | Chaque nuit à 2h30, le système analyse les opérations de la journée à la recherche de patterns frauduleux et génère des alertes consultables par l'administrateur. | Aucun | `@Schedule(hour=2:30)` — EJB Timer JVM | `ejb/NightlyBatchService.java` | `@Schedule(hour="2", minute="30")` ; crée des entrées dans FRAUD_ALERTS | Statique | Actif | Route batch/cron sans interface UI |
| FEAT-020 | Relevés mensuels — batch stub (dette-legacy) | Batch | Le 1er de chaque mois, un traitement compte les clients actifs et journalise le résultat. Ce batch est un stub de dette technique : il ne génère pas encore les relevés PDF attendus. | Aucun | `@Schedule(hour=3, dayOfMonth=1)` — 1er de chaque mois — EJB Timer JVM | `ejb/NightlyBatchService.java` | code stub : compte les clients actifs et log le résultat, n'appelle pas DocumentService et ne génère aucun fichier — dette-legacy | Statique | Actif | Route batch/cron sans interface UI |
| FEAT-021 | Exécution des virements programmés | Batch | Toutes les 5 minutes, le système déclenche les virements récurrents dont la date d'exécution est atteinte et met à jour leur prochaine échéance. | Aucun | `@Schedule(minute="*/5")` — toutes les 5 min — EJB Timer JVM | `ejb/RecurringTransferScheduler.java` | `@Schedule(minute="*/5", hour="*")` | Statique | Actif | Route batch/cron sans interface UI |
| FEAT-022 | Surveillance soldes faibles et génération d'alertes | Batch | Toutes les 30 minutes, le système détecte les comptes dont le solde passe sous un seuil et crée une notification interne pour le client concerné. | Aucun | `@Schedule(minute="*/30")` — toutes les 30 min — EJB Timer JVM | `ejb/NotificationService.java` | `@Schedule(hour="*", minute="*/30")` | Statique | Actif | Route batch/cron sans interface UI |

---

## Transversal

| ID | Nom métier | Interface | Résumé | Effet | Déclencheur | Fichier principal | Preuve | Source | Statut | Pourquoi source unique |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| FEAT-023 | Traitement asynchrone des virements (JMS) | Transversal | Lorsqu'un virement est soumis, il est placé dans une file JMS et traité de manière asynchrone. Ce mécanisme découple la soumission du traitement effectif et garantit la robustesse en cas de pic de charge. | Aucun | Soumission d'un virement (VirementBean.executeVirement → JMS Queue virements) | `mq/VirementProcessorMDB.java` | `@MessageDriven` appel inconditionnel | Statique | Actif | Échange interne JMS sans écran propre |
| FEAT-024 | Traitement asynchrone approbation prêts (JMS) | Transversal | Lorsqu'un administrateur approuve un prêt, la décision est envoyée dans une file JMS et traitée de manière asynchrone. Ce mécanisme garantit que l'approbation est enregistrée même sous charge. | Aucun | Approbation d'un prêt depuis admin.xhtml (AdminBean.approuverPret → JMS) | `mq/LoanApprovalMDB.java` | `@MessageDriven` appel LoanService qui envoie le message | Statique | Actif | Échange interne JMS sans écran propre |
| FEAT-025 | Système de notifications internes | Transversal | Ce composant crée une notification interne à chaque événement significatif (virement exécuté, statut de prêt modifié, solde faible). Les notifications sont affichées sur le tableau de bord de l'utilisateur concerné. | Aucun | Exécution d'un virement, changement de statut de prêt, alerte solde faible | `ejb/NotificationService.java` | méthodes `createTransactionNotification`, `createLoanNotification`, `notifyLowBalance` ; notifications affichées sur dashboard.xhtml via DashboardBean | Statique | Actif | Préoccupation transversale — l'affichage est intégré au tableau de bord (FEAT-002) |
| FEAT-026 | Audit trail (journalisation des actions) | Transversal | Chaque action sensible (virement, activation ou désactivation d'un client) est enregistrée dans un journal d'audit horodaté. Ce journal assure la traçabilité réglementaire des opérations. | Aucun | Toute action utilisateur (virement, activation/désactivation client) | `ejb/AuditService.java` | AuditService.java ; appelé depuis | Statique | Actif | Préoccupation transversale sans point d'entrée propre |
| FEAT-027 | Détection de fraude en temps réel | Transversal | Avant chaque virement, le système évalue le niveau de risque de la transaction et peut la bloquer si elle est suspecte. Les alertes générées sont consultables par l'administrateur. | Aucun | Soumission d'un virement (VirementBean.executeVirement, avant JMS) | `ejb/FraudDetectionService.java` | import FraudDetectionService ; FRAUD_ALERTS table | Statique | Actif | Préoccupation transversale — contrôle appliqué à chaque virement |
| FEAT-028 | Intégration Powens (Open Banking — agrégation) | Transversal | Ce composant gère la communication avec l'API Powens pour initier et finaliser la connexion à une banque externe. Il est utilisé par la page Comptes Tiers (FEAT-012) et le Callback OAuth (FEAT-014). | Aucun | Navigation vers comptes-tiers.xhtml, clic "Connecter ma banque" | `powens/PowensService.java` | appel Powens Webview ; appel HttpURLConnection | Statique | Actif | Aucun écran propre — fonctionnalité intégrée dans FEAT-012 |
| FEAT-029 | Intégration AlphaVantage (cotations boursières) | Transversal | Ce composant interroge l'API AlphaVantage pour récupérer les cours boursiers en temps réel à la demande. Il est utilisé exclusivement par la page Bourse et watchlist (FEAT-009). | Aucun | Navigation vers bourse.xhtml, sélection d'un symbole | `bourse/AlphaVantageService.java` | import AlphaVantageService sélection symbole | Statique | Actif | Aucun écran propre — fonctionnalité intégrée dans FEAT-009 |
| FEAT-030 | Contrôle d'accès RBAC | Transversal | Ce filtre vérifie à chaque requête que l'utilisateur a le rôle requis pour accéder à la page demandée. Les pages réservées (CONSEILLER, ADMIN) renvoient une erreur si le rôle est insuffisant. | Aucun | Toute requête HTTP (filtre Web + templates JSF) | `web/AppFilter.java` | `@WebFilter(urlPatterns={"*.xhtml"})` `@Named` — utilisé dans releve.xhtml, comptes-tiers.xhtml, conseiller.xhtml, documents.xhtml, bourse.xhtml | Statique | Actif | Préoccupation transversale sans point d'entrée propre |
| FEAT-031 | Analytique client et segmentation (code mort probable) | Transversal | | Aucun | Aucun appelant identifié | `ejb/CustomerAnalyticsService.java` | méthodes `computeCustomerProfile`, `computeRiskScore`, `generateRecommendations`, `computeMonthlyStats` — aucun `@EJB` ni import trouvé dans le reste du code | Statique | Mort | Aucun appelant : service inatteignable en production — proposé au registre des fonctionnalités mortes |
| FEAT-032 | Bean gestion notifications UI (code mort probable) | Transversal | | Aucun | Aucun appelant identifié | `web/NotificationBean.java` | `@Named @SessionScoped` — méthodes `markRead`, `markAllRead`, `refreshNotifications` — aucune référence `#{notificationBean` dans les 16 fichiers xhtml ; l'affichage des notifs passe par DashboardBean | Statique | Mort | Aucun appelant xhtml : bean CDI inatteignable en production — proposé au registre des fonctionnalités mortes |

---

## Résumé

| Catégorie | Nb features | Actif | À confirmer | Mort | Conservé |
| --- | --- | --- | --- | --- | --- |
| Features CLIENT (tous les rôles : CLIENT, CONSEILLER, ADMIN) | 14 | 14 | 0 | 0 | 0 |
| Features CONSEILLER (rôles : CONSEILLER, ADMIN) | 1 | 1 | 0 | 0 | 0 |
| Features ADMIN (rôle : ADMIN) | 1 | 1 | 0 | 0 | 0 |
| Intégrations externes | 1 | 1 | 0 | 0 | 0 |
| Batchs et jobs invisibles (sans UI directe) | 5 | 5 | 0 | 0 | 0 |
| Transversal | 10 | 8 | 0 | 2 | 0 |
| **Total** | **32** | **30** | **0** | **2** | **0** |

**Par source :**

| Source | Nb | Notes |
| --- | --- | --- |
| Statique+Dynamique | 17 | Niveau de confiance maximum |
| Statique seul | 15 | Non atteint par le crawl — voir Pourquoi source unique |
| Dynamique seul | 0 | Toutes les features crawlées ont un backing bean ou servlet confirmé |

**Couverture des écrans** : 14/16 (87,5 %) — FEAT-001 (`ui:lien` — page connexion), FEAT-014 (`ui:condition` — callback OAuth) non atteintes
**API** : 1 | **Batch** : 5 (avec effet : aucun — FEAT-020 est un stub dette-legacy) | **Transversal** : 10 (avec effet : aucune)
