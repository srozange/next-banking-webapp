-- Projection locale des clients, alimentée par identite-clients.client-modifie.v1 (documents n'appelle jamais
-- identite-clients) : nom_complet est celui imprimé sur les documents, version_agregat écarte un message ancien.
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

-- Projection locale des comptes, alimentée par coeur-bancaire.compte-modifie.v1 (documents n'appelle jamais
-- coeur-bancaire et ne modifie jamais un solde) : l'état complet du compte, sa version d'agrégat écartant un message
-- ancien. client_id désigne le titulaire ; l'identifiant numérique compte_id est le seul échangé entre services.
create table comptes (
    compte_id       bigint         primary key,
    client_id       bigint         not null,
    numero          varchar(34)    not null,
    type            varchar(20)    not null
                    constraint comptes_type check (type in ('COURANT', 'EPARGNE')),
    iban            varchar(34)    not null,
    bic             varchar(11)    not null,
    solde           numeric(19, 2) not null,
    version_agregat bigint         not null
);

create index comptes_par_client on comptes (client_id, compte_id);
