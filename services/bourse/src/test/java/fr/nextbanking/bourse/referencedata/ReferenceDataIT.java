package fr.nextbanking.bourse.referencedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import fr.nextbanking.bourse.infrastructure.Conteneurs;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Données de référence : le fichier committé se charge dans une base migrée par Flyway (les 9 valeurs de l'import.sql
 * du legacy, avec ses identifiants : TTE.PA = 1 ... GOOGL = 9), et la séquence reprend après. Chaque test travaille dans un schéma neuf.
 */
@Tag("FR-009-10")
@Tag("NFR-PAR-04")
class ReferenceDataIT {

    private static final Path FICHIER = Path.of("reference-data/reference-data.sql");

    private record Ligne(long id, String symbol, String name, String market, String currency) {
    }

    @Test
    void le_fichier_committe_charge_les_9_valeurs_du_legacy() throws Exception {
        List<Ligne> lignes = new ArrayList<>();
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));
            try (ResultSet resultat = instruction.executeQuery(
                    "select id, symbol, name, market, currency from watchlist_items order by id")) {
                while (resultat.next()) {
                    lignes.add(new Ligne(resultat.getLong(1), resultat.getString(2), resultat.getString(3),
                            resultat.getString(4), resultat.getString(5)));
                }
            }
        }

        assertThat(lignes).extracting(Ligne::id, Ligne::symbol, Ligne::name, Ligne::market, Ligne::currency)
                .containsExactly(
                        tuple(1L, "TTE.PA", "TotalEnergies", "Euronext Paris", "EUR"),
                        tuple(2L, "MC.PA", "LVMH", "Euronext Paris", "EUR"),
                        tuple(3L, "BNP.PA", "BNP Paribas", "Euronext Paris", "EUR"),
                        tuple(4L, "OR.PA", "L Oreal", "Euronext Paris", "EUR"),
                        tuple(5L, "AI.PA", "Air Liquide", "Euronext Paris", "EUR"),
                        tuple(6L, "SAN.PA", "Sanofi", "Euronext Paris", "EUR"),
                        tuple(7L, "AAPL", "Apple", "NASDAQ", "USD"),
                        tuple(8L, "MSFT", "Microsoft", "NASDAQ", "USD"),
                        tuple(9L, "GOOGL", "Alphabet", "NASDAQ", "USD"));
    }

    @Test
    void les_migrations_seules_ne_chargent_aucune_valeur() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement();
                ResultSet resultat = instruction.executeQuery("select count(*) from watchlist_items")) {
            resultat.next();
            assertThat(resultat.getLong(1)).isZero();
        }
    }

    @Test
    void la_sequence_reprend_apres_le_plus_grand_identifiant() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));
            try (ResultSet ligne = instruction.executeQuery("insert into watchlist_items (symbol, name, market,"
                    + " currency) values ('SG.PA', 'Société Générale', 'Euronext Paris', 'EUR') returning id")) {
                ligne.next();
                assertThat(ligne.getLong(1)).isEqualTo(10L);
            }
        }
    }

    private static String schemaNeuf() {
        String schema = "ref_" + UUID.randomUUID().toString().replace("-", "");
        Flyway.configure()
                .dataSource(Conteneurs.POSTGRES.getJdbcUrl(), Conteneurs.POSTGRES.getUsername(),
                        Conteneurs.POSTGRES.getPassword())
                .schemas(schema)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        return schema;
    }

    private static Connection connexion(String schema) throws SQLException {
        Connection connexion = DriverManager.getConnection(Conteneurs.POSTGRES.getJdbcUrl(),
                Conteneurs.POSTGRES.getUsername(), Conteneurs.POSTGRES.getPassword());
        try (Statement instruction = connexion.createStatement()) {
            instruction.execute("set search_path to " + schema);
        }
        return connexion;
    }
}
