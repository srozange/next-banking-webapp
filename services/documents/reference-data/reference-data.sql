-- Données de référence du service documents. Hors migrations : chargé dans une base déjà migrée par Flyway.
-- 1. Les 5 lignes DOCUMENTS de l'import.sql du legacy (l. 405-415) : mêmes valeurs, dates en timestamptz (heure de
--    Paris du legacy, hiver : UTC+1). Leur type est celui du legacy ; les téléchargements du service créent leurs
--    propres lignes (RIB, ATTESTATION, ...) à la première génération.
insert into documents (id, customer_id, document_type, filename, mime_type, file_size_bytes, generated_at, download_count, checksum, parametres)
values (1, 1, 'RELEVE_MENSUEL', 'releve_user1_2025_12.pdf', 'application/pdf', 24680, timestamptz '2026-01-02 08:00:00+01', 3, 'a1b2c3d4e5f6789012345678901234ab', 'month=12;year=2025;customerId=1');
insert into documents (id, customer_id, document_type, filename, mime_type, file_size_bytes, generated_at, download_count, checksum, parametres)
values (2, 1, 'ATTESTATION_SOLDE', 'attestation_user1_20260115.pdf', 'application/pdf', 18432, timestamptz '2026-01-15 10:22:00+01', 1, 'bc45de67fa890123456789abcdef1234', 'customerId=1;totalBalance=17330.00');
insert into documents (id, customer_id, document_type, filename, mime_type, file_size_bytes, generated_at, download_count, checksum, parametres)
values (3, 1, 'TABLEAU_AMORTISSEMENT', 'tableau_amort_pret1_user1.pdf', 'application/pdf', 52100, timestamptz '2026-01-20 14:05:00+01', 2, 'e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6', 'loanId=1;customerId=1;montant=150000.00;dureeMois=240');
insert into documents (id, customer_id, document_type, filename, mime_type, file_size_bytes, generated_at, download_count, checksum, parametres)
values (4, 2, 'RELEVE_MENSUEL', 'releve_user2_2025_12.pdf', 'application/pdf', 19200, timestamptz '2026-01-02 08:05:00+01', 0, 'f0e1d2c3b4a5968778695a4b3c2d1e0f', 'month=12;year=2025;customerId=2');
insert into documents (id, customer_id, document_type, filename, mime_type, file_size_bytes, generated_at, download_count, checksum, parametres)
values (5, 3, 'ATTESTATION_SOLDE', 'attestation_user3_20260310.pdf', 'application/pdf', 17800, timestamptz '2026-03-10 09:00:00+01', 1, '12ab34cd56ef78901234567890abcdef', 'customerId=3;totalBalance=8950.00');
select setval(pg_get_serial_sequence('documents', 'id'), (select max(id) from documents));

