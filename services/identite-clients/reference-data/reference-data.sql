-- Données de référence du service identite-clients : les 7 utilisateurs du legacy (import.sql),
-- identifiants conservés. Hors migrations : chargé dans une base déjà migrée par Flyway.
-- Généré par reference-data/generer-reference-data.sh : ne pas modifier à la main.
-- Mots de passe : empreintes {bcrypt} calculées depuis RETRO_ADMIN_PASSWORD (admin),
-- RETRO_CONSEILLER_PASSWORD (conseillers) et RETRO_CLIENT_PASSWORD (clients), jamais en clair.
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (10, 'admin', '{bcrypt}$2a$10$yE6Px7Xb2QE8M8eMXPR25.TW3ZPLUP/AOwefPhnneURwpTbiD3fCu', 'Alice Martin', 'alice.martin@demobank.fr', '+33612345670', 'ADMIN', true, null, null);
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (11, 'sophie', '{bcrypt}$2a$10$VErs/XT9dkCqDzUikE5gY.AKsbH9GDn9WtqK26k/XVrDUP4RgcRAO', 'Sophie Durand', 'sophie.durand@demobank.fr', '+33612345671', 'CONSEILLER', true, null, null);
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (12, 'marc', '{bcrypt}$2a$10$jk61xtohIiD9WLHZCrQhDefq/S923hdOcCkUGT2tm5YAr813ZdJVi', 'Marc Lefevre', 'marc.lefevre@demobank.fr', '+33612345672', 'CONSEILLER', true, null, null);
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (1, 'user1', '{bcrypt}$2a$10$35uq7PIQK7LKWTXAlA4HVuYh9HL4lsmng09ukgvmBHuqF7OR/zsyO', 'Jean Dupont', 'jean.dupont@example.com', '+33601020304', 'CLIENT', true, 11, date '2024-01-15');
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (2, 'user2', '{bcrypt}$2a$10$8xMr1EsBuTd9ycuiGSDwQuiwaVyuwISSleV/3hOKOZweZmL2BRooG', 'Marie Lebrun', 'marie.lebrun@example.com', '+33601020305', 'CLIENT', true, 11, date '2024-03-10');
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (3, 'user3', '{bcrypt}$2a$10$ritiWLsuvwwhsuihSjzhCOn7zEkSbdwQxJGKI76ZynpaUhUXg/oM2', 'Pierre Bernard', 'pierre.bernard@example.com', '+33601020306', 'CLIENT', true, 12, date '2024-02-20');
insert into customers (id, username, password, full_name, email, phone, role, active, advisor_id, advisor_since)
values (4, 'user4', '{bcrypt}$2a$10$Rzb6QJETnLeozmJYhQzIr.RTz582REpzKnpZy31HZfUTj8qw//Cb2', 'Lucie Martin', 'lucie.martin@example.com', '+33601020307', 'CLIENT', true, 12, date '2024-05-01');
select setval(pg_get_serial_sequence('customers', 'id'), (select max(id) from customers));
