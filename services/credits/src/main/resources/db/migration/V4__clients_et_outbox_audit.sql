-- Projection locale des clients, alimentée par identite-clients.client-modifie.v1 (credits n'appelle jamais
-- identite-clients) : version_agregat écarte un message ancien ; nom_complet sert au pipeline de l'administration.
create table clients (
    client_id       bigint       primary key,
    login           varchar(100) not null,
    nom_complet     varchar(255) not null,
    role            varchar(20)  not null
                    constraint clients_role check (role in ('CLIENT', 'CONSEILLER', 'ADMIN')),
    actif           boolean      not null,
    conseiller_id   bigint,
    date_creation   date         not null,
    supprime        boolean      not null default false,
    version_agregat bigint       not null
);

-- Outbox d'audit : une ligne par décision de l'administrateur (ADMIN_LOAN_APPROVE, ADMIN_LOAN_REFUSE), écrite dans une
-- transaction indépendante de celle de la décision, publiée sur credits.audit.v1 après validation, ou par le relais.
create table outbox_audit (
    id           bigint generated always as identity primary key,
    id_evenement uuid    not null unique,
    topic        text    not null,
    cle          text    not null,
    charge       text    not null,
    publie       boolean not null default false
);

create index outbox_audit_non_publiees on outbox_audit (id) where not publie;
