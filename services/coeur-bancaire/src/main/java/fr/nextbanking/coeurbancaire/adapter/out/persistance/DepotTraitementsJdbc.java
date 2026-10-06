package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.DepotTraitements;
import fr.nextbanking.coeurbancaire.domain.ExecutionTraitement;
import fr.nextbanking.coeurbancaire.domain.StatutTraitement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : lecture du journal de la table {@code batch_job_logs}, alimentée par {@link JournalBatchJdbc}.
 */
@Component
public class DepotTraitementsJdbc implements DepotTraitements {

    private final JdbcClient jdbc;

    public DepotTraitementsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ExecutionTraitement> dernieres(int limite) {
        return jdbc.sql("select id, job, debut, fin, enregistrements, statut, details"
                        + " from batch_job_logs order by debut desc nulls last, id desc limit :limite")
                .param("limite", limite)
                .query(DepotTraitementsJdbc::execution)
                .list();
    }

    private static ExecutionTraitement execution(ResultSet ligne, int numero) throws SQLException {
        String statut = ligne.getString("statut");
        return new ExecutionTraitement(ligne.getLong("id"), ligne.getString("job"),
                instant(ligne.getTimestamp("debut")), instant(ligne.getTimestamp("fin")),
                ligne.getObject("enregistrements", Integer.class),
                statut == null ? null : StatutTraitement.valueOf(statut), ligne.getString("details"));
    }

    private static Instant instant(Timestamp horodatage) {
        return horodatage == null ? null : horodatage.toInstant();
    }
}
