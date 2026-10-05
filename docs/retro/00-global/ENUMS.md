# ENUMS

_Valeurs réelles extraites par SELECT DISTINCT — source la plus fiable._

> **Important** : les valeurs viennent de la seconde passe (`generate-sql-introspection.py
> --intermediate`, puis `parse-introspection-results.py --enum-values`). Compléter les
> libellés `TODO` ; une colonne sans valeur n'a pas encore été interrogée.

## ENUM-bank_accounts-account_type
**Table** : `PUBLIC.BANK_ACCOUNTS` — **Colonne** : `ACCOUNT_TYPE`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `COURANT` | | 4 |
| `EPARGNE` | | 3 |

## ENUM-bank_products-product_type
**Table** : `PUBLIC.BANK_PRODUCTS` — **Colonne** : `PRODUCT_TYPE`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `CREDIT` | | 4 |
| `INVESTMENT` | | 5 |
| `SAVINGS` | | 4 |

## ENUM-budgets-category
**Table** : `PUBLIC.BUDGETS` — **Colonne** : `CATEGORY`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `ABONNEMENTS` | | 1 |
| `ALIMENTATION` | | 1 |
| `LOGEMENT` | | 1 |
| `LOISIRS` | | 1 |
| `SANTE` | | 1 |
| `TRANSPORT` | | 1 |

## ENUM-credit_scores-category
**Table** : `PUBLIC.CREDIT_SCORES` — **Colonne** : `CATEGORY`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `EXCELLENT` | | 2 |
| `GOOD` | | 1 |
| `MEDIUM` | | 1 |

## ENUM-documents-document_type
**Table** : `PUBLIC.DOCUMENTS` — **Colonne** : `DOCUMENT_TYPE`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `ATTESTATION_SOLDE` | | 2 |
| `RELEVE_MENSUEL` | | 2 |
| `TABLEAU_AMORTISSEMENT` | | 1 |

## ENUM-documents-mime_type
**Table** : `PUBLIC.DOCUMENTS` — **Colonne** : `MIME_TYPE`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `application/pdf` | | 5 |

## ENUM-fraud_alerts-status
**Table** : `PUBLIC.FRAUD_ALERTS` — **Colonne** : `STATUS`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `CONFIRMED` | | 1 |
| `FALSE_POSITIVE` | | 1 |
| `OPEN` | | 3 |
| `REVIEWED` | | 1 |

## ENUM-loans-status
**Table** : `PUBLIC.LOANS` — **Colonne** : `STATUS`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `EN_COURS` | | 2 |

## ENUM-loans-type
**Table** : `PUBLIC.LOANS` — **Colonne** : `TYPE`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `AUTO` | | 1 |
| `TRAVAUX` | | 1 |

## ENUM-notifications-type
**Table** : `PUBLIC.NOTIFICATIONS` — **Colonne** : `TYPE`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `ACCOUNT_BALANCE` | | 1 |
| `FRAUD_ALERT` | | 1 |
| `LOAN_STATUS` | | 2 |
| `SYSTEM` | | 1 |
| `TRANSACTION_ALERT` | | 1 |

## ENUM-notifications-system_code
**Table** : `PUBLIC.NOTIFICATIONS` — **Colonne** : `SYSTEM_CODE`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `FRAUD-VELOCITY` | | 1 |
| `MAINT-20260601` | | 1 |

## ENUM-notifications-loan_status
**Table** : `PUBLIC.NOTIFICATIONS` — **Colonne** : `LOAN_STATUS`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `EN_COURS` | | 1 |

## ENUM-transactions-type
**Table** : `PUBLIC.TRANSACTIONS` — **Colonne** : `TYPE`

| Valeur | Libellé métier | Occurrences en base |
| --- | --- | --- |
| `CREDIT` | | 14 |
| `DEBIT` | | 34 |

## ENUM-transfers-status
**Table** : `PUBLIC.TRANSFERS` — **Colonne** : `STATUS`

| Valeur | Libellé métier | Notes |
| --- | --- | --- |
| | | Valeurs non extraites : seconde passe generate-sql-introspection.py --intermediate |
