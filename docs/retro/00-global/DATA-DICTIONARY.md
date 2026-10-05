# DATA-DICTIONARY

_Généré par retro-data depuis l'introspection SQL._

## PUBLIC.AUDIT_LOGS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_B665F007_3B9A_4C71_9998_2F82756AF9A4) |  |
| ACTION | 12 | NO |  |  |
| ACTION_DATE | 93 | NO |  |  |
| DETAILS | 12 | YES |  |  |
| IPADDRESS | 12 | YES |  |  |
| USERNAME | 12 | NO |  |  |

## PUBLIC.BANK_ACCOUNTS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_A0914657_40B1_4D85_85AF_842A3EC2ED58) |  |
| ACCOUNT_NUMBER | 12 | NO |  |  |
| ACCOUNT_TYPE | 12 | NO |  | ⚠️ enum candidate |
| BALANCE | 3 | NO |  |  |
| CUSTOMER_ID | -5 | NO |  |  |

## PUBLIC.BANK_PRODUCTS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| PRODUCT_TYPE | 12 | NO |  | ⚠️ enum candidate |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_E32E6BF4_BE24_4C19_BC44_BF933D2F5D3B) |  |
| ACTIVE | 16 | NO |  |  |
| CREATED_AT | 93 | YES |  |  |
| CURRENCY | 12 | YES |  |  |
| DESCRIPTION | 12 | YES |  |  |
| INTEREST_RATE | 3 | YES |  |  |
| MAX_AMOUNT | 3 | YES |  |  |
| MIN_AMOUNT | 3 | YES |  |  |
| NAME | 12 | NO |  |  |

## PUBLIC.BATCH_JOB_LOGS

_Table technique (Spring Batch) : exclue des énumérations et des données de référence._

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_844B4498_0E0D_464B_895A_25B8221FF647) |  |
| DETAILS | 12 | YES |  |  |
| END_TIME | 93 | YES |  |  |
| JOB_NAME | 12 | NO |  |  |
| RECORDS_PROCESSED | 4 | YES |  |  |
| START_TIME | 93 | YES |  |  |
| STATUS | 12 | YES |  |  |

## PUBLIC.BUDGETS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_2636DB3A_FF48_4C3C_A262_CCBD468245DD) |  |
| CATEGORY | 12 | NO |  | ⚠️ enum candidate |
| PLAFOND | 3 | NO |  |  |
| YEAR_MONTH | 12 | NO |  |  |
| CUSTOMER_ID | -5 | NO |  |  |

## PUBLIC.CREDIT_PRODUCTS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| MAX_DURATION_MONTHS | 4 | YES |  |  |
| MAX_LTV | 3 | YES |  |  |
| REQUIRES_INSURANCE | 16 | YES |  |  |
| ID | -5 | NO |  |  |

## PUBLIC.CREDIT_SCORES

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_19910885_1D07_4DDE_8F41_39196A5172D6) |  |
| CALCULATED_AT | 93 | YES |  |  |
| CATEGORY | 12 | NO |  | ⚠️ enum candidate |
| CREDIT_AGE_MONTHS | 4 | YES |  |  |
| PAYMENT_HISTORY_PCT | 4 | YES |  |  |
| SCORE | 4 | NO |  |  |
| TOTAL_DEBT | 3 | YES |  |  |
| CUSTOMER_ID | -5 | YES |  |  |

## PUBLIC.CUSTOMERS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_30E7B8AC_612D_4A44_8B90_F11EEC3C79CB) |  |
| ACTIVE | 16 | NO |  |  |
| ADVISOR_SINCE | 91 | YES |  |  |
| EMAIL | 12 | NO |  |  |
| FULL_NAME | 12 | NO |  |  |
| PASSWORD | 12 | NO |  |  |
| PHONE | 12 | YES |  |  |
| ROLE | 12 | NO |  |  |
| USERNAME | 12 | NO |  |  |
| ADVISOR_ID | -5 | YES |  |  |

