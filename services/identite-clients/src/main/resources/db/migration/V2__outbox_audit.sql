-- Outbox d'audit : une ligne par entrée (LOGIN, LOGIN_FAILED...), écrite dans une transaction indépendante de
-- l'opération, publiée sur identite-clients.audit.v1 après validation, ou par le relais.
create table outbox_audit (
    id           bigint generated always as identity primary key,
    id_evenement uuid    not null unique,
    topic        text    not null,
    cle          text    not null,
    charge       text    not null,
    publie       boolean not null default false
);

create index outbox_audit_non_publiees on outbox_audit (id) where not publie;
