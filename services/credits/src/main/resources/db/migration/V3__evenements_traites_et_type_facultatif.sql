-- Idempotence des consommateurs : un événement n'est appliqué qu'une fois (clé primaire = identifiant de l'événement).
create table evenements_traites (
    id_evenement uuid primary key
);

-- Le legacy enregistre un prêt sans type (BUG-078) : le scoring le refuse. La contrainte loans_type continue de
-- refuser toute valeur hors des cinq types.
alter table loans alter column type drop not null;
