package fr.nextbanking.credits.referencedata;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.infrastructure.Conteneurs;
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
 * Données de référence : le fichier committé reprend les lignes de l'import.sql du legacy (2 prêts EN_COURS de user1,
 * 18 échéances, 4 scores de crédit, 4 profils de risque) et se charge dans une base migrée par Flyway, sans date
 * relative. Chaque test travaille dans un schéma neuf.
 */
@Tag("FR-007-08")
@Tag("NFR-PAR-04")
class ReferenceDataIT {

    private static final Path FICHIER = Path.of("reference-data/reference-data.sql");

    @Test
    void le_fichier_committe_charge_les_lignes_de_l_import() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));

            assertThat(entier(instruction, "select count(*) from loans where customer_id = 1 and status = 'EN_COURS'"))
                    .isEqualTo(2);
            assertThat(entier(instruction, "select count(*) from loans")).isEqualTo(2);
            assertThat(entier(instruction, "select count(*) from loan_repayments")).isEqualTo(18);
            assertThat(entier(instruction, "select count(*) from credit_scores")).isEqualTo(4);
            assertThat(entier(instruction, "select count(*) from risk_profiles")).isEqualTo(4);
            assertThat(entier(instruction, "select count(*) from loan_repayments r join loans l on l.id = r.loan_id"
                    + " where r.numero_echeance > l.duree_mois")).isZero();
            assertThat(entier(instruction, "select count(*) from loan_repayments where paye <> (date_paiement is not null)"))
                    .isZero();
        }
    }

    @Test
    void les_prets_et_les_echeances_sont_ceux_de_l_import() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));

            assertThat(texte(instruction, "select type || ' ' || montant || ' ' || duree_mois || ' ' || mensualite"
                    + " || ' ' || date_debut || ' ' || date_fin from loans where id = 1"))
                    .isEqualTo("AUTO 12000.00 48 268.69 2025-01-01 2029-01-01");
            assertThat(texte(instruction, "select type || ' ' || montant || ' ' || duree_mois || ' ' || mensualite"
                    + " || ' ' || motif from loans where id = 2"))
                    .isEqualTo("TRAVAUX 5000.00 24 218.63 Renovation cuisine");
            assertThat(entier(instruction, "select count(*) from loan_repayments where paye")).isEqualTo(15);
            assertThat(texte(instruction, "select capital_restant || ' ' || date_echeance from loan_repayments"
                    + " where id = 18")).isEqualTo("3798.45 2025-12-01");
            assertThat(texte(instruction, "select to_char(date_paiement at time zone 'Europe/Paris',"
                    + " 'YYYY-MM-DD HH24:MI') from loan_repayments where id = 1")).isEqualTo("2025-02-01 10:00");
            assertThat(texte(instruction, "select score || ' ' || category || ' ' || total_debt from credit_scores"
                    + " where customer_id = 3")).isEqualTo("548 MEDIUM 41200.00");
            assertThat(texte(instruction, "select risk_level || ' ' || patrimoine from risk_profiles"
                    + " where customer_id = 4")).isEqualTo("DYNAMIC 248000.00");
        }
    }

    @Test
    void les_sequences_reprennent_apres_les_plus_grands_identifiants() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));
            instruction.execute("insert into loans (customer_id, type, montant, taux_annuel, duree_mois, mensualite,"
                    + " cout_total, total_interets, status, date_debut, date_fin) values (2, 'ETUDIANT', 100, 1, 1,"
                    + " 100, 100, 0, 'EN_ATTENTE', date '2026-10-01', date '2026-11-01')");

            assertThat(entier(instruction, "select max(id) from loans")).isEqualTo(3);
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
