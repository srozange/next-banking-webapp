-- Projection de référence des clients du service credits : un client par utilisateur du legacy, en version 1.
-- Hors migrations : chargé dans une base déjà migrée par Flyway.
-- Généré par scripts/generer-projections-reference.sh depuis services/identite-clients/reference-data/reference-data.sql : ne pas modifier à la main.
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
