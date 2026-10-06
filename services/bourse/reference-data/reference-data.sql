-- Données de référence du service bourse : les 9 valeurs de la watchlist commune du legacy
-- (docs/retro/features/FEAT-009/screens/SCR-009-01_charge.png), identifiants 1 à 9.
-- Hors migrations : chargé dans une base déjà migrée par Flyway.
insert into watchlist_items (id, symbol, name, market, currency) values (1, 'AI.PA', 'Air Liquide', 'Euronext Paris', 'EUR');
insert into watchlist_items (id, symbol, name, market, currency) values (2, 'BNP.PA', 'BNP Paribas', 'Euronext Paris', 'EUR');
insert into watchlist_items (id, symbol, name, market, currency) values (3, 'MC.PA', 'LVMH', 'Euronext Paris', 'EUR');
insert into watchlist_items (id, symbol, name, market, currency) values (4, 'OR.PA', 'L Oreal', 'Euronext Paris', 'EUR');
insert into watchlist_items (id, symbol, name, market, currency) values (5, 'SAN.PA', 'Sanofi', 'Euronext Paris', 'EUR');
insert into watchlist_items (id, symbol, name, market, currency) values (6, 'TTE.PA', 'TotalEnergies', 'Euronext Paris', 'EUR');
insert into watchlist_items (id, symbol, name, market, currency) values (7, 'AAPL', 'Apple', 'NASDAQ', 'USD');
insert into watchlist_items (id, symbol, name, market, currency) values (8, 'GOOGL', 'Alphabet', 'NASDAQ', 'USD');
insert into watchlist_items (id, symbol, name, market, currency) values (9, 'MSFT', 'Microsoft', 'NASDAQ', 'USD');
select setval(pg_get_serial_sequence('watchlist_items', 'id'), (select max(id) from watchlist_items));
