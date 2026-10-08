-- Virements programmés (FEAT-021, story 4.6) : instant de la dernière exécution d'une échéance. Nul tant qu'aucune
-- échéance n'a été exécutée. Aucun now() : l'instant vient du port Horloge.
alter table virements_programmes add column derniere_execution timestamptz;
