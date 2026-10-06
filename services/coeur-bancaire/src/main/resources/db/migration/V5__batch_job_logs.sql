-- Journal des traitements nocturnes (FEAT-011, story 7.5) : la table batch_job_logs existe déjà (V4, story 3.4,
-- alimentée par le rejet à l'échéance) ; cette migration la reprend au lieu de la recréer.
-- Comme au legacy, une exécution non démarrée ou non terminée garde début, fin, durée, enregistrements ou statut vides.
alter table batch_job_logs
    alter column debut drop not null,
    alter column fin drop not null,
    alter column duree_ms drop not null,
    alter column enregistrements drop not null,
    alter column statut drop not null;

create index batch_job_logs_debut on batch_job_logs (debut desc nulls last);