-- 2. Projections des clients et des comptes, générées par scripts/generer-projections-reference.sh depuis les
--    reference-data.sql d'identite-clients et de coeur-bancaire : ne pas modifier à la main.
-- DEBUT PROJECTIONS GENEREES
insert into clients (client_id, login, nom_complet, role, actif, conseiller_id, date_creation, supprime, version_agregat)
values (10, 'admin', 'Alice Martin', 'ADMIN', true, null, date '2026-10-06', false, 1);
insert into clients (client_id, login, nom_complet, role, actif, conseiller_id, date_creation, supprime, version_agregat)
values (11, 'sophie', 'Sophie Durand', 'CONSEILLER', true, null, date '2026-10-06', false, 1);
insert into clients (client_id, login, nom_complet, role, actif, conseiller_id, date_creation, supprime, version_agregat)
values (12, 'marc', 'Marc Lefevre', 'CONSEILLER', true, null, date '2026-10-06', false, 1);
insert into clients (client_id, login, nom_complet, role, actif, conseiller_id, date_creation, supprime, version_agregat)
values (1, 'user1', 'Jean Dupont', 'CLIENT', true, 11, date '2026-10-06', false, 1);
insert into clients (client_id, login, nom_complet, role, actif, conseiller_id, date_creation, supprime, version_agregat)
values (2, 'user2', 'Marie Lebrun', 'CLIENT', true, 11, date '2026-10-06', false, 1);
insert into clients (client_id, login, nom_complet, role, actif, conseiller_id, date_creation, supprime, version_agregat)
values (3, 'user3', 'Pierre Bernard', 'CLIENT', true, 12, date '2026-10-06', false, 1);
insert into clients (client_id, login, nom_complet, role, actif, conseiller_id, date_creation, supprime, version_agregat)
values (4, 'user4', 'Lucie Martin', 'CLIENT', true, 12, date '2026-10-06', false, 1);
insert into comptes (compte_id, client_id, numero, type, iban, bic, solde, version_agregat)
values (1, 1, 'FR76-0001-0001', 'COURANT', 'FR76-0001-0001', 'DEMOFR21XXX', 2450.00, 1);
insert into comptes (compte_id, client_id, numero, type, iban, bic, solde, version_agregat)
values (2, 1, 'FR76-0001-0002', 'EPARGNE', 'FR76-0001-0002', 'DEMOFR21XXX', 15000.00, 1);
insert into comptes (compte_id, client_id, numero, type, iban, bic, solde, version_agregat)
values (3, 2, 'FR76-0002-0001', 'COURANT', 'FR76-0002-0001', 'DEMOFR21XXX', 3850.00, 1);
insert into comptes (compte_id, client_id, numero, type, iban, bic, solde, version_agregat)
values (4, 2, 'FR76-0002-0002', 'EPARGNE', 'FR76-0002-0002', 'DEMOFR21XXX', 22000.00, 1);
insert into comptes (compte_id, client_id, numero, type, iban, bic, solde, version_agregat)
values (5, 3, 'FR76-0003-0001', 'COURANT', 'FR76-0003-0001', 'DEMOFR21XXX', 1200.00, 1);
insert into comptes (compte_id, client_id, numero, type, iban, bic, solde, version_agregat)
values (6, 4, 'FR76-0004-0001', 'COURANT', 'FR76-0004-0001', 'DEMOFR21XXX', 5600.00, 1);
insert into comptes (compte_id, client_id, numero, type, iban, bic, solde, version_agregat)
values (7, 4, 'FR76-0004-0002', 'EPARGNE', 'FR76-0004-0002', 'DEMOFR21XXX', 48000.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (1, 1, timestamptz '2025-10-01 08:00:00 Europe/Paris', 'Virement salaire octobre', 'CREDIT', 3200.00, 3200.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (2, 1, timestamptz '2025-10-02 09:30:00 Europe/Paris', 'Loyer octobre', 'DEBIT', 850.00, 2350.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (3, 1, timestamptz '2025-10-05 10:00:00 Europe/Paris', 'EDF - Facture energie', 'DEBIT', 120.50, 2229.50, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (4, 1, timestamptz '2025-10-10 11:00:00 Europe/Paris', 'Virement vers FR76-0001-0002 - Epargne mensuelle', 'DEBIT', 200.00, 2029.50, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (5, 1, timestamptz '2025-10-12 14:20:00 Europe/Paris', 'Courses Carrefour', 'DEBIT', 65.30, 1964.20, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (6, 1, timestamptz '2025-10-15 08:00:00 Europe/Paris', 'Abonnement telephone', 'DEBIT', 45.00, 1919.20, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (7, 1, timestamptz '2025-10-18 17:00:00 Europe/Paris', 'Remboursement ami - Pierre', 'CREDIT', 200.00, 2119.20, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (8, 1, timestamptz '2025-10-20 10:15:00 Europe/Paris', 'Amazon - Commande 123', 'DEBIT', 89.90, 2029.30, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (9, 1, timestamptz '2025-10-22 20:30:00 Europe/Paris', 'Restaurant Le Bistrot', 'DEBIT', 30.00, 1999.30, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (10, 1, timestamptz '2025-10-25 09:00:00 Europe/Paris', 'Assurance voiture annuelle', 'DEBIT', 450.70, 1548.60, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (11, 2, timestamptz '2025-10-10 11:00:00 Europe/Paris', 'Virement depuis FR76-0001-0001 - Epargne mensuelle', 'CREDIT', 200.00, 14800.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (12, 2, timestamptz '2025-09-10 11:00:00 Europe/Paris', 'Virement epargne septembre', 'CREDIT', 200.00, 14600.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (13, 2, timestamptz '2025-08-10 11:00:00 Europe/Paris', 'Virement epargne aout', 'CREDIT', 200.00, 14400.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (14, 1, timestamptz '2025-11-01 08:00:00 Europe/Paris', 'Virement salaire novembre', 'CREDIT', 3200.00, 5649.30, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (15, 1, timestamptz '2025-11-02 09:00:00 Europe/Paris', 'Loyer novembre', 'DEBIT', 850.00, 4799.30, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (16, 1, timestamptz '2025-11-05 10:00:00 Europe/Paris', 'EDF - Facture energie novembre', 'DEBIT', 112.30, 4687.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (17, 1, timestamptz '2025-11-10 11:00:00 Europe/Paris', 'Virement epargne novembre', 'DEBIT', 200.00, 4487.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (18, 1, timestamptz '2025-11-12 14:00:00 Europe/Paris', 'Courses Leclerc novembre', 'DEBIT', 78.40, 4408.60, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (19, 1, timestamptz '2025-11-15 08:00:00 Europe/Paris', 'Abonnement telephone SFR', 'DEBIT', 45.00, 4363.60, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (20, 1, timestamptz '2025-11-16 20:00:00 Europe/Paris', 'Restaurant La Brasserie', 'DEBIT', 42.00, 4321.60, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (21, 1, timestamptz '2025-11-18 00:00:00 Europe/Paris', 'Netflix abonnement mensuel', 'DEBIT', 15.99, 4305.61, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (22, 1, timestamptz '2025-11-18 00:01:00 Europe/Paris', 'Spotify abonnement mensuel', 'DEBIT', 9.99, 4295.62, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (23, 1, timestamptz '2025-11-20 11:00:00 Europe/Paris', 'Pharmacie Monge - medicaments', 'DEBIT', 65.00, 4230.62, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (24, 1, timestamptz '2025-11-22 16:00:00 Europe/Paris', 'Essence Total station autoroute', 'DEBIT', 120.00, 4110.62, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (25, 1, timestamptz '2025-11-24 10:00:00 Europe/Paris', 'Carrefour courses semaine', 'DEBIT', 89.00, 4021.62, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (26, 1, timestamptz '2025-11-25 09:00:00 Europe/Paris', 'Assurance voiture annuelle AXA', 'DEBIT', 550.00, 3471.62, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (27, 1, timestamptz '2025-11-27 19:00:00 Europe/Paris', 'Cinema UGC - sorties loisirs', 'DEBIT', 25.00, 3446.62, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (28, 2, timestamptz '2025-11-10 11:00:00 Europe/Paris', 'Virement epargne novembre', 'CREDIT', 200.00, 15200.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (100, 3, timestamptz '2025-11-01 08:00:00 Europe/Paris', 'Virement salaire novembre', 'CREDIT', 4500.00, 4500.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (101, 3, timestamptz '2025-11-02 09:00:00 Europe/Paris', 'Loyer novembre', 'DEBIT', 950.00, 3550.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (102, 3, timestamptz '2025-11-05 10:00:00 Europe/Paris', 'Virement epargne', 'DEBIT', 300.00, 3250.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (103, 3, timestamptz '2025-11-10 11:00:00 Europe/Paris', 'Assurance habitation + auto', 'DEBIT', 600.00, 2650.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (104, 3, timestamptz '2025-11-15 14:00:00 Europe/Paris', 'Agence voyage - vacances', 'DEBIT', 1200.00, 1450.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (105, 3, timestamptz '2025-12-01 08:00:00 Europe/Paris', 'Virement salaire decembre', 'CREDIT', 4500.00, 5950.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (106, 3, timestamptz '2025-12-02 09:00:00 Europe/Paris', 'Loyer decembre', 'DEBIT', 1200.00, 4750.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (107, 3, timestamptz '2025-12-10 10:00:00 Europe/Paris', 'Factures diverses decembre', 'DEBIT', 900.00, 3850.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (200, 5, timestamptz '2025-10-01 08:00:00 Europe/Paris', 'Virement salaire octobre', 'CREDIT', 2800.00, 2800.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (201, 5, timestamptz '2025-10-02 09:00:00 Europe/Paris', 'Loyer octobre', 'DEBIT', 700.00, 2100.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (202, 5, timestamptz '2025-11-01 08:00:00 Europe/Paris', 'Virement salaire novembre', 'CREDIT', 2800.00, 4900.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (203, 5, timestamptz '2025-11-05 10:00:00 Europe/Paris', 'Remboursement pret personnel', 'DEBIT', 3700.00, 1200.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (300, 6, timestamptz '2025-11-01 08:00:00 Europe/Paris', 'Virement salaire novembre', 'CREDIT', 8000.00, 8000.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (301, 6, timestamptz '2025-11-02 09:00:00 Europe/Paris', 'Loyer + charges', 'DEBIT', 1800.00, 6200.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (302, 6, timestamptz '2025-11-05 10:00:00 Europe/Paris', 'Virement epargne', 'DEBIT', 2000.00, 4200.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (303, 6, timestamptz '2025-12-01 08:00:00 Europe/Paris', 'Virement salaire decembre', 'CREDIT', 8000.00, 12200.00, 1);
insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat)
values (304, 6, timestamptz '2025-12-10 10:00:00 Europe/Paris', 'Depenses diverses decembre', 'DEBIT', 6600.00, 5600.00, 1);
-- FIN PROJECTIONS GENEREES
