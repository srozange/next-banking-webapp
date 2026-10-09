-- Les 6 notifications de l'import.sql du legacy (données de référence) ne se rattachent ni à un virement ni à un
-- virement programmé : la base accepte désormais une notification sans origine, mais jamais deux origines.
-- Le domaine (Notification) garde sa règle pour les écritures du service : exactement une origine.
alter table notifications drop constraint notifications_une_origine;
alter table notifications add constraint notifications_au_plus_une_origine
    check (not (virement_id is not null and virement_programme_id is not null));
