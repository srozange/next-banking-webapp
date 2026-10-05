# CONCERNS

## Intention

Aucune documentation de dette technique ou de concerns connues présente dans le dépôt.

## Réalité

### Marqueurs dans le code source

Total (fichiers sources Java uniquement, hors `.arch-eval-bck`) : **28 TODO, 14 FIXME, 4 HACK** = 46 marqueurs.

Fichiers les plus affectés :

| Fichier | TODO | FIXME | HACK | Thèmes |
|---|---|---|---|---|
| `ejb/FraudDetectionService.java` | 11 | 7 | 3 | Seuils hardcodés, cache statique, timezone, IBAN dans logs, SEPA validator dupliqué |
| `ejb/NotificationService.java` | 4 | 4 | 1 | Locking optimiste absent, O(n) fetch clients, spam hardcodé |
| `ejb/DocumentService.java` | 5 | 2 | 0 | Service trop large (PDF + repository), métadonnées banque hardcodées |
| `web/VirementBean.java` | 3 | 1 | 0 | Logique métier dans le bean (JIRA-9200, 9201, 9202) |
| `ejb/CustomerAnalyticsService.java` | 3 | 2 | 0 | Logique dupliquée avec RiskProfile, absence de moteur de règles |
| `ejb/NotificationService.java` | 2 | 2 | 1 | Seuil anti-spam hardcodé, bulk UPDATE absent |

### Concerns critiques

**RGPD / Sécurité des données :**
- IBAN complet loggé (JIRA-9400, JIRA-9401) — FIXME RGPD
- `bourse/AlphaVantageService.java` — Clé API AlphaVantage hardcodée en fallback (`FALLBACK_API_KEY`) — JIRA-9300

**Scalabilité :**
- Fetch de TOUS les clients pour vérifier les notifications (JIRA-7200) — FIXME : ne pas scaler
- Mise à jour O(n) des notifications (JIRA-7102)
- COUNT via fetch d'entités complètes au lieu de requête COUNT() (JIRA-9001)
- Cache statique en mémoire d'instance (static mutable) au lieu de Redis (JIRA-8910)

**Logique métier déplacée :**
- Contrôles de transfert dans le bean UI plutôt que dans `TransferService` (JIRA-9200)
- Validation SEPA dupliquée (SEPA validator non réutilisé — JIRA-4102)

**Concurrence :**
- Mark-as-read sans locking optimiste → race condition possible (JIRA-7101)

**Configuration hardcodée :**
- Seuils fraude hardcodés (montant, velocité, fenêtre temps) — FRAUD-001
- SIRET banque hardcodé (`123 456 789 00012`) — JIRA-8021
- Intervalle anti-spam hardcodé — JIRA-7001

**Timezone :**
- `Calendar.getInstance()` utilise la timezone JVM par défaut au lieu de la timezone client (JIRA-7703)

### Hotspots git (90 jours)

Les fichiers du corpus retro (`retro/` et `retro/00-global/`) dominent le churn ; dans les sources applicatives, aucun hotspot significatif identifié sur la période — le projet a peu évolué récemment.

## Écarts

- Aucune documentation de dette technique ne permet de comparer intention et réalité.
- Les JIRA référencés dans les commentaires (`JIRA-xxxx`, `FRAUD-001`) sont des tickets de dette connus mais non résolus — leur statut réel dans l'outil de suivi est inaccessible ici.
