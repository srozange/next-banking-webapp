-- Outbox transactionnelle : une ligne par événement, écrite dans la transaction de l'écriture métier,
-- publiée après validation puis marquée publiée (relais pour les lignes restées en attente).
create table outbox (
    id           bigint generated always as identity primary key,
    id_evenement uuid    not null unique,
    topic        text    not null,
    cle          text    not null,
    charge       text    not null,
    publie       boolean not null default false
);

create index outbox_non_publiees on outbox (id) where not publie;
