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
 * Données de référence du cœur bancaire : le fichier committé reprend les lignes de l'import.sql du legacy (7 comptes,
 * 45 opérations, 3 virements programmés, 5 bénéficiaires, 6 alertes, 6 plafonds, aucun virement), avec ses
 * identifiants, ses soldes et ses dates lues à l'heure de Paris, et se charge dans une base migrée par Flyway. Chaque
 * test travaille dans un schéma neuf.
 */
@Tag("NFR-PAR-04")
class ReferenceDataIT {

    private static final Path FICHIER = Path.of("reference-data/reference-data.sql");

    @Test
    void le_fichier_committe_charge_les_lignes_de_l_import_table_par_table() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));

            assertThat(entier(instruction, "select count(*) from comptes")).isEqualTo(7);
            assertThat(entier(instruction, "select count(*) from operations")).isEqualTo(45);
            assertThat(entier(instruction, "select count(*) from operations where virement_id is not null")).isZero();
            assertThat(entier(instruction, "select count(*) from virements")).isZero();
            assertThat(entier(instruction, "select count(*) from virements_programmes")).isEqualTo(3);
            assertThat(entier(instruction, "select count(*) from beneficiaires_externes")).isEqualTo(5);
            assertThat(entier(instruction, "select count(*) from alertes_fraude")).isEqualTo(6);
            assertThat(entier(instruction, "select count(*) from plafonds_budget")).isEqualTo(6);
        }
    }

    @Test
    void les_valeurs_exigees_par_le_golden_master_sont_presentes() throws Exception {
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));

            // Comptes 1 et 2 de user1, soldes de l'import (même incohérents avec les opérations).
            assertThat(texte(instruction, "select string_agg(id || ':' || numero || ':' || type || ':' || solde, ','"
                    + " order by id) from comptes where client_id = 1"))
                    .isEqualTo("1:FR76-0001-0001:COURANT:2450.00,2:FR76-0001-0002:EPARGNE:15000.00");
            // Bénéficiaire SEPA en FR76 de user1.
            assertThat(entier(instruction, "select count(*) from beneficiaires_externes where client_id = 1"
                    + " and iban like 'FR76%' and zone_sepa and actif")).isEqualTo(3);
            // Compte interne FR76-0001-0002, destinataire du virement programmé d'épargne.
            assertThat(texte(instruction, "select numero_beneficiaire || ':' || libelle_beneficiaire || ':'"
                    + " || frequence || ':' || nb_executions from virements_programmes where id = 1"))
                    .isEqualTo("FR76-0001-0002:Compte Epargne Demo Bank:MENSUEL:2");
            // 24 opérations sur le COURANT de user1 ; la plus récente est celle du 27/11/2025 à 19h à Paris.
            assertThat(entier(instruction, "select count(*) from operations where compte_id = 1")).isEqualTo(24);
            assertThat(texte(instruction, "select description || ':' || to_char(date_operation at time zone"
                    + " 'Europe/Paris', 'YYYY-MM-DD HH24:MI') from operations where compte_id = 1"
                    + " order by date_operation desc limit 1")).isEqualTo("Cinema UGC - sorties loisirs:2025-11-27 19:00");
            assertThat(texte(instruction, "select string_agg(categorie || '=' || plafond, ',' order by id)"
                    + " from plafonds_budget where client_id = 1 and mois = '2025-11'"))
                    .isEqualTo("LOGEMENT=1000.00,ALIMENTATION=250.00,TRANSPORT=150.00,ABONNEMENTS=80.00,"
                            + "LOISIRS=100.00,SANTE=50.00");
            assertThat(texte(instruction, "select regle || ':' || statut || ':' || coalesce(revu_par, '-')"
                    + " from alertes_fraude where id = 1")).isEqualTo("LARGE_AMOUNT:CONFIRMED:ADMIN");
        }
    }

    @Test
    void aucune_date_relative_et_les_sequences_reprennent_apres_les_plus_grands_identifiants() throws Exception {
        String sql = Files.readString(FICHIER, StandardCharsets.UTF_8).toLowerCase();
        assertThat(sql).doesNotContain("now()", "current_date", "current_timestamp");
        try (Connection connexion = connexion(schemaNeuf()); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));
            instruction.execute("insert into operations (compte_id, type, montant, solde_apres, description,"
                    + " date_operation) values (1, 'CREDIT', 1, 1, 'x', timestamptz '2026-11-17 10:00:00+01')");

            assertThat(entier(instruction, "select max(id) from operations")).isEqualTo(305);
            assertThat(entier(instruction, "select nextval(pg_get_serial_sequence('comptes', 'id'))")).isEqualTo(8);
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
