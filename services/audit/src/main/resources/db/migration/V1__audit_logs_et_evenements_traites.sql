-- Journal d'audit (AUDIT_LOGS du legacy) : écrit par le seul service audit, depuis les événements {service}.audit.v1.
-- action_date : horodatage de l'enveloppe (Horloge du producteur), jamais l'heure de réception.
-- username en texte libre : identifiant saisi pour LOGIN_FAILED, sans borne de longueur.
create table audit_logs (
    id          bigint generated always as identity primary key,
    action      varchar(64) not null,
    action_date timestamptz not null,
    details     text,
    ipaddress   text,
    username    text        not null
);

-- Idempotence du consommateur : un événement déjà présent est ignoré.
create table evenements_traites (
    id_evenement uuid primary key
);
