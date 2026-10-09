-- Version de l'agrégat compte : incrémentée à chaque débit et crédit, portée par coeur-bancaire.compte-modifie.v1
-- (les consommateurs n'appliquent qu'une version strictement supérieure). Les comptes existants partent de 1.
alter table comptes add column version_agregat bigint not null default 1;
