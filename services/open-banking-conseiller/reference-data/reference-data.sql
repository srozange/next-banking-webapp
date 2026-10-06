-- Données de référence du service open-banking-conseiller : les 13 produits actifs du catalogue du legacy
-- (docs/retro/features/FEAT-011/screens/SCR-011-01_charge.png), identifiants 1 à 13 : 4 d'épargne, 4 de crédit,
-- 5 d'investissement. Taux, rendements et LTV en fractions décimales (0.0300 = 3 %). Devise EUR.
-- Hors migrations : chargé dans une base déjà migrée par Flyway.
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (1, 'EPARGNE', true, 'EUR', 'Compte reglemente pour projet immobilier. Permet d''obtenir un pret CEL bonifie.', 0.0175, 'CEL - Compte Epargne Logement');
insert into savings_products (id, capital_guaranteed, max_duration_months, min_opening_deposit) values (1, true, null, 300.00);
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (2, 'EPARGNE', true, 'EUR', 'Livret reglemente garanti par l''Etat. Disponibilite immediate, exonere d''impots.', 0.0300, 'Livret A');
insert into savings_products (id, capital_guaranteed, max_duration_months, min_opening_deposit) values (2, true, null, 10.00);
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (3, 'EPARGNE', true, 'EUR', 'Livret d''epargne a taux bonifie. Sans frais, sans duree minimum.', 0.0250, 'Livret Demo Bank');
insert into savings_products (id, capital_guaranteed, max_duration_months, min_opening_deposit) values (3, true, null, 100.00);
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (4, 'EPARGNE', true, 'EUR', 'Plan reglemente sur 4 a 10 ans. Taux garanti, prime de l''Etat sous conditions.', 0.0225, 'PEL - Plan Epargne Logement');
insert into savings_products (id, capital_guaranteed, max_duration_months, min_opening_deposit) values (4, true, 120, 225.00);
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (5, 'CREDIT', true, 'EUR', 'Financement vehicule neuf ou occasion. Taux preferentiel concessionnaire.', 0.0449, 'Credit Auto Classique');
insert into credit_products (id, max_duration_months, max_ltv, requires_insurance) values (5, 72, null, false);
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (6, 'CREDIT', true, 'EUR', 'Financement acquisition ou travaux. Taux fixe, avec ou sans apport.', 0.0365, 'Credit Immobilier Classique');
insert into credit_products (id, max_duration_months, max_ltv, requires_insurance) values (6, 300, 0.8000, true);
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (7, 'CREDIT', true, 'EUR', 'Credit affecte ou non. Financement projet personnel, travaux, permis, etc.', 0.0598, 'Credit Personnel');
insert into credit_products (id, max_duration_months, max_ltv, requires_insurance) values (7, 84, null, false);
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (8, 'CREDIT', true, 'EUR', 'Regroupement de credits en cours pour reduire les mensualites.', 0.0489, 'Rachat de Credits');
insert into credit_products (id, max_duration_months, max_ltv, requires_insurance) values (8, 180, 0.7000, true);
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (9, 'INVESTISSEMENT', true, 'EUR', 'Fonds indiciel mondial (MSCI World). Exposition actions internationales.', null, 'ETF Monde Diversifie');
insert into investment_products (id, expected_return, market_sector, risk_level) values (9, 0.0720, 'Actions mondiales', 3);
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (10, 'INVESTISSEMENT', true, 'EUR', 'OPCVM obligataire a capital garanti. Rendement stable et securise.', null, 'Fonds Euro Garanti');
insert into investment_products (id, expected_return, market_sector, risk_level) values (10, 0.0280, 'Obligations souveraines', 1);
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (11, 'INVESTISSEMENT', true, 'EUR', 'Plan Epargne en Actions. Investissement actions europeennes, fiscalite avantageuse.', null, 'PEA Actions Europe');
insert into investment_products (id, expected_return, market_sector, risk_level) values (11, 0.0950, 'Actions Europe', 4);
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (12, 'INVESTISSEMENT', true, 'EUR', 'Fonds de capital investissement PME. Horizon 8-10 ans, potentiel eleve.', null, 'Private Equity Croissance');
insert into investment_products (id, expected_return, market_sector, risk_level) values (12, 0.1500, 'Capital investissement', 5);
insert into bank_products (id, product_type, active, currency, description, interest_rate, name) values (13, 'INVESTISSEMENT', true, 'EUR', 'Societe civile de placement immobilier diversifiee. Revenus locatifs trimestriels.', null, 'SCPI Rendement Plus');
insert into investment_products (id, expected_return, market_sector, risk_level) values (13, 0.0510, 'Immobilier tertiaire', 2);
select setval(pg_get_serial_sequence('bank_products', 'id'), (select max(id) from bank_products));