## PUBLIC.DOCUMENTS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_21C2FF0B_AD56_4791_AD2F_18E415BAD5F1) |  |
| CHECKSUM | 12 | YES |  |  |
| CUSTOMER_ID | -5 | NO |  |  |
| DOCUMENT_TYPE | 12 | NO |  | ⚠️ enum candidate |
| DOWNLOAD_COUNT | 4 | NO |  |  |
| FILE_SIZE_BYTES | -5 | NO |  |  |
| FILENAME | 12 | NO |  |  |
| GENERATED_AT | 93 | NO |  |  |
| MIME_TYPE | 12 | NO |  | ⚠️ enum candidate |
| PARAMETRES | 12 | YES |  |  |

## PUBLIC.EXTERNAL_BENEFICIARIES

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_0F2BF529_1145_4365_8D76_6EDB33CCFD90) |  |
| ACTIVE | 16 | YES |  |  |
| BANK | 12 | YES |  |  |
| BIC | 12 | YES |  |  |
| CREATED_AT | 91 | YES |  |  |
| HOLDER_NAME | 12 | NO |  |  |
| IBAN | 12 | NO |  |  |
| LABEL | 12 | NO |  |  |
| SEPA_ZONE | 16 | YES |  |  |
| CUSTOMER_ID | -5 | NO |  |  |

## PUBLIC.FRAUD_ALERTS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_983E2C60_1CD7_4280_8F6D_6E6883B2A699) |  |
| CREATED_AT | 93 | NO |  |  |
| CUSTOMER_ID | -5 | NO |  |  |
| DESCRIPTION | 12 | YES |  |  |
| REVIEWED_AT | 93 | YES |  |  |
| REVIEWED_BY | 12 | YES |  |  |
| RISK_SCORE | 4 | NO |  |  |
| RULE | 12 | NO |  |  |
| STATUS | 12 | NO |  | ⚠️ enum candidate |
| TRANSACTION_ID | -5 | YES |  |  |

## PUBLIC.INVESTMENT_PRODUCTS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| EXPECTED_RETURN | 3 | YES |  |  |
| MARKET_SECTOR | 12 | YES |  |  |
| RISK_LEVEL | 4 | YES |  |  |
| ID | -5 | NO |  |  |

## PUBLIC.LOANS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_5963E416_D076_48A1_8F0C_C0DC307FF988) |  |
| COUT_TOTAL | 3 | NO |  |  |
| DATE_DEBUT | 91 | NO |  |  |
| DATE_FIN | 91 | NO |  |  |
| DUREE_MOIS | 4 | NO |  |  |
| MENSUALITE | 3 | NO |  |  |
| MONTANT | 3 | NO |  |  |
| MOTIF | 12 | YES |  |  |
| STATUS | 12 | NO |  | ⚠️ enum candidate |
| TAUX_ANNUEL | 3 | NO |  |  |
| TOTAL_INTERETS | 3 | NO |  |  |
| TYPE | 12 | NO |  | ⚠️ enum candidate |
| CUSTOMER_ID | -5 | NO |  |  |

## PUBLIC.LOAN_REPAYMENTS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_1B241868_2E18_409E_BCD0_0C9F4DA91362) |  |
| CAPITAL_RESTANT | 3 | NO |  |  |
| DATE_ECHEANCE | 91 | NO |  |  |
| DATE_PAIEMENT | 93 | YES |  |  |
| MONTANT_CAPITAL | 3 | NO |  |  |
| MONTANT_INTERETS | 3 | NO |  |  |
| MONTANT_TOTAL | 3 | NO |  |  |
| NUMERO_ECHEANCE | 4 | NO |  |  |
| PAYE | 16 | NO |  |  |
| LOAN_ID | -5 | NO |  |  |

