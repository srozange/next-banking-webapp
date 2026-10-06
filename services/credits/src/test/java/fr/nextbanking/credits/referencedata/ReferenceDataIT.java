package fr.nextbanking.credits.referencedata;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Données de référence : le fichier committé se charge dans une base migrée par Flyway (2 prêts EN_COURS de user1,
 * 18 échéances cohérentes), et le générateur produit le même contenu pour une date donnée. Chaque test travaille
 * dans un schéma neuf.
 */
@Tag("FR-007-08")
class ReferenceDataIT {

    private static final Path FICHIER = Path.of("reference-data/reference-data.sql");

    @Test
    void le_fichier_committe_charge_2_prets_en_cours_de_user1_et_18_echeances() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));

            assertThat(entier(instruction, "select count(*) from loans where customer_id = 1 and status = 'EN_COURS'"))
                    .isEqualTo(2);
            assertThat(entier(instruction, "select count(*) from loans")).isEqualTo(2);
            assertThat(entier(instruction, "select count(*) from loan_repayments")).isEqualTo(18);
            assertThat(entier(instruction, "select count(*) from loan_repayments r join loans l on l.id = r.loan_id"
                    + " where r.numero_echeance > l.duree_mois")).isZero();
            assertThat(entier(instruction, "select count(*) from loan_repayments where paye <> (date_paiement is not null)"))
                    .isZero();
        }
    }

    @Test
    void les_echeances_de_chaque_pret_soldent_son_capital() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));

            try (ResultSet lignes = instruction.executeQuery(
                    "select l.montant, sum(r.montant_capital) capital, min(r.capital_restant) restant"
                            + " from loans l join loan_repayments r on r.loan_id = l.id group by l.id, l.montant")) {
                int prets = 0;
                while (lignes.next()) {
                    prets++;
                    assertThat(lignes.getBigDecimal("capital")).isEqualByComparingTo(lignes.getBigDecimal("montant"));
                    assertThat(lignes.getBigDecimal("restant")).isEqualByComparingTo(BigDecimal.ZERO);
                }
                assertThat(prets).isEqualTo(2);
            }
        }
    }

    @Test
    void le_generateur_place_les_debuts_le_premier_d_un_mois_passe_et_paie_les_echeances_echues() throws Exception {
        String sql = GenerateurReferenceData.generer(LocalDate.of(2026, 10, 6));

        assertThat(sql).contains("date '2026-06-01'", "date '2026-08-01'");
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(sql);

            // Prêt 1 (début 2026-06-01) : échéances des 1er juillet, août, septembre, octobre payées sur 12 ;
            // prêt 2 (début 2026-08-01) : 1er septembre et 1er octobre.
            assertThat(entier(instruction, "select count(*) from loan_repayments where paye")).isEqualTo(6);
            assertThat(entier(instruction, "select count(*) from loans where date_debut > date '2026-10-06'")).isZero();
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
