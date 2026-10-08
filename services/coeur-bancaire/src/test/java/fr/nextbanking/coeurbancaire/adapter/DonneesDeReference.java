package fr.nextbanking.coeurbancaire.adapter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Remet la base dans l'état des données de référence du service ({@code reference-data/reference-data.sql}), plus un
 * compte {@code 3} (COURANT) d'un autre client {@code 8} pour les contrôles de propriété.
 */
public final class DonneesDeReference {

    private static final Path FICHIER = Path.of("reference-data/reference-data.sql");

    private DonneesDeReference() {
    }

    /** Vide les tables métier, outbox, outbox d'audit et événements traités, puis recharge les données. */
    public static void recharger(DataSource source, JdbcClient jdbc) throws Exception {
        vider(jdbc);
        try (Connection connexion = source.getConnection()) {
            ScriptUtils.executeSqlScript(connexion,
                    new ByteArrayResource(Files.readAllBytes(FICHIER)) {
                        @Override
                        public String getFilename() {
                            return "reference-data.sql";
                        }
                    });
        }
        jdbc.sql("insert into comptes (id, numero, type, solde, client_id) values (3, 'FR76-0002-0001', 'COURANT', 500.00, 8)")
                .update();
        jdbc.sql("select setval(pg_get_serial_sequence('comptes', 'id'), 3)").query(Long.class).single();
    }

    /**
     * Vide les tables écrites par les tests : la base de test est partagée, et d'autres tests retrouvent leurs lignes
     * d'outbox par leur clé.
     */
    public static void vider(JdbcClient jdbc) {
        jdbc.sql("truncate table virements_programmes, operations, alertes_fraude, beneficiaires_externes, virements, comptes, outbox, outbox_audit, evenements_traites, clients, batch_job_logs "
                + "restart identity cascade").update();
    }

    /** Charge utile du fichier pour les tests qui le lisent sans base. */
    public static String sql() throws Exception {
        return Files.readString(FICHIER, StandardCharsets.UTF_8);
    }
}
