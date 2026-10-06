-- Publication de l'état des clients (identite-clients.client-modifie.v1) : outbox écrite dans la transaction de
-- l'écriture de customers, et colonnes d'état complet. date_creation reçoit la date de la migration pour les clients
-- existants (valeur de référence, pas une date métier) ; supprime reste faux (aucune suppression n'existe).
alter table customers add column date_creation date not null default current_date;
alter table customers add column supprime boolean not null default false;
alter table customers add column version_agregat bigint not null default 1;

create table outbox (
    id           bigint generated always as identity primary key,
    id_evenement uuid    not null unique,
    topic        text    not null,
    cle          text    not null,
    charge       text    not null,
    publie       boolean not null default false
);

create index outbox_non_publiees on outbox (id) where not publie;
