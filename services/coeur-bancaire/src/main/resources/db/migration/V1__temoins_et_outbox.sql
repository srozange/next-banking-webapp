create table temoins (
    id      bigint generated always as identity primary key,
    libelle text   not null
);

create table outbox (
    id           bigint generated always as identity primary key,
    id_evenement uuid    not null unique,
    topic        text    not null,
    cle          text    not null,
    charge       text    not null,
    publie       boolean not null default false
);

create index outbox_non_publiees on outbox (id) where not publie;
