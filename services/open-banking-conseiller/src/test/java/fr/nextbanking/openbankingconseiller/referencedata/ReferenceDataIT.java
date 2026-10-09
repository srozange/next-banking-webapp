package fr.nextbanking.openbankingconseiller.referencedata;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.openbankingconseiller.infrastructure.Conteneurs;
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
 * Données de référence : le fichier committé se charge dans une base migrée par Flyway (13 produits actifs, 4 d'épargne,
 * 4 de crédit, 5 d'investissement, identifiants de l'import.sql du legacy : Livret A = 1 ... Private Equity Croissance
 * = 13), et la séquence reprend après. Chaque test travaille dans un
 * schéma neuf.
 */
@Tag("FR-011-02")
@Tag("NFR-PAR-04")
class ReferenceDataIT {

    private static final Path FICHIER = Path.of("reference-data/reference-data.sql");

    @Test
    void le_fichier_committe_charge_les_13_produits_du_legacy() throws Exception {
        List<String> lignes = new ArrayList<>();
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));
            try (ResultSet resultat = instruction.executeQuery("select product_type, count(*), bool_and(active),"
                    + " min(id), max(id) from bank_products group by product_type order by product_type")) {
                while (resultat.next()) {
                    lignes.add(resultat.getString(1) + ":" + resultat.getLong(2) + ":" + resultat.getBoolean(3)
                            + ":" + resultat.getLong(4) + "-" + resultat.getLong(5));
                }
            }
            assertThat(compter(instruction, "savings_products")).isEqualTo(4);
            assertThat(compter(instruction, "credit_products")).isEqualTo(4);
            assertThat(compter(instruction, "investment_products")).isEqualTo(5);
        }

        assertThat(lignes).containsExactly("CREDIT:4:true:5-8", "EPARGNE:4:true:1-4", "INVESTISSEMENT:5:true:9-13");
    }

    @Test
    void les_identifiants_et_les_valeurs_sont_ceux_de_l_import() throws Exception {
        List<String> lignes = new ArrayList<>();
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));
            try (ResultSet resultat = instruction.executeQuery("select b.id, b.name, b.interest_rate, b.min_amount,"
                    + " c.max_ltv from bank_products b left join credit_products c on c.id = b.id"
                    + " where b.id in (1, 3, 5, 9, 13) order by b.id")) {
                while (resultat.next()) {
                    lignes.add(resultat.getLong(1) + ":" + resultat.getString(2) + ":" + resultat.getString(3)
                            + ":" + resultat.getString(4) + ":" + resultat.getString(5));
                }
            }
        }

        assertThat(lignes).containsExactly(
                "1:Livret A:0.0300:0.00:null",
                "3:CEL - Compte Epargne Logement:0.0175:300.00:null",
                "5:Credit Immobilier Classique:0.0365:50000.00:0.8000",
                "9:Fonds Euro Garanti:0.0280:1000.00:null",
                "13:Private Equity Croissance:null:50000.00:null");
    }

    @Test
    void les_migrations_seules_ne_chargent_aucun_produit() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            assertThat(compter(instruction, "bank_products")).isZero();
        }
    }

    @Test
    void la_sequence_reprend_apres_le_plus_grand_identifiant() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));
            try (ResultSet ligne = instruction.executeQuery("insert into bank_products (product_type, name)"
                    + " values ('EPARGNE', 'Nouveau') returning id")) {
                ligne.next();
                assertThat(ligne.getLong(1)).isEqualTo(14L);
            }
        }
    }

    @Test
    void un_type_inconnu_est_refuse_par_la_base() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> instruction.execute(
                    "insert into bank_products (product_type, name) values ('AUTRE', 'x')"))
                    .isInstanceOf(SQLException.class);
        }
    }

    private static long compter(Statement instruction, String table) throws SQLException {
        try (ResultSet resultat = instruction.executeQuery("select count(*) from " + table)) {
            resultat.next();
            return resultat.getLong(1);
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
