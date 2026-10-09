-- Données de référence du service coeur-bancaire : les lignes de l'import.sql du legacy pour BANK_ACCOUNTS (7),
-- TRANSACTIONS (45), TRANSFERS (0), RECURRING_TRANSFERS (3), EXTERNAL_BENEFICIARIES (5), FRAUD_ALERTS (6) et
-- BUDGETS (6), identifiants, valeurs et dates repris tels quels.
-- Hors migrations : chargé dans une base déjà migrée par Flyway (make reset-data), jamais en production.
-- Dates : l'import n'a pas de fuseau ; elles sont lues à l'heure de Paris. Aucune date relative au jour du chargement.
-- Soldes : ceux de l'import, même s'ils ne sont pas cohérents avec les opérations (compte 1 = 2450.00, 2 = 15000.00).
-- Colonnes legacy sans équivalent cible : aucune (FRAUD_ALERTS.transaction_id, nul dans l'import, devient
-- virement_id nul). Les comptes tiennent chacun sur une ligne : scripts/generer-projections-reference.sh les lit.
-- BANK_ACCOUNTS (7) : account_number -> numero, account_type -> type, balance -> solde, customer_id -> client_id.
insert into comptes (id, numero, type, solde, client_id) values (1, 'FR76-0001-0001', 'COURANT', 2450.00, 1);
insert into comptes (id, numero, type, solde, client_id) values (2, 'FR76-0001-0002', 'EPARGNE', 15000.00, 1);
insert into comptes (id, numero, type, solde, client_id) values (3, 'FR76-0002-0001', 'COURANT', 3850.00, 2);
insert into comptes (id, numero, type, solde, client_id) values (4, 'FR76-0002-0002', 'EPARGNE', 22000.00, 2);
insert into comptes (id, numero, type, solde, client_id) values (5, 'FR76-0003-0001', 'COURANT', 1200.00, 3);
insert into comptes (id, numero, type, solde, client_id) values (6, 'FR76-0004-0001', 'COURANT', 5600.00, 4);
insert into comptes (id, numero, type, solde, client_id) values (7, 'FR76-0004-0002', 'EPARGNE', 48000.00, 4);
select setval(pg_get_serial_sequence('comptes', 'id'), (select max(id) from comptes));

