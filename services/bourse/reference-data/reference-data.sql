-- Données de référence du service bourse : les 9 lignes WATCHLIST_ITEMS de l'import.sql du legacy (watchlist
-- commune), identifiants, symboles, noms, marchés et devises repris tels quels (TTE.PA = 1 ... GOOGL = 9).
-- Hors migrations : chargé dans une base déjà migrée par Flyway (make reset-data), jamais en production.
-- Colonnes legacy sans équivalent cible : aucune.
insert into watchlist_items (id, symbol, name, market, currency) values (1, 'TTE.PA', 'TotalEnergies', 'Euronext Paris', 'EUR');
insert into watchlist_items (id, symbol, name, market, currency) values (2, 'MC.PA', 'LVMH', 'Euronext Paris', 'EUR');
insert into watchlist_items (id, symbol, name, market, currency) values (3, 'BNP.PA', 'BNP Paribas', 'Euronext Paris', 'EUR');
insert into watchlist_items (id, symbol, name, market, currency) values (4, 'OR.PA', 'L Oreal', 'Euronext Paris', 'EUR');
insert into watchlist_items (id, symbol, name, market, currency) values (5, 'AI.PA', 'Air Liquide', 'Euronext Paris', 'EUR');
insert into watchlist_items (id, symbol, name, market, currency) values (6, 'SAN.PA', 'Sanofi', 'Euronext Paris', 'EUR');
insert into watchlist_items (id, symbol, name, market, currency) values (7, 'AAPL', 'Apple', 'NASDAQ', 'USD');
insert into watchlist_items (id, symbol, name, market, currency) values (8, 'MSFT', 'Microsoft', 'NASDAQ', 'USD');
insert into watchlist_items (id, symbol, name, market, currency) values (9, 'GOOGL', 'Alphabet', 'NASDAQ', 'USD');
select setval(pg_get_serial_sequence('watchlist_items', 'id'), (select max(id) from watchlist_items));
