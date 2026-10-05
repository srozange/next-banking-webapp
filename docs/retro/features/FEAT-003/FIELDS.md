# FIELDS — FEAT-003 Effectuer un virement

_Source : crawl MCP Playwright (rôle CLIENT, user1) — retro-ui 2026-10-03._
_Snapshot de référence : `screens/CLIENT-chargé.snap`._

## Formulaire principal — `/virement.xhtml`

| Champ | Type | Libellé | Obligatoire | Valeur par défaut | Contraintes / Notes |
|---|---|---|---|---|---|
| `compteSource` | combobox | Compte source | Oui | `-- Choisir un compte --` | Options dynamiques chargées depuis les comptes du client connecté. CONSEILLER et ADMIN voient une liste vide (aucun compte propre en base). |
| `compteDestinataire` | combobox | Compte destinataire | Oui | `-- Choisir un compte --` | Mêmes comptes que `compteSource` ; libellé sans solde. Doit être différent de `compteSource` (validation serveur). |
| `montant` | textbox | Montant (€) | Oui | _(vide)_ | Format attendu : décimale virgule (ex : `100,00`). Seuil fraude : > 5 000 € → ALERTE ; > 10 000 € → CONFIRMATION + FRAUDE. Conversion implicite virgule/point lors du POST (anomalie affichage : `9999,00` → `999,900.00`). |
| `motif` | textbox | Motif | Non | _(vide)_ | Champ libre. Transmis dans la notification JMS publiée sur `queue/virements` (ActiveMQ Artemis) ; visible dans le dashboard. |

## Action

| Bouton | Libellé | Effet |
|---|---|---|
| `executerVirement` | Exécuter le virement | POST `/virement.xhtml` → `VirementBean.executeVirement()` → `TransferService` (EJB CMT) → `VirementProducer` → `queue/virements` (Artemis) → `VirementProcessorMDB`. **Pas de modale de confirmation avant exécution.** Succès → redirect vers `/dashboard.xhtml`. |

## Validations observées

| Règle | Message affiché | Déclencheur |
|---|---|---|
| Comptes identiques | `Le compte source et destination doivent être différents` | Soumission avec même valeur dans les deux combobox |
| Solde insuffisant | `Erreur lors du virement : java.lang.IllegalStateException: Solde insuffisant` | Montant > solde du compte source — stack trace Java exposée (dette-legacy) |

## Activations dynamiques

Aucune : tous les champs sont présents dans le DOM initial. Les options des combobox sont chargées au chargement de la page (ViewScoped JSF), non rechargées dynamiquement.

## Comportement par rôle

| Rôle | Accès | Comptes disponibles | Remarques |
|---|---|---|---|
| CLIENT (`user1`) | Oui | COURANT FR76-0001-0001, EPARGNE FR76-0001-0002 | Formulaire utilisable |
| CONSEILLER (`sophie`) | Oui | Aucun | Formulaire inutilisable — aucun compte propre en base |
| ADMIN (`admin`) | Oui | Aucun | Formulaire inutilisable — aucun compte propre en base |

## Infobulles et libellés complémentaires affichés

- Bandeau info : _« Système Legacy — Virement géré par EJB CMT (Container Managed Transaction) »_
- Bandeau info : _« Après validation, une notification sera publiée sur `queue/virements` (ActiveMQ Artemis) et visible dans le tableau de bord. »_
- Lien : _« Virement SEPA externe — Gérer mes bénéficiaires »_ → `/beneficiaires.xhtml`
- Indice champ montant : _« Ex : 100,00 — Saisir plus de 5 000 € pour tester l'alerte fraude »_
