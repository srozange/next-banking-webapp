package fr.nextbanking.notifications.referencedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.notifications.infrastructure.Conteneurs;
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
 * Données de référence des notifications : les 6 lignes de l'import.sql du legacy, sans origine (ni virement ni
 * virement programmé), se chargent dans une base migrée par Flyway ; la base refuse toujours deux origines. Chaque
 * test travaille dans un schéma neuf.
 */
@Tag("NFR-PAR-04")
class ReferenceDataIT {

    private static final Path FICHIER = Path.of("reference-data/reference-data.sql");

    @Test
    void le_fichier_committe_charge_les_6_notifications_de_l_import_avec_leur_texte_exact() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));

            assertThat(entier(instruction, "select count(*) from notifications")).isEqualTo(6);
            assertThat(entier(instruction, "select count(*) from notifications where virement_id is null"
                    + " and virement_programme_id is null")).isEqualTo(6);
            assertThat(entier(instruction, "select count(*) from notifications where client_id = 1")).isEqualTo(3);
            assertThat(texte(instruction, "select type || '|' || titre || '|' || message || '|' || lue || '|'"
                    + " || to_char(cree_le at time zone 'Europe/Paris', 'YYYY-MM-DD HH24:MI') from notifications"
                    + " where id = 1")).isEqualTo("TRANSACTION_ALERT|Virement important|Un virement de 5 200,00 EUR a"
                    + " été effectué depuis votre compte FR76-30001-00001-00000000001.|false|2026-05-20 11:00");
            assertThat(texte(instruction, "select message from notifications where id = 6")).isEqualTo(
                    "5 virements supérieurs à 500 EUR ont été détectés en moins d'une heure. Contactez votre"
                            + " conseiller si vous n'êtes pas à l'origine de ces opérations.");
            // La séquence d'identité reprend après le plus grand identifiant.
            instruction.execute("insert into notifications (virement_id, client_id, type, message, cree_le)"
                    + " values (1, 1, 'CONFIRMATION', 'x', timestamptz '2026-11-17 10:00:00+01')");
            assertThat(entier(instruction, "select max(id) from notifications")).isEqualTo(7);
        }
    }

    @Test
    void la_base_refuse_une_notification_a_deux_origines() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            assertThatThrownBy(() -> instruction.execute("insert into notifications (virement_id,"
                    + " virement_programme_id, client_id, type, message, cree_le) values (1, 2, 1, 'ALERTE', 'x',"
                    + " timestamptz '2026-11-17 10:00:00+01')")).isInstanceOf(SQLException.class)
                    .hasMessageContaining("notifications_au_plus_une_origine");
        }
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
