-- Données de référence du service coeur-bancaire : comptes du legacy (BANK_ACCOUNTS), identifiants conservés.
-- Hors migrations : chargé dans une base déjà migrée par Flyway.
-- Sources (docs/retro) : COURANT 1 « FR76-0001-0001 » et EPARGNE 2 « FR76-0001-0002 » de user1 (client 1), FEAT-003
-- FIELDS.md ; solde du COURANT à 2 130,00 € (FEAT-003 FLOW.md) ; solde total de user1 à 17 330,00 € (FEAT-002 FLOW.md),
-- soit 15 200,00 € pour l'EPARGNE.
-- Complément attendu (hors de la story 3.1) : les 5 autres comptes du legacy (4 COURANT et 3 EPARGNE au total) et les
-- 45 opérations du legacy ne figurent pas dans le corpus docs/retro : à ajouter par la story « Socle du rejeu final ».
insert into comptes (id, numero, type, solde, client_id) values (1, 'FR76-0001-0001', 'COURANT', 2130.00, 1);
insert into comptes (id, numero, type, solde, client_id) values (2, 'FR76-0001-0002', 'EPARGNE', 15200.00, 1);
select setval(pg_get_serial_sequence('comptes', 'id'), (select max(id) from comptes));

-- Bénéficiaire externe SEPA de user1 (client 1) : IBAN en FR76, BIC et banque renseignés ; écran Bénéficiaires
-- (FEAT-013, SC-013-01 : « FR76*** » et « ✓ SEPA »). Date de création en littéral : aucun now() dans les données.
insert into beneficiaires_externes (client_id, titulaire, iban, libelle, banque, bic, zone_sepa, actif, cree_le)
values (1, 'Marie Durand', 'FR7630006000011234567890189', 'Loyer', 'Banque Exemple', 'AGRIFRPP', true, true,
        '2026-10-01T08:00:00Z');
