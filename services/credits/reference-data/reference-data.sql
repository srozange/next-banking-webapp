-- Données de référence du service credits : 2 prêts EN_COURS de user1 (identifiant client 1) et leurs
-- 18 échéances, identifiants numériques. Hors migrations : chargé dans une base déjà migrée par Flyway.
-- Valeurs reconstituées (import.sql du legacy absent du dépôt), à confirmer contre le legacy.
-- Généré par reference-data/generer-reference-data.sh : ne pas modifier à la main.
-- Dates relatives à la date de génération (Horloge) : début = 1er d'un mois passé.
insert into loans (id, customer_id, type, montant, taux_annuel, duree_mois, mensualite, cout_total, total_interets, motif, status, date_debut, date_fin)
values (1, 1, 'TRAVAUX', 5000, 4.9000, 12, 427.81, 5133.72, 133.72, 'Travaux de renovation', 'EN_COURS', date '2026-06-01', date '2027-06-01');
insert into loans (id, customer_id, type, montant, taux_annuel, duree_mois, mensualite, cout_total, total_interets, motif, status, date_debut, date_fin)
values (2, 1, 'AUTO', 9000, 3.9000, 6, 1517.11, 9102.66, 102.66, 'Achat vehicule', 'EN_COURS', date '2026-08-01', date '2027-02-01');
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (1, 1, 1, date '2026-07-01', 407.39, 20.42, 427.81, 4592.61, true, timestamptz '2026-07-01T08:00:00Z');
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (2, 1, 2, date '2026-08-01', 409.06, 18.75, 427.81, 4183.55, true, timestamptz '2026-08-01T08:00:00Z');
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (3, 1, 3, date '2026-09-01', 410.73, 17.08, 427.81, 3772.82, true, timestamptz '2026-09-01T08:00:00Z');
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (4, 1, 4, date '2026-10-01', 412.40, 15.41, 427.81, 3360.42, true, timestamptz '2026-10-01T08:00:00Z');
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (5, 1, 5, date '2026-11-01', 414.09, 13.72, 427.81, 2946.33, false, null);
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (6, 1, 6, date '2026-12-01', 415.78, 12.03, 427.81, 2530.55, false, null);
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (7, 1, 7, date '2027-01-01', 417.48, 10.33, 427.81, 2113.07, false, null);
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (8, 1, 8, date '2027-02-01', 419.18, 8.63, 427.81, 1693.89, false, null);
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (9, 1, 9, date '2027-03-01', 420.89, 6.92, 427.81, 1273.00, false, null);
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (10, 1, 10, date '2027-04-01', 422.61, 5.20, 427.81, 850.39, false, null);
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (11, 1, 11, date '2027-05-01', 424.34, 3.47, 427.81, 426.05, false, null);
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (12, 1, 12, date '2027-06-01', 426.05, 1.74, 427.79, 0.00, false, null);
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (13, 2, 1, date '2026-09-01', 1487.86, 29.25, 1517.11, 7512.14, true, timestamptz '2026-09-01T08:00:00Z');
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (14, 2, 2, date '2026-10-01', 1492.70, 24.41, 1517.11, 6019.44, true, timestamptz '2026-10-01T08:00:00Z');
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (15, 2, 3, date '2026-11-01', 1497.55, 19.56, 1517.11, 4521.89, false, null);
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (16, 2, 4, date '2026-12-01', 1502.41, 14.70, 1517.11, 3019.48, false, null);
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (17, 2, 5, date '2027-01-01', 1507.30, 9.81, 1517.11, 1512.18, false, null);
insert into loan_repayments (id, loan_id, numero_echeance, date_echeance, montant_capital, montant_interets, montant_total, capital_restant, paye, date_paiement)
values (18, 2, 6, date '2027-02-01', 1512.18, 4.91, 1517.09, 0.00, false, null);
select setval(pg_get_serial_sequence('loans', 'id'), (select max(id) from loans));
select setval(pg_get_serial_sequence('loan_repayments', 'id'), (select max(id) from loan_repayments));
