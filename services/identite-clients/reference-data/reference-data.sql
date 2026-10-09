-- Données de référence du service identite-clients : les 7 utilisateurs du legacy (import.sql),
-- identifiants conservés. Hors migrations : chargé dans une base déjà migrée par Flyway.
-- Généré par reference-data/generer-reference-data.sh : ne pas modifier à la main.
-- Mots de passe : empreintes {bcrypt} calculées depuis RETRO_ADMIN_PASSWORD (admin),
-- RETRO_CONSEILLER_PASSWORD (conseillers) et RETRO_CLIENT_PASSWORD (clients), jamais en clair.
-- date_creation : date fixe, celle des projections des autres services (aucune date relative).
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since, date_creation)
values (10, 'admin', '{bcrypt}$2a$10$hXNfUPlnacyarmYL6eloM.qyhKf1O/N9RrXFtovk5DH3gQTCSxurm', 'Alice Martin', 'alice.martin@demobank.fr', '+33612345670', 'ADMIN', true, null, null, date '2026-10-06');
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since, date_creation)
values (11, 'sophie', '{bcrypt}$2a$10$6ERGv1whyHc7EFRMyUsNrOdZM9i/A.BFCn1KGM6yCQG4mp4q8tqm.', 'Sophie Durand', 'sophie.durand@demobank.fr', '+33612345671', 'CONSEILLER', true, null, null, date '2026-10-06');
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since, date_creation)
values (12, 'marc', '{bcrypt}$2a$10$ccRJSFbbrTkdON5sKi64helJZajs2vgIb6L28BXhf/dBp9rqo2AWC', 'Marc Lefevre', 'marc.lefevre@demobank.fr', '+33612345672', 'CONSEILLER', true, null, null, date '2026-10-06');
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since, date_creation)
values (1, 'user1', '{bcrypt}$2a$10$vxkD3qrK/Z7AU3eN19K5xeYv.Uq2GUiop47zLHzr5dajl/ZLOAFLa', 'Jean Dupont', 'jean.dupont@example.com', '+33601020304', 'CLIENT', true, 11, date '2024-01-15', date '2026-10-06');
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since, date_creation)
values (2, 'user2', '{bcrypt}$2a$10$017.fFNQJZExRSvNk3ToweuhgOoBFYgCIdIDhB/7pXfmrC11RLgZ2', 'Marie Lebrun', 'marie.lebrun@example.com', '+33601020305', 'CLIENT', true, 11, date '2024-03-10', date '2026-10-06');
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since, date_creation)
values (3, 'user3', '{bcrypt}$2a$10$ftKs0DZBvfHZjxsYm34fFexqB0MTxPxU1g4tFU2Cw7Eg5tDeVkPvq', 'Pierre Bernard', 'pierre.bernard@example.com', '+33601020306', 'CLIENT', true, 12, date '2024-02-20', date '2026-10-06');
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since, date_creation)
values (4, 'user4', '{bcrypt}$2a$10$/lrOg4f3tVJKhUUuPL562uLII5Jw6czgcI0BDt67POl/ryhokwj72', 'Lucie Martin', 'lucie.martin@example.com', '+33601020307', 'CLIENT', true, 12, date '2024-05-01', date '2026-10-06');
select setval(pg_get_serial_sequence('customers', 'id'), (select max(id) from customers));
