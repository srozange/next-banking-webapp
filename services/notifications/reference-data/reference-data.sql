-- Données de référence du service notifications : les 6 lignes NOTIFICATIONS de l'import.sql du legacy,
-- identifiants, types, titres, messages exacts, état de lecture et dates repris tels quels (dates de l'import lues à
-- l'heure de Paris). Aucune ne se rattache à un virement ni à un virement programmé.
-- Hors migrations : chargé dans une base déjà migrée par Flyway (make reset-data), jamais en production.
-- Correspondance : customer_id -> client_id, title -> titre, read -> lue, created_at -> cree_le.
-- Colonnes legacy sans équivalent cible, perdues : priority, read_at, notif_disc, transaction_amount, account_number,
-- loan_id, loan_status, system_code, action_url.
insert into notifications (id, client_id, type, titre, message, lue, cree_le) overriding system value values (1, 1, 'TRANSACTION_ALERT', 'Virement important', 'Un virement de 5 200,00 EUR a été effectué depuis votre compte FR76-30001-00001-00000000001.', false, timestamptz '2026-05-20 11:00:00 Europe/Paris');
insert into notifications (id, client_id, type, titre, message, lue, cree_le) overriding system value values (2, 1, 'LOAN_STATUS', 'Prêt approuvé', 'Votre demande de prêt immobilier de 150 000 EUR a été approuvée.', true, timestamptz '2026-01-10 09:30:00 Europe/Paris');
insert into notifications (id, client_id, type, titre, message, lue, cree_le) overriding system value values (3, 2, 'LOAN_STATUS', 'Remboursement en cours', 'Votre prochain prélèvement de 312,45 EUR est prévu le 01/06/2026.', false, timestamptz '2026-05-25 08:00:00 Europe/Paris');
insert into notifications (id, client_id, type, titre, message, lue, cree_le) overriding system value values (4, 1, 'SYSTEM', 'Maintenance planifiée', 'Une maintenance est prévue le 01/06/2026 de 02h00 à 04h00. Les virements seront temporairement indisponibles.', false, timestamptz '2026-05-28 10:00:00 Europe/Paris');
insert into notifications (id, client_id, type, titre, message, lue, cree_le) overriding system value values (5, 3, 'ACCOUNT_BALANCE', 'Solde faible', 'Le solde de votre compte FR76-30001-00001-00000000003 est inférieur à 100 EUR.', false, timestamptz '2026-05-30 07:00:00 Europe/Paris');
insert into notifications (id, client_id, type, titre, message, lue, cree_le) overriding system value values (6, 4, 'FRAUD_ALERT', 'Alerte fraude détectée', '5 virements supérieurs à 500 EUR ont été détectés en moins d''une heure. Contactez votre conseiller si vous n''êtes pas à l''origine de ces opérations.', false, timestamptz '2026-05-18 16:45:00 Europe/Paris');
select setval(pg_get_serial_sequence('notifications', 'id'), (select max(id) from notifications));
