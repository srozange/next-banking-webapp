-- Données de référence du service identite-clients : les 7 utilisateurs du legacy (import.sql),
-- identifiants conservés. Hors migrations : chargé dans une base déjà migrée par Flyway.
-- Généré par reference-data/generer-reference-data.sh : ne pas modifier à la main.
-- Mots de passe : empreintes {bcrypt} calculées depuis RETRO_ADMIN_PASSWORD (admin),
-- RETRO_CONSEILLER_PASSWORD (conseillers) et RETRO_CLIENT_PASSWORD (clients), jamais en clair.
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (10, 'admin', '{bcrypt}$2a$10$i5L1imsOUckglxkaeZTTeeru3sFP4ad2K4kmfv0CtAbruJyWJZqUK', 'Alice Martin', 'alice.martin@demobank.fr', '+33612345670', 'ADMIN', true, null, null);
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (11, 'sophie', '{bcrypt}$2a$10$3GE1UaAMYlqqonfus6g99O0A5cpImKnEuvU40liNoR9uX7Djxzequ', 'Sophie Durand', 'sophie.durand@demobank.fr', '+33612345671', 'CONSEILLER', true, null, null);
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (12, 'marc', '{bcrypt}$2a$10$SKVFQ6s6jKoTia1KmyHpaO8qJArOmv23MS7ZK/knk2NITzWrbVjjK', 'Marc Lefevre', 'marc.lefevre@demobank.fr', '+33612345672', 'CONSEILLER', true, null, null);
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (1, 'user1', '{bcrypt}$2a$10$QOdBMTzisfE07NfnmUyBW.VIFWn.0NGANqbDviZiCFr2KZpCX1h4i', 'Jean Dupont', 'jean.dupont@example.com', '+33601020304', 'CLIENT', true, 11, date '2024-01-15');
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (2, 'user2', '{bcrypt}$2a$10$nHeyRWRn27S/.H3NOsTKqO8claYQwvszvPAA0JLWt.svy21eggRUG', 'Marie Lebrun', 'marie.lebrun@example.com', '+33601020305', 'CLIENT', true, 11, date '2024-03-10');
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (3, 'user3', '{bcrypt}$2a$10$W1/RJ1qBazt3dg8EbCypjum9x2WPimWAolZst49PUVRXPypiHrb.e', 'Pierre Bernard', 'pierre.bernard@example.com', '+33601020306', 'CLIENT', true, 12, date '2024-02-20');
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (4, 'user4', '{bcrypt}$2a$10$0fv8KPMYA2VteY3rHW8uXuXDh1HIt7kvAVPdB./.A.EM0VySFfVNO', 'Lucie Martin', 'lucie.martin@example.com', '+33601020307', 'CLIENT', true, 12, date '2024-05-01');
select setval(pg_get_serial_sequence('customers', 'id'), (select max(id) from customers));
