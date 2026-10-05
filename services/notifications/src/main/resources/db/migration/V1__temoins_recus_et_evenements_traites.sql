-- Pas de clé unique sur temoin_id : l'idempotence repose sur evenements_traites.
create table temoins_recus (
    id        bigint generated always as identity primary key,
    temoin_id bigint not null
);

create table evenements_traites (
    id_evenement uuid primary key
);
