package fr.nextbanking.identiteclients.referencedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.identiteclients.infrastructure.Conteneurs;
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
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Données de référence : le fichier committé se charge dans une base migrée par Flyway (7 utilisateurs du legacy,
 * empreintes {@code {bcrypt}}), et le générateur produit des empreintes qui vérifient les mots de passe donnés sans
 * les contenir. Chaque test travaille dans un schéma neuf.
 */
@Tag("FR-001-08")
@Tag("BUG-002")
class ReferenceDataIT {

    private static final Path FICHIER = Path.of("reference-data/reference-data.sql");

    private record Utilisateur(long id, String username, String fullName, String role, String password,
            Long advisorId) {
    }

    @Test
    void le_fichier_committe_charge_les_7_utilisateurs_du_legacy_aux_mots_de_passe_haches() throws Exception {
        List<Utilisateur> utilisateurs = charger(Files.readString(FICHIER, StandardCharsets.UTF_8));

        assertThat(utilisateurs).extracting(Utilisateur::id, Utilisateur::username, Utilisateur::fullName,
                        Utilisateur::role)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "user1", "Jean Dupont", "CLIENT"),
                        org.assertj.core.groups.Tuple.tuple(2L, "user2", "Marie Lebrun", "CLIENT"),
                        org.assertj.core.groups.Tuple.tuple(3L, "user3", "Pierre Bernard", "CLIENT"),
                        org.assertj.core.groups.Tuple.tuple(4L, "user4", "Lucie Martin", "CLIENT"),
                        org.assertj.core.groups.Tuple.tuple(10L, "admin", "Alice Martin", "ADMIN"),
                        org.assertj.core.groups.Tuple.tuple(11L, "sophie", "Sophie Durand", "CONSEILLER"),
                        org.assertj.core.groups.Tuple.tuple(12L, "marc", "Marc Lefevre", "CONSEILLER"));
        assertThat(utilisateurs).extracting(Utilisateur::advisorId)
                .containsExactly(11L, 11L, 12L, 12L, null, null, null);
        assertThat(utilisateurs).allSatisfy(u -> assertThat(u.password()).startsWith("{bcrypt}"));
    }

    @Test
    void le_generateur_produit_des_empreintes_qui_verifient_les_mots_de_passe_sans_les_contenir() throws Exception {
        Map<String, String> environnement = Map.of(
                "RETRO_ADMIN_PASSWORD", "admin-connu",
                "RETRO_CONSEILLER_PASSWORD", "conseiller-connu",
                "RETRO_CLIENT_PASSWORD", "client-connu");
        String sql = GenerateurReferenceData.generer(environnement::get);

        assertThat(sql).doesNotContain("admin-connu", "conseiller-connu", "client-connu");
        PasswordEncoder encodeur = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        Map<String, String> attendus = Map.of("ADMIN", "admin-connu", "CONSEILLER", "conseiller-connu",
                "CLIENT", "client-connu");
        assertThat(charger(sql)).hasSize(7).allSatisfy(u ->
                assertThat(encodeur.matches(attendus.get(u.role()), u.password())).as(u.username()).isTrue());
    }

    @Test
    @Tag("FR-001-08")
    @Tag("BUG-002")
    @EnabledIfEnvironmentVariable(named = "RETRO_ADMIN_PASSWORD", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "RETRO_CONSEILLER_PASSWORD", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "RETRO_CLIENT_PASSWORD", matches = ".+")
    void le_fichier_committe_a_ete_genere_avec_les_variables_retro() throws Exception {
        PasswordEncoder encodeur = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        Map<String, String> variables = Map.of("ADMIN", "RETRO_ADMIN_PASSWORD",
                "CONSEILLER", "RETRO_CONSEILLER_PASSWORD", "CLIENT", "RETRO_CLIENT_PASSWORD");

        List<Utilisateur> utilisateurs = charger(Files.readString(FICHIER, StandardCharsets.UTF_8));

        assertThat(utilisateurs).hasSize(7);
        for (Utilisateur u : utilisateurs) {
            // Message d'assertion limité à l'identifiant : la valeur de la variable n'est jamais affichée.
            if (!encodeur.matches(System.getenv(variables.get(u.role())), u.password())) {
                throw new AssertionError("empreinte de " + u.username() + " non conforme à " + variables.get(u.role()));
            }
        }
    }

    @Test
    void le_generateur_refuse_une_variable_absente() {
        assertThatThrownBy(() -> GenerateurReferenceData.generer(Map.of(
                "RETRO_ADMIN_PASSWORD", "a", "RETRO_CONSEILLER_PASSWORD", "c")::get))
                .hasMessage("variable d'environnement RETRO_CLIENT_PASSWORD absente");
    }

    @Test
    void la_sequence_reprend_apres_le_plus_grand_identifiant() throws Exception {
        String schema = schemaNeuf();
        try (Connection connexion = connexion(schema); Statement instruction = connexion.createStatement()) {
            instruction.execute(Files.readString(FICHIER, StandardCharsets.UTF_8));
            instruction.execute("insert into customers (username, password, full_name, email, role)"
                    + " values ('nouveau', '{bcrypt}x', 'Nouveau Client', 'n@example.com', 'CLIENT')");
            try (ResultSet ligne = instruction.executeQuery("select id from customers where username = 'nouveau'")) {
                ligne.next();
                assertThat(ligne.getLong(1)).isEqualTo(13L);
            }
        }
    }

    private static List<Utilisateur> charger(String sql) throws SQLException {
        String schema = schemaNeuf();
        List<Utilisateur> utilisateurs = new ArrayList<>();
        try (Connection connexion = connexion(schema); Statement instruction = connexion.createStatement()) {
            instruction.execute(sql);
            try (ResultSet lignes = instruction.executeQuery(
                    "select id, username, full_name, role, password, advisor_id from customers order by id")) {
                while (lignes.next()) {
                    long conseiller = lignes.getLong("advisor_id");
                    Long conseillerOuRien = lignes.wasNull() ? null : conseiller;
                    utilisateurs.add(new Utilisateur(lignes.getLong("id"), lignes.getString("username"),
                            lignes.getString("full_name"), lignes.getString("role"), lignes.getString("password"),
                            conseillerOuRien));
                }
            }
        }
        return utilisateurs;
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
