-- Comptes et transactions externes lus chez Powens (FEAT-014, story 8.2) ; un solde absent reste NULL (BUG-042).
alter table etat_powens add column compte_selectionne bigint;

create table comptes_externes (
    sub       varchar(100)  not null,
    compte_id bigint        not null,
    nom       varchar(200),
    type      varchar(50),
    solde     numeric(19,2),
    iban      varchar(50),
    primary key (sub, compte_id)
);

create table transactions_externes (
    sub       varchar(100)  not null,
    compte_id bigint        not null,
    id        bigint        not null,
    date      date          not null,
    libelle   varchar(500),
    montant   numeric(19,2) not null,
    primary key (sub, compte_id, id)
);
