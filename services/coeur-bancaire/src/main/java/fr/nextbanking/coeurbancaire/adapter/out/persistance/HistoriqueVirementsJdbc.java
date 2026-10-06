package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.HistoriqueVirements;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : signaux d'historique du contrôle de fraude, lus sur {@code operations}, {@code comptes} et
 * {@code virements}. Le verrou consultatif {@code pg_advisory_xact_lock(clientId)} est pris dans la transaction de
 * soumission et relâché avec elle.
 */
@Component
public class HistoriqueVirementsJdbc implements HistoriqueVirements {

    private final JdbcClient jdbc;

    public HistoriqueVirementsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void verrouillerClient(long clientId) {
        jdbc.sql("select pg_advisory_xact_lock(:clientId)").param("clientId", clientId).query().listOfRows();
    }

    @Override
    public long operationsDepuis(long clientId, Instant debut) {
        return jdbc.sql("""
                select count(*) from operations o join comptes c on c.id = o.compte_id
                where c.client_id = :clientId and o.date_operation >= :debut""")
                .param("clientId", clientId)
                .param("debut", Timestamp.from(debut))
                .query(Long.class)
                .single();
    }

    @Override
    public Optional<Instant> dernierVirement(long clientId) {
        return jdbc.sql("select soumis_le from virements where client_id = :clientId "
                + "order by soumis_le desc, id desc limit 1")
                .param("clientId", clientId)
                .query((ligne, numero) -> ligne.getTimestamp("soumis_le").toInstant())
                .optional();
    }

    @Override
    public long virementsExecutesAuDessus(long clientId, BigDecimal montantMin, Instant depuis) {
        return jdbc.sql("""
                select count(*) from virements
                where client_id = :clientId and etat = 'EXECUTE' and montant > :montantMin and soumis_le >= :depuis""")
                .param("clientId", clientId)
                .param("montantMin", montantMin)
                .param("depuis", Timestamp.from(depuis))
                .query(Long.class)
                .single();
    }
}
