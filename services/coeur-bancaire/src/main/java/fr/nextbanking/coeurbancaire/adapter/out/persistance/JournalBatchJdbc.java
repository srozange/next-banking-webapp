package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.JournalBatch;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Adaptateur sortant : table {@code batch_job_logs} (BATCH_JOB_LOGS du legacy), une ligne par exécution notable. */
@Component
public class JournalBatchJdbc implements JournalBatch {

    private final JdbcClient jdbc;

    public JournalBatchJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void enregistrer(String job, Instant debut, Instant fin, int enregistrements, Statut statut,
            String details) {
        jdbc.sql("""
                insert into batch_job_logs (job, debut, fin, duree_ms, enregistrements, statut, details)
                values (:job, :debut, :fin, :dureeMs, :enregistrements, :statut, :details)""")
                .param("job", job)
                .param("debut", Timestamp.from(debut))
                .param("fin", Timestamp.from(fin))
                .param("dureeMs", Duration.between(debut, fin).toMillis())
                .param("enregistrements", enregistrements)
                .param("statut", statut.name())
                .param("details", details)
                .update();
    }
}
