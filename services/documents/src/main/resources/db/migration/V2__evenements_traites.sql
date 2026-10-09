-- Idempotence des consommateurs : un événement n'est appliqué qu'une fois (clé primaire = identifiant de l'événement).
create table evenements_traites (
    id_evenement uuid primary key
);
