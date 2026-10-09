-- Projection locale des opérations, alimentée par coeur-bancaire.operation-enregistree.v1 (documents n'appelle jamais
-- coeur-bancaire) : une opération est immuable, son identifiant operation_id est la clé d'idempotence. compte_id est
-- l'identifiant numérique du compte, seul échangé entre services. version_agregat est la version du compte relue après
-- l'écriture : elle trace l'ordre de livraison, elle n'ordonne pas les opérations.
create table operations (
    operation_id    bigint         primary key,
    compte_id       bigint         not null,
    instant         timestamptz    not null,
    libelle         text           not null,
    type            varchar(10)    not null
                    constraint operations_type check (type in ('CREDIT', 'DEBIT')),
    montant         numeric(19, 2) not null,
    solde_apres     numeric(19, 2) not null,
    version_agregat bigint         not null
);

create index operations_par_compte on operations (compte_id, instant);
