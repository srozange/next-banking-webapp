-- Projection de référence des clients du service coeur-bancaire : un client par utilisateur du legacy, en version 1.
-- Hors migrations : chargé dans une base déjà migrée par Flyway.
-- Généré par scripts/generer-projections-reference.sh depuis services/identite-clients/reference-data/reference-data.sql : ne pas modifier à la main.
insert into clients (client_id, nom_complet, conseiller_id, supprime, version_agregat)
values (10, 'Alice Martin', null, false, 1);
insert into clients (client_id, nom_complet, conseiller_id, supprime, version_agregat)
values (11, 'Sophie Durand', null, false, 1);
insert into clients (client_id, nom_complet, conseiller_id, supprime, version_agregat)
values (12, 'Marc Lefevre', null, false, 1);
insert into clients (client_id, nom_complet, conseiller_id, supprime, version_agregat)
values (1, 'Jean Dupont', 11, false, 1);
insert into clients (client_id, nom_complet, conseiller_id, supprime, version_agregat)
values (2, 'Marie Lebrun', 11, false, 1);
insert into clients (client_id, nom_complet, conseiller_id, supprime, version_agregat)
values (3, 'Pierre Bernard', 12, false, 1);
insert into clients (client_id, nom_complet, conseiller_id, supprime, version_agregat)
values (4, 'Lucie Martin', 12, false, 1);
