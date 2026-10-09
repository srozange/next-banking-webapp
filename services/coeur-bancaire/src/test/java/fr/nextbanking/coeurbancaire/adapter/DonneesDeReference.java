package fr.nextbanking.coeurbancaire.adapter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Remet la base dans l'état des données de référence du service ({@code reference-data/reference-data.sql}, lignes de
 * l'import.sql du legacy).
 * <ul>
 * <li>{@link #recharger} : socle des tests de comportement, les 7 comptes et les 5 bénéficiaires externes de l'import
 * seulement ; les opérations, virements programmés, alertes et plafonds de l'import n'y sont pas, chaque test pose les
 * siens. Les contrôles de propriété s'appuient sur le compte {@code 3} (COURANT de user2, client {@code 2}).</li>
 * <li>{@link #rechargerTout} : le fichier entier, pour les tests qui lisent les données de référence elles-mêmes.</li>
 * </ul>
 */
public final class DonneesDeReference {

    private static final Path FICHIER = Path.of("reference-data/reference-data.sql");

    /** Tables du socle des tests de comportement. */
    private static final List<String> TABLES_DU_SOCLE = List.of("comptes", "beneficiaires_externes");

    private DonneesDeReference() {
    }

    /** Vide les tables, puis charge les comptes et les bénéficiaires externes de l'import. */
    public static void recharger(DataSource source, JdbcClient jdbc) throws Exception {
        vider(jdbc);
        String socle = Files.readAllLines(FICHIER, StandardCharsets.UTF_8).stream()
                .filter(DonneesDeReference::concerneLeSocle)
                .collect(Collectors.joining("\n"));
        executer(source, socle);
    }

    /** Vide les tables, puis charge le fichier entier. */
    public static void rechargerTout(DataSource source, JdbcClient jdbc) throws Exception {
        vider(jdbc);
        executer(source, sql());
    }

    private static boolean concerneLeSocle(String ligne) {
        String texte = ligne.strip().toLowerCase();
        return TABLES_DU_SOCLE.stream().anyMatch(table -> texte.startsWith("insert into " + table + " (")
                || texte.startsWith("select setval(pg_get_serial_sequence('" + table + "',"));
    }

    private static void executer(DataSource source, String script) throws Exception {
        try (Connection connexion = source.getConnection()) {
            ScriptUtils.executeSqlScript(connexion,
                    new ByteArrayResource(script.getBytes(StandardCharsets.UTF_8)) {
                        @Override
                        public String getFilename() {
                            return "reference-data.sql";
                        }
                    });
        }
    }

    /**
     * Vide les tables écrites par les tests : la base de test est partagée, et d'autres tests retrouvent leurs lignes
     * d'outbox par leur clé.
     */
    public static void vider(JdbcClient jdbc) {
        jdbc.sql("truncate table plafonds_budget, echeances_notifiees, virements_programmes, operations, alertes_fraude, beneficiaires_externes, virements, comptes, outbox, outbox_audit, evenements_traites, clients, batch_job_logs "
                + "restart identity cascade").update();
    }

    /** Charge utile du fichier pour les tests qui le lisent sans base. */
    public static String sql() throws Exception {
        return Files.readString(FICHIER, StandardCharsets.UTF_8);
    }
}
