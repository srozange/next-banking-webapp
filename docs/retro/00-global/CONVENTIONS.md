# CONVENTIONS

## Intention

Aucune documentation de conventions (style guide, coding standards) présente dans le dépôt.

## Réalité

**Nommage :**

| Domaine | Convention observée | Exemples |
|---|---|---|
| Classes EJB | Anglais, suffixe `Service` | `TransferService`, `AccountService`, `FraudDetectionService` |
| Classes backing bean | Français ou franglais, suffixe `Bean` | `VirementBean`, `DashboardBean`, `BeneficiaireBean`, `LoanBean` |
| Classes entités | Anglais | `BankAccount`, `Transfer`, `Customer`, `FraudAlert` |
| Classes MQ | Anglais, suffixe `MDB` (consumer) ou `Producer` | `VirementProcessorMDB`, `VirementProducer`, `LoanApprovalMDB` |
| Méthodes/champs | Camelcase, mélange français/anglais | `effectuerVirement()`, `getBalance()`, `compteSource`, `targetIban` |
| Vues xhtml | Français, kebab-case | `virement.xhtml`, `virement-programme.xhtml`, `conseiller-workspace.xhtml` |
| Tables/colonnes JPA | Conventions non vérifiées ici ; voir `retro-data` |

**Formatage :**

- Indentation : espaces (pas de fichier `.editorconfig` détecté)
- Dates affichées : `dd/MM/yyyy` (pattern JSF `f:convertDateTime`) — preuve :
- Dates saisies (formulaire) : `yyyy-MM-dd` (champ input HTML) — preuve :
- Montants affichés : virgule décimale, symbole EUR — preuve : (`100,00`)
- Logs : Log4j2, messages en anglais dans les services, français dans les beans — pas de pattern unifié

**Patterns répétés :**

- Chaque backing bean déclare `private static final Logger LOG = LogManager.getLogger(…)` 
- Les EJBs `@Stateless` injectent `@PersistenceContext EntityManager em`
- Les backing beans injectent les EJBs par champ `@EJB`
- Gestion des exceptions : `try/catch` locaux dans chaque méthode, sans handler global
- Validation JSF : `required="true"` + validateurs inline, parfois doublonnée en Java

**Ce qui est exclu :**
- Aucun pattern depuis `dist/` (absent du projet)
- Aucun framework de style CSS communautaire — CSS inline dans les `.xhtml`

## Écarts

- Mélange systématique de termes français et anglais dans le même fichier source.
- Aucune convention unifiée de nommage des packages : `ejb/`, `web/`, `mq/` côté technique mais `powens/`, `bourse/`, `conseiller/` côté domaine.
- Champ injection `@EJB` utilisé partout (jamais d'injection constructeur).
- Logging des données sensibles non masquées (IBAN complet) — voir CONCERNS.md.
