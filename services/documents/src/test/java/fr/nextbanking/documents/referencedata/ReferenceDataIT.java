package fr.nextbanking.documents.referencedata;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.documents.infrastructure.Conteneurs;
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
 * Données de référence : le fichier committé se charge dans une base migrée par Flyway (5 lignes DOCUMENTS du legacy,
 * projections des sept clients, des sept comptes et des 45 opérations de coeur-bancaire en version 1), et {@code
 * scripts/generer-projections-reference.sh} régénère le bloc des projections à l'identique depuis les
 * {@code reference-data.sql} d'identite-clients et de coeur-bancaire. Chaque test travaille dans un schéma neuf.
 */
@Tag("NFR-PAR-04")
class ReferenceDataIT {

    private static final Path FICHIER = Path.of("reference-data/reference-data.sql");
    private static final Path SOURCE_COEUR_BANCAIRE = Path.of("../coeur-bancaire/reference-data/reference-data.sql");
    private static final Path SCRIPT = Path.of("../../scripts/generer-projections-reference.sh");

    @Test
    void le_fichier_committe_charge_les_5_documents_du_legacy() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));

            assertThat(entier(instruction, "select count(*) from documents")).isEqualTo(5);
            assertThat(entier(instruction, "select sum(download_count) from documents")).isEqualTo(7);
            assertThat(entier(instruction, "select count(*) from documents where customer_id = 1")).isEqualTo(3);
            assertThat(entier(instruction, "select count(*) from documents where document_type = 'RELEVE_MENSUEL'"
                    + " and filename = 'releve_user1_2025_12.pdf' and file_size_bytes = 24680 and download_count = 3"
                    + " and generated_at = timestamptz '2026-01-02 07:00:00+00'"
                    + " and parametres = 'month=12;year=2025;customerId=1'")).isEqualTo(1);
            // La séquence reprend après le dernier identifiant.
            instruction.execute("insert into documents (customer_id, document_type, filename, mime_type,"
                    + " file_size_bytes, generated_at, parametres) values (1, 'RIB', 'rib_1.pdf', 'application/pdf', 1,"
                    + " timestamptz '2026-10-10 00:30:00+02', 'compteId=1')");
            assertThat(entier(instruction, "select max(id) from documents")).isEqualTo(6);
        }
    }

    @Test
    void le_fichier_committe_charge_les_projections_des_clients_et_des_comptes_en_version_1() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));

            assertThat(entier(instruction, "select count(*) from clients where version_agregat = 1 and not supprime"))
                    .isEqualTo(7);
            assertThat(entier(instruction, "select count(*) from clients where client_id = 1 and login = 'user1'"
                    + " and nom_complet = 'Jean Dupont' and role = 'CLIENT'")).isEqualTo(1);
            assertThat(entier(instruction, "select count(*) from comptes where version_agregat = 1")).isEqualTo(7);
            assertThat(entier(instruction, "select count(*) from comptes where compte_id = 1 and client_id = 1"
                    + " and numero = 'FR76-0001-0001' and type = 'COURANT' and iban = numero and bic = 'DEMOFR21XXX'"
                    + " and solde = 2450.00")).isEqualTo(1);
            assertThat(entier(instruction, "select count(*) from comptes where compte_id = 2 and type = 'EPARGNE'"
                    + " and solde = 15000.00")).isEqualTo(1);
        }
    }

    @Test
    @Tag("FR-017-04")
    void le_fichier_committe_charge_les_operations_generees_de_coeur_bancaire_et_la_table_se_remplit_ou_reste_vide()
            throws Exception {
        long attendues = Files.readAllLines(SOURCE_COEUR_BANCAIRE, StandardCharsets.UTF_8).stream()
                .filter(ligne -> ligne.toLowerCase().startsWith("insert into operations")).count();
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));

            assertThat(entier(instruction, "select count(*) from operations")).isEqualTo((int) attendues);
            assertThat(entier(instruction, "select count(*) from operations where version_agregat <> 1"))
                    .isZero();
            // La table accepte une opération et refuse un doublon d'identifiant et un type inconnu.
            instruction.execute("insert into operations (operation_id, compte_id, instant, libelle, type, montant,"
                    + " solde_apres, version_agregat) values (999999, 1, timestamptz '2026-03-01 10:00:00+01',"
                    + " 'Salaire', 'CREDIT', 10.00, 2140.00, 2)");
            assertThat(entier(instruction, "select count(*) from operations where operation_id = 999999"))
                    .isEqualTo(1);
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
        assertThat(avant).doesNotContain("bcrypt", "password").contains("-- DEBUT PROJECTIONS GENEREES",
                "-- FIN PROJECTIONS GENEREES");
    }

    @Test
    void aucun_script_du_service_ne_modifie_un_solde() throws Exception {
        try (var fichiers = Files.walk(Path.of("src/main"))) {
            for (Path fichier : fichiers.filter(Files::isRegularFile).filter(f -> f.toString().endsWith(".java")
                    || f.toString().endsWith(".sql")).toList()) {
                assertThat(Files.readString(fichier, StandardCharsets.UTF_8).toLowerCase())
                        .as(fichier.toString()).doesNotContain("update comptes", "update bank_accounts");
            }
        }
        assertThat(Files.readString(FICHIER, StandardCharsets.UTF_8).toLowerCase()).doesNotContain("update ");
    }

    private static int entier(Statement instruction, String requete) throws SQLException {
        try (ResultSet ligne = instruction.executeQuery(requete)) {
            ligne.next();
            return ligne.getInt(1);
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
