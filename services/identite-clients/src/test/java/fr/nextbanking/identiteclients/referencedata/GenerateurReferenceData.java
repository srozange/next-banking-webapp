package fr.nextbanking.identiteclients.referencedata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Générateur de {@code reference-data/reference-data.sql} (portée test, lancé par
 * {@code reference-data/generer-reference-data.sh}) : les 7 utilisateurs du legacy ({@code import.sql}) avec leurs
 * identifiants, chaque mot de passe remplacé par son empreinte {@code {bcrypt}} calculée depuis la variable
 * d'environnement de son rôle ({@code RETRO_ADMIN_PASSWORD}, {@code RETRO_CONSEILLER_PASSWORD},
 * {@code RETRO_CLIENT_PASSWORD}). Aucun mot de passe n'est écrit ni affiché.
 */
public final class GenerateurReferenceData {

    static final String ADMIN = "RETRO_ADMIN_PASSWORD";
    static final String CONSEILLER = "RETRO_CONSEILLER_PASSWORD";
    static final String CLIENT = "RETRO_CLIENT_PASSWORD";

    /**
     * Date de création posée sur chaque utilisateur, identique à celle des projections des autres services : sans
     * elle, la base prendrait la date du jour (défaut de la migration V3) et la donnée changerait d'un chargement à
     * l'autre.
     */
    static final String DATE_CREATION = "2026-10-06";

    /** Ligne de {@code customers} du legacy, sans mot de passe ; conseillers et administrateur d'abord (clé étrangère). */
    private record Ligne(long id, String username, String fullName, String email, String phone, String role,
            Long advisorId, String advisorSince, String variable) {
    }

    private static final List<Ligne> LIGNES = List.of(
            new Ligne(10, "admin", "Alice Martin", "alice.martin@demobank.fr", "+33612345670", "ADMIN",
                    null, null, ADMIN),
            new Ligne(11, "sophie", "Sophie Durand", "sophie.durand@demobank.fr", "+33612345671", "CONSEILLER",
                    null, null, CONSEILLER),
            new Ligne(12, "marc", "Marc Lefevre", "marc.lefevre@demobank.fr", "+33612345672", "CONSEILLER",
                    null, null, CONSEILLER),
            new Ligne(1, "user1", "Jean Dupont", "jean.dupont@example.com", "+33601020304", "CLIENT",
                    11L, "2024-01-15", CLIENT),
            new Ligne(2, "user2", "Marie Lebrun", "marie.lebrun@example.com", "+33601020305", "CLIENT",
                    11L, "2024-03-10", CLIENT),
            new Ligne(3, "user3", "Pierre Bernard", "pierre.bernard@example.com", "+33601020306", "CLIENT",
                    12L, "2024-02-20", CLIENT),
            new Ligne(4, "user4", "Lucie Martin", "lucie.martin@example.com", "+33601020307", "CLIENT",
                    12L, "2024-05-01", CLIENT));

    private GenerateurReferenceData() {
    }

    /** Écrit le fichier désigné par le premier argument (par défaut {@code reference-data/reference-data.sql}). */
    public static void main(String[] arguments) throws IOException {
        Path fichier = Path.of(arguments.length > 0 ? arguments[0] : "reference-data/reference-data.sql");
        Files.writeString(fichier, generer(System::getenv), StandardCharsets.UTF_8);
        System.out.println("reference-data.sql généré : " + LIGNES.size() + " utilisateurs, empreintes {bcrypt}");
    }

    /**
     * Rend le contenu de {@code reference-data.sql}.
     *
     * @param environnement lecture d'une variable d'environnement
     * @throws IllegalStateException si une variable est absente ou vide
     */
    public static String generer(UnaryOperator<String> environnement) {
        for (String variable : List.of(ADMIN, CONSEILLER, CLIENT)) {
            String valeur = environnement.apply(variable);
            if (valeur == null || valeur.isEmpty()) {
                throw new IllegalStateException("variable d'environnement " + variable + " absente");
            }
        }
        PasswordEncoder encodeur = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        StringBuilder sql = new StringBuilder("""
                -- Données de référence du service identite-clients : les 7 utilisateurs du legacy (import.sql),
                -- identifiants conservés. Hors migrations : chargé dans une base déjà migrée par Flyway.
                -- Généré par reference-data/generer-reference-data.sh : ne pas modifier à la main.
                -- Mots de passe : empreintes {bcrypt} calculées depuis RETRO_ADMIN_PASSWORD (admin),
                -- RETRO_CONSEILLER_PASSWORD (conseillers) et RETRO_CLIENT_PASSWORD (clients), jamais en clair.
                -- date_creation : date fixe, celle des projections des autres services (aucune date relative).
                """);
        for (Ligne ligne : LIGNES) {
            sql.append("insert into customers (id, username, password, full_name, email, phone, role, active,"
                    + " advisor_id, advisor_since, date_creation)\nvalues (")
                    .append(ligne.id()).append(", ")
                    .append(texte(ligne.username())).append(", ")
                    .append(texte(encodeur.encode(environnement.apply(ligne.variable())))).append(", ")
                    .append(texte(ligne.fullName())).append(", ")
                    .append(texte(ligne.email())).append(", ")
                    .append(texte(ligne.phone())).append(", ")
                    .append(texte(ligne.role())).append(", true, ")
                    .append(ligne.advisorId() == null ? "null" : ligne.advisorId().toString()).append(", ")
                    .append(ligne.advisorSince() == null ? "null" : "date " + texte(ligne.advisorSince()))
                    .append(", date ").append(texte(DATE_CREATION))
                    .append(");\n");
        }
        sql.append("select setval(pg_get_serial_sequence('customers', 'id'), (select max(id) from customers));\n");
        return sql.toString();
    }

    private static String texte(String valeur) {
        return "'" + valeur.replace("'", "''") + "'";
    }
}
