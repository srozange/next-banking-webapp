-- Suspension, revue et rejet à l'échéance des virements (story 3.4), projection locale des clients et journal
-- d'exécution des traitements planifiés. Aucun now(), CURRENT_TIMESTAMP ni CURRENT_DATE : le temps vient du port Horloge.
--
-- heure_butoir : soumission + 24 h, posée pour un virement SUSPENDU et conservée ensuite (suivi du client) ;
-- motif_suspension : motif en clair de la décision du contrôle ; issue, decideur, motif_decision, date_issue :
-- décision de revue (libération, rejet de l'administrateur, rejet à l'échéance, rejet au débit).
alter table virements
    add column heure_butoir     timestamptz,
    add column motif_suspension text,
    add column issue            varchar(30)
        constraint virements_issue check (issue in ('LIBERE', 'REJETE', 'REJETE_HEURE_BUTOIR', 'REJETE_SOLDE_AU_DEBIT')),
    add column decideur         varchar(100),
    add column motif_decision   text,
    add column date_issue       timestamptz;

create index virements_suspendus_par_butoir on virements (heure_butoir) where etat = 'SUSPENDU';
create index virements_traites_par_issue on virements (date_issue desc, id desc) where issue is not null;
create index virements_en_revue_par_client on virements (client_id, soumis_le desc, id desc)
    where heure_butoir is not null;

-- Projection locale des clients, alimentée par identite-clients.client-modifie.v1 : version_agregat écarte un message ancien.
create table clients (
    client_id       bigint       primary key,
    nom_complet     varchar(255) not null,
    version_agregat bigint       not null
);

-- BATCH_JOB_LOGS du cœur bancaire (NFR-BATCH-02) : une ligne par exécution notable d'un traitement planifié.
create table batch_job_logs (
    id              bigint generated always as identity primary key,
    job             varchar(64)  not null,
    debut           timestamptz  not null,
    fin             timestamptz  not null,
    duree_ms        bigint       not null,
    enregistrements integer      not null,
    statut          varchar(10)  not null
                    constraint batch_job_logs_statut check (statut in ('SUCCESS', 'FAILED', 'PARTIAL')),
    details         text
);
