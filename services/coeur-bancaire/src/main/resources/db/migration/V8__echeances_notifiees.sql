-- Échéances de virements programmés notifiées (FEAT-021, story 4.7) : une notification au plus par virement et par
-- échéance. La clé primaire absorbe deux passages concurrents. Aucun now() : l'instant vient du port Horloge.
create table echeances_notifiees (
    virement_programme_id bigint      not null,
    echeance              timestamptz not null,
    notifiee_le           timestamptz not null,
    primary key (virement_programme_id, echeance)
);
