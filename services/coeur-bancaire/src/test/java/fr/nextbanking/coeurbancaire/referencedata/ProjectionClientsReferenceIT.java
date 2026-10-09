package fr.nextbanking.coeurbancaire.referencedata;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Projection de référence des clients du cœur bancaire : le fichier committé se charge dans une base migrée par Flyway
 * (sept clients d'identite-clients, affectations aux conseillers comprises), et
 * {@code scripts/generer-projections-reference.sh} le régénère à l'identique depuis le {@code reference-data.sql}
 * d'identite-clients.
 */
@Tag("FR-015-02")
class ProjectionClientsReferenceIT {

    private static final Path FICHIER = Path.of("reference-data/projection-clients.sql");
    private static final Path SCRIPT = Path.of("../../scripts/generer-projections-reference.sh");

    @Test
    void le_fichier_committe_charge_les_sept_clients_du_legacy_avec_leur_conseiller_en_version_1() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));

            assertThat(entier(instruction, "select count(*) from clients")).isEqualTo(7);
            assertThat(entier(instruction, "select count(*) from clients where version_agregat = 1 and not supprime"))
                    .isEqualTo(7);
            assertThat(texte(instruction, "select string_agg(client_id::text, ',' order by client_id) from clients"
                    + " where conseiller_id = 11")).isEqualTo("1,2");
            assertThat(texte(instruction, "select string_agg(client_id::text, ',' order by client_id) from clients"
                    + " where conseiller_id = 12")).isEqualTo("3,4");
            assertThat(entier(instruction, "select count(*) from clients where client_id = 1"
                    + " and nom_complet = 'Jean Dupont'")).isEqualTo(1);
        }
    }

    @Test
    void le_script_regenere_le_fichier_a_l_identique_sans_recopier_de_mot_de_passe() throws Exception {
        String avant = Files.readString(FICHIER, StandardCharsets.UTF_8);
        try {
            Process processus = new ProcessBuilder("bash", SCRIPT.toString()).redirectErrorStream(true).start();
            String sortie = new String(processus.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(processus.waitFor()).as(sortie).isZero();

            assertThat(Files.readString(FICHIER, StandardCharsets.UTF_8)).isEqualTo(avant);
        } finally {
            Files.writeString(FICHIER, avant, StandardCharsets.UTF_8);
        }
        assertThat(avant).doesNotContain("bcrypt", "password");
    }

    private static int entier(Statement instruction, String requete) throws SQLException {
        try (ResultSet ligne = instruction.executeQuery(requete)) {
            ligne.next();
            return ligne.getInt(1);
        }
    }

    private static String texte(Statement instruction, String requete) throws SQLException {
        try (ResultSet ligne = instruction.executeQuery(requete)) {
            ligne.next();
            return ligne.getString(1);
        }
    }

    private static String schemaNeuf() {
        String schema = "proj_" + UUID.randomUUID().toString().replace("-", "");
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