## PUBLIC.NOTIFICATIONS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| NOTIF_DISC | 12 | NO |  |  |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_A9E6875D_13E9_4CFD_8648_8C541926543A) |  |
| CREATED_AT | 93 | NO |  |  |
| CUSTOMER_ID | -5 | NO |  |  |
| MESSAGE | 2005 | YES |  |  |
| PRIORITY | 12 | NO |  |  |
| READ | 16 | NO |  |  |
| READ_AT | 93 | YES |  |  |
| TITLE | 12 | NO |  |  |
| TYPE | 12 | NO |  | ⚠️ enum candidate |
| ACTION_URL | 12 | YES |  |  |
| SYSTEM_CODE | 12 | YES |  | ⚠️ enum candidate |
| ACCOUNT_NUMBER | 12 | YES |  |  |
| TRANSACTION_AMOUNT | 3 | YES |  |  |
| LOAN_ID | -5 | YES |  |  |
| LOAN_STATUS | 12 | YES |  | ⚠️ enum candidate |

## PUBLIC.RECURRING_TRANSFERS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_76E722B6_9196_43CB_9906_4DAAE5254B95) |  |
| ACTIF | 16 | NO |  |  |
| CREATED_DATE | 93 | NO |  |  |
| EXECUTION_COUNT | 4 | NO |  |  |
| FREQUENCE | 12 | NO |  |  |
| LAST_EXECUTED | 93 | YES |  |  |
| MONTANT | 3 | NO |  |  |
| MOTIF | 12 | YES |  |  |
| PROCHAIN_EXECUTION | 93 | NO |  |  |
| TARGET_ACCOUNT_NUMBER | 12 | NO |  |  |
| TARGET_LABEL | 12 | NO |  |  |
| CUSTOMER_ID | -5 | NO |  |  |
| SOURCE_ACCOUNT_ID | -5 | NO |  |  |

## PUBLIC.RISK_PROFILES

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_30FB28A1_6662_456B_93AC_E3007CCF7AA8) |  |
| CREATED_AT | 93 | YES |  |  |
| MONTHLY_EXPENSES | 3 | YES |  |  |
| MONTHLY_INCOME | 3 | YES |  |  |
| PATRIMOINE | 3 | YES |  |  |
| RISK_LEVEL | 12 | NO |  |  |
| UPDATED_AT | 93 | YES |  |  |
| CUSTOMER_ID | -5 | YES |  |  |

## PUBLIC.SAVINGS_PRODUCTS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| CAPITAL_GUARANTEED | 16 | YES |  |  |
| MAX_DURATION_MONTHS | 4 | YES |  |  |
| MIN_OPENING_DEPOSIT | 3 | YES |  |  |
| ID | -5 | NO |  |  |

## PUBLIC.TRANSACTIONS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_E88C7EAB_4BC3_4D78_A768_91AAFBA75B0F) |  |
| AMOUNT | 3 | NO |  |  |
| BALANCE_AFTER | 3 | YES |  |  |
| DESCRIPTION | 12 | NO |  |  |
| TRANSACTION_DATE | 93 | NO |  |  |
| TYPE | 12 | NO |  | ⚠️ enum candidate |
| ACCOUNT_ID | -5 | NO |  |  |

## PUBLIC.TRANSFERS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_710749E7_1AE0_48DC_9C13_2D77789CE859) |  |
| AMOUNT | 3 | NO |  |  |
| MOTIF | 12 | YES |  |  |
| STATUS | 12 | NO |  | ⚠️ enum candidate |
| TRANSFER_DATE | 93 | NO |  |  |
| SOURCE_ACCOUNT_ID | -5 | NO |  |  |
| TARGET_ACCOUNT_ID | -5 | NO |  |  |

## PUBLIC.WATCHLIST_ITEMS

| Colonne | Type | Nullable | Défaut | Notes |
| --- | --- | --- | --- | --- |
| ID | -5 | NO | (NEXT VALUE FOR PUBLIC.SYSTEM_SEQUENCE_324F28C9_D495_43D2_AF5C_0C2C79C8B674) |  |
| CURRENCY | 12 | NO |  |  |
| MARKET | 12 | NO |  |  |
| NAME | 12 | NO |  |  |
| SYMBOL | 12 | NO |  |  |
