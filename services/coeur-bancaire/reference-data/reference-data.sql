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
