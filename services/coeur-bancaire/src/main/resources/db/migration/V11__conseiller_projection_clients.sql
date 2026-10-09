-- Portefeuille du conseiller (story 9.1) : la projection locale des clients reçoit l'affectation et la suppression
-- portées par identite-clients.client-modifie.v1. Une ligne déjà présente garde conseiller_id nul jusqu'à un événement
-- de version supérieure.
alter table clients add column conseiller_id bigint;
alter table clients add column supprime boolean not null default false;

create index clients_par_conseiller on clients (conseiller_id);