-- TRANSACTIONS (45) : transaction_date -> date_operation ; virement_id nul (aucun virement dans l'import).
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (1, 1, timestamptz '2025-10-01 08:00:00 Europe/Paris', 'Virement salaire octobre', 'CREDIT', 3200.00, 3200.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (2, 1, timestamptz '2025-10-02 09:30:00 Europe/Paris', 'Loyer octobre', 'DEBIT', 850.00, 2350.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (3, 1, timestamptz '2025-10-05 10:00:00 Europe/Paris', 'EDF - Facture energie', 'DEBIT', 120.50, 2229.50);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (4, 1, timestamptz '2025-10-10 11:00:00 Europe/Paris', 'Virement vers FR76-0001-0002 - Epargne mensuelle', 'DEBIT', 200.00, 2029.50);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (5, 1, timestamptz '2025-10-12 14:20:00 Europe/Paris', 'Courses Carrefour', 'DEBIT', 65.30, 1964.20);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (6, 1, timestamptz '2025-10-15 08:00:00 Europe/Paris', 'Abonnement telephone', 'DEBIT', 45.00, 1919.20);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (7, 1, timestamptz '2025-10-18 17:00:00 Europe/Paris', 'Remboursement ami - Pierre', 'CREDIT', 200.00, 2119.20);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (8, 1, timestamptz '2025-10-20 10:15:00 Europe/Paris', 'Amazon - Commande 123', 'DEBIT', 89.90, 2029.30);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (9, 1, timestamptz '2025-10-22 20:30:00 Europe/Paris', 'Restaurant Le Bistrot', 'DEBIT', 30.00, 1999.30);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (10, 1, timestamptz '2025-10-25 09:00:00 Europe/Paris', 'Assurance voiture annuelle', 'DEBIT', 450.70, 1548.60);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (11, 2, timestamptz '2025-10-10 11:00:00 Europe/Paris', 'Virement depuis FR76-0001-0001 - Epargne mensuelle', 'CREDIT', 200.00, 14800.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (12, 2, timestamptz '2025-09-10 11:00:00 Europe/Paris', 'Virement epargne septembre', 'CREDIT', 200.00, 14600.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (13, 2, timestamptz '2025-08-10 11:00:00 Europe/Paris', 'Virement epargne aout', 'CREDIT', 200.00, 14400.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (14, 1, timestamptz '2025-11-01 08:00:00 Europe/Paris', 'Virement salaire novembre', 'CREDIT', 3200.00, 5649.30);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (15, 1, timestamptz '2025-11-02 09:00:00 Europe/Paris', 'Loyer novembre', 'DEBIT', 850.00, 4799.30);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (16, 1, timestamptz '2025-11-05 10:00:00 Europe/Paris', 'EDF - Facture energie novembre', 'DEBIT', 112.30, 4687.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (17, 1, timestamptz '2025-11-10 11:00:00 Europe/Paris', 'Virement epargne novembre', 'DEBIT', 200.00, 4487.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (18, 1, timestamptz '2025-11-12 14:00:00 Europe/Paris', 'Courses Leclerc novembre', 'DEBIT', 78.40, 4408.60);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (19, 1, timestamptz '2025-11-15 08:00:00 Europe/Paris', 'Abonnement telephone SFR', 'DEBIT', 45.00, 4363.60);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (20, 1, timestamptz '2025-11-16 20:00:00 Europe/Paris', 'Restaurant La Brasserie', 'DEBIT', 42.00, 4321.60);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (21, 1, timestamptz '2025-11-18 00:00:00 Europe/Paris', 'Netflix abonnement mensuel', 'DEBIT', 15.99, 4305.61);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (22, 1, timestamptz '2025-11-18 00:01:00 Europe/Paris', 'Spotify abonnement mensuel', 'DEBIT', 9.99, 4295.62);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (23, 1, timestamptz '2025-11-20 11:00:00 Europe/Paris', 'Pharmacie Monge - medicaments', 'DEBIT', 65.00, 4230.62);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (24, 1, timestamptz '2025-11-22 16:00:00 Europe/Paris', 'Essence Total station autoroute', 'DEBIT', 120.00, 4110.62);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (25, 1, timestamptz '2025-11-24 10:00:00 Europe/Paris', 'Carrefour courses semaine', 'DEBIT', 89.00, 4021.62);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (26, 1, timestamptz '2025-11-25 09:00:00 Europe/Paris', 'Assurance voiture annuelle AXA', 'DEBIT', 550.00, 3471.62);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (27, 1, timestamptz '2025-11-27 19:00:00 Europe/Paris', 'Cinema UGC - sorties loisirs', 'DEBIT', 25.00, 3446.62);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (28, 2, timestamptz '2025-11-10 11:00:00 Europe/Paris', 'Virement epargne novembre', 'CREDIT', 200.00, 15200.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (100, 3, timestamptz '2025-11-01 08:00:00 Europe/Paris', 'Virement salaire novembre', 'CREDIT', 4500.00, 4500.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (101, 3, timestamptz '2025-11-02 09:00:00 Europe/Paris', 'Loyer novembre', 'DEBIT', 950.00, 3550.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (102, 3, timestamptz '2025-11-05 10:00:00 Europe/Paris', 'Virement epargne', 'DEBIT', 300.00, 3250.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (103, 3, timestamptz '2025-11-10 11:00:00 Europe/Paris', 'Assurance habitation + auto', 'DEBIT', 600.00, 2650.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (104, 3, timestamptz '2025-11-15 14:00:00 Europe/Paris', 'Agence voyage - vacances', 'DEBIT', 1200.00, 1450.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (105, 3, timestamptz '2025-12-01 08:00:00 Europe/Paris', 'Virement salaire decembre', 'CREDIT', 4500.00, 5950.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (106, 3, timestamptz '2025-12-02 09:00:00 Europe/Paris', 'Loyer decembre', 'DEBIT', 1200.00, 4750.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (107, 3, timestamptz '2025-12-10 10:00:00 Europe/Paris', 'Factures diverses decembre', 'DEBIT', 900.00, 3850.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (200, 5, timestamptz '2025-10-01 08:00:00 Europe/Paris', 'Virement salaire octobre', 'CREDIT', 2800.00, 2800.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (201, 5, timestamptz '2025-10-02 09:00:00 Europe/Paris', 'Loyer octobre', 'DEBIT', 700.00, 2100.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (202, 5, timestamptz '2025-11-01 08:00:00 Europe/Paris', 'Virement salaire novembre', 'CREDIT', 2800.00, 4900.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (203, 5, timestamptz '2025-11-05 10:00:00 Europe/Paris', 'Remboursement pret personnel', 'DEBIT', 3700.00, 1200.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (300, 6, timestamptz '2025-11-01 08:00:00 Europe/Paris', 'Virement salaire novembre', 'CREDIT', 8000.00, 8000.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (301, 6, timestamptz '2025-11-02 09:00:00 Europe/Paris', 'Loyer + charges', 'DEBIT', 1800.00, 6200.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (302, 6, timestamptz '2025-11-05 10:00:00 Europe/Paris', 'Virement epargne', 'DEBIT', 2000.00, 4200.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (303, 6, timestamptz '2025-12-01 08:00:00 Europe/Paris', 'Virement salaire decembre', 'CREDIT', 8000.00, 12200.00);
insert into operations (id, compte_id, date_operation, description, type, montant, solde_apres) values (304, 6, timestamptz '2025-12-10 10:00:00 Europe/Paris', 'Depenses diverses decembre', 'DEBIT', 6600.00, 5600.00);
select setval(pg_get_serial_sequence('operations', 'id'), (select max(id) from operations));

-- RECURRING_TRANSFERS (3) : target_account_number -> numero_beneficiaire, target_label -> libelle_beneficiaire,
-- prochain_execution -> prochaine_execution, created_date -> cree_le, last_executed -> derniere_execution,
-- execution_count -> nb_executions.
insert into virements_programmes (id, client_id, compte_source_id, numero_beneficiaire, libelle_beneficiaire, montant, frequence, motif, prochaine_execution, actif, nb_executions, cree_le, derniere_execution) values (1, 1, 1, 'FR76-0001-0002', 'Compte Epargne Demo Bank', 200.00, 'MENSUEL', 'Epargne mensuelle automatique', timestamptz '2025-12-10 08:00:00 Europe/Paris', true, 2, timestamptz '2025-10-01 09:00:00 Europe/Paris', timestamptz '2025-11-10 08:00:00 Europe/Paris');
insert into virements_programmes (id, client_id, compte_source_id, numero_beneficiaire, libelle_beneficiaire, montant, frequence, motif, prochaine_execution, actif, nb_executions, cree_le, derniere_execution) values (2, 1, 1, 'FR76-3000-2001', 'EDF Energie', 120.00, 'MENSUEL', 'Facture electricite', timestamptz '2025-12-05 08:00:00 Europe/Paris', true, 3, timestamptz '2025-09-01 09:00:00 Europe/Paris', timestamptz '2025-11-05 08:00:00 Europe/Paris');
insert into virements_programmes (id, client_id, compte_source_id, numero_beneficiaire, libelle_beneficiaire, montant, frequence, motif, prochaine_execution, actif, nb_executions, cree_le, derniere_execution) values (3, 1, 1, 'FR76-2000-9999', 'Club de sport FitLife', 35.00, 'MENSUEL', 'Abonnement salle de sport', timestamptz '2026-01-01 08:00:00 Europe/Paris', false, 10, timestamptz '2025-01-01 09:00:00 Europe/Paris', timestamptz '2025-10-01 08:00:00 Europe/Paris');
select setval(pg_get_serial_sequence('virements_programmes', 'id'), (select max(id) from virements_programmes));

-- EXTERNAL_BENEFICIARIES (5) : holder_name -> titulaire, label -> libelle, bank -> banque, sepa_zone -> zone_sepa,
-- active -> actif, created_at -> cree_le.
insert into beneficiaires_externes (id, client_id, titulaire, iban, libelle, banque, bic, zone_sepa, actif, cree_le) values (1, 1, 'Jean Dupont', 'FR7630006000011234567890189', 'Mon compte Boursorama', 'Boursorama Banque', 'COURCNPAXXX', true, true, timestamptz '2025-01-15 00:00:00 Europe/Paris');
insert into beneficiaires_externes (id, client_id, titulaire, iban, libelle, banque, bic, zone_sepa, actif, cree_le) values (2, 1, 'Agence Dufour Immobilier', 'FR7630001007941234567890185', 'Loyer Paris 14e', 'BNP Paribas', 'BNPAFRPPXXX', true, true, timestamptz '2025-02-01 00:00:00 Europe/Paris');
insert into beneficiaires_externes (id, client_id, titulaire, iban, libelle, banque, bic, zone_sepa, actif, cree_le) values (3, 1, 'EDF SA', 'FR7630002005000000000000019', 'Electricite EDF', 'Credit Agricole', 'AGRIFRPPXXX', true, true, timestamptz '2025-03-10 00:00:00 Europe/Paris');
insert into beneficiaires_externes (id, client_id, titulaire, iban, libelle, banque, bic, zone_sepa, actif, cree_le) values (4, 2, 'Marie Lebrun', 'FR7610278000010002028490016', 'Compte credit mutuel', 'Credit Mutuel', 'CMCIFRPAXXX', true, true, timestamptz '2025-04-01 00:00:00 Europe/Paris');
insert into beneficiaires_externes (id, client_id, titulaire, iban, libelle, banque, bic, zone_sepa, actif, cree_le) values (5, 4, 'SCPI Pierre Plus', 'FR7630004000031234567890143', 'Investissement SCPI', 'BNP Paribas', 'BNPAFRPPXXX', true, true, timestamptz '2025-05-15 00:00:00 Europe/Paris');
select setval(pg_get_serial_sequence('beneficiaires_externes', 'id'), (select max(id) from beneficiaires_externes));

-- FRAUD_ALERTS (6) : rule -> regle, risk_score -> score, status -> statut, created_at -> cree_le,
-- reviewed_at -> revu_le, reviewed_by -> revu_par ; transaction_id (nul dans l'import) -> virement_id nul.
insert into alertes_fraude (id, client_id, virement_id, regle, score, description, statut, cree_le, revu_le, revu_par) values (1, 1, null, 'LARGE_AMOUNT', 80, 'Virement de 18 500 EUR vers compte externe — seuil très élevé dépassé.', 'CONFIRMED', timestamptz '2026-01-15 02:34:00 Europe/Paris', timestamptz '2026-01-15 09:00:00 Europe/Paris', 'ADMIN');
insert into alertes_fraude (id, client_id, virement_id, regle, score, description, statut, cree_le, revu_le, revu_par) values (2, 2, null, 'UNUSUAL_HOURS', 45, 'Transaction effectuée à 03h12 — heure inhabituelle.', 'FALSE_POSITIVE', timestamptz '2026-02-03 03:12:00 Europe/Paris', timestamptz '2026-02-03 10:30:00 Europe/Paris', 'ADMIN');
insert into alertes_fraude (id, client_id, virement_id, regle, score, description, statut, cree_le, revu_le, revu_par) values (3, 3, null, 'HIGH_FREQUENCY', 50, '9 transactions en une seule journée — fréquence anormale détectée.', 'OPEN', timestamptz '2026-03-10 14:20:00 Europe/Paris', null, null);
insert into alertes_fraude (id, client_id, virement_id, regle, score, description, statut, cree_le, revu_le, revu_par) values (4, 1, null, 'FOREIGN_IBAN', 30, 'Virement vers IBAN DE89 3704 0044 — zone hors France détectée.', 'OPEN', timestamptz '2026-04-22 11:05:00 Europe/Paris', null, null);
insert into alertes_fraude (id, client_id, virement_id, regle, score, description, statut, cree_le, revu_le, revu_par) values (5, 4, null, 'VELOCITY_CHECK', 65, '5 virements > 500 EUR en moins de 60 minutes.', 'OPEN', timestamptz '2026-05-18 16:45:00 Europe/Paris', null, null);
insert into alertes_fraude (id, client_id, virement_id, regle, score, description, statut, cree_le, revu_le, revu_par) values (6, 2, null, 'NEW_BENEFICIARY', 35, 'Premier virement vers ce bénéficiaire non enregistré.', 'REVIEWED', timestamptz '2026-05-25 09:30:00 Europe/Paris', timestamptz '2026-05-25 11:00:00 Europe/Paris', 'ADMIN');
select setval(pg_get_serial_sequence('alertes_fraude', 'id'), (select max(id) from alertes_fraude));

-- BUDGETS (6) : customer_id -> client_id, category -> categorie, year_month -> mois.
insert into plafonds_budget (id, client_id, categorie, mois, plafond) values (1, 1, 'LOGEMENT', '2025-11', 1000.00);
insert into plafonds_budget (id, client_id, categorie, mois, plafond) values (2, 1, 'ALIMENTATION', '2025-11', 250.00);
insert into plafonds_budget (id, client_id, categorie, mois, plafond) values (3, 1, 'TRANSPORT', '2025-11', 150.00);
insert into plafonds_budget (id, client_id, categorie, mois, plafond) values (4, 1, 'ABONNEMENTS', '2025-11', 80.00);
insert into plafonds_budget (id, client_id, categorie, mois, plafond) values (5, 1, 'LOISIRS', '2025-11', 100.00);
insert into plafonds_budget (id, client_id, categorie, mois, plafond) values (6, 1, 'SANTE', '2025-11', 50.00);
select setval(pg_get_serial_sequence('plafonds_budget', 'id'), (select max(id) from plafonds_budget));
