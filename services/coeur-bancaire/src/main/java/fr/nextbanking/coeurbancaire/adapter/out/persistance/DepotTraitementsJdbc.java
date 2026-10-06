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
 * Adaptateur sortant : journal de la table {@code batch_job_logs}, lu sans être alimenté ici.
 */
@Component
public class DepotTraitementsJdbc implements DepotTraitements {

    private final JdbcClient jdbc;

    public DepotTraitementsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ExecutionTraitement> dernieres(int limite) {
        return jdbc.sql("select id, job_name, start_time, end_time, records_processed, status, details"
                        + " from batch_job_logs order by start_time desc nulls last, id desc limit :limite")
                .param("limite", limite)
                .query(DepotTraitementsJdbc::execution)
                .list();
    }

    private static ExecutionTraitement execution(ResultSet ligne, int numero) throws SQLException {
        String statut = ligne.getString("status");
        return new ExecutionTraitement(ligne.getLong("id"), ligne.getString("job_name"),
                instant(ligne.getTimestamp("start_time")), instant(ligne.getTimestamp("end_time")),
                ligne.getObject("records_processed", Integer.class),
                statut == null ? null : StatutTraitement.valueOf(statut), ligne.getString("details"));
    }

    private static Instant instant(Timestamp horodatage) {
        return horodatage == null ? null : horodatage.toInstant();
    }
}
