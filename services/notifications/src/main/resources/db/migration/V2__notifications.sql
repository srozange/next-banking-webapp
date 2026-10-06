-- Notifications conservées en base (BUG-006) : la plus récente a le plus grand identifiant.
-- L'idempotence repose sur evenements_traites ; le titre n'a aucun texte défini pour un virement.
create table notifications (
    id          bigint generated always as identity primary key,
    virement_id bigint      not null,
    client_id   bigint      not null,
    type        text        not null,
    titre       text,
    message     text        not null,
    lue         boolean     not null default false,
    cree_le     timestamptz not null
);

create index notifications_client_id_idx on notifications (client_id, id desc);
