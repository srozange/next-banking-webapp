-- Notifications d'échéance de virement programmé (story 4.7) : une notification se rattache à un virement
-- (virement_id) ou à un virement programmé (virement_programme_id), jamais aux deux.
alter table notifications alter column virement_id drop not null;
alter table notifications add column virement_programme_id bigint;
alter table notifications add constraint notifications_une_origine
    check ((virement_id is null) <> (virement_programme_id is null));
