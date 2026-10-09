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
values (1, 1, 'FR76-0001-0001', 'COURANT', 'FR76-0001-0001', 'DEMOFR21XXX', 2130.00, 1);
insert into comptes (compte_id, client_id, numero, type, iban, bic, solde, version_agregat)
values (2, 1, 'FR76-0001-0002', 'EPARGNE', 'FR76-0001-0002', 'DEMOFR21XXX', 15200.00, 1);
-- FIN PROJECTIONS GENEREES
