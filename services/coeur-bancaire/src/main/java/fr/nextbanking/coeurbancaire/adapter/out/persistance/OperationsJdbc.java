package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.TypeOperation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : table {@code operations} (TRANSACTIONS du legacy).
 */
@Component
public class OperationsJdbc implements Operations {

    private final JdbcClient jdbc;

    public OperationsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void enregistrer(Operation operation) {
        jdbc.sql("""
                insert into operations (compte_id, type, montant, solde_apres, description, date_operation, virement_id)
                values (:compteId, :type, :montant, :soldeApres, :description, :date, :virementId)""")
                .param("compteId", operation.compteId())
                .param("type", operation.type().name())
                .param("montant", operation.montant())
                .param("soldeApres", operation.soldeApres())
                .param("description", operation.description())
                .param("date", Timestamp.from(operation.date()))
                .param("virementId", operation.virementId())
                .update();
    }

    @Override
    public List<Operation> duCompte(long compteId) {
        return jdbc.sql("""
                select compte_id, type, montant, solde_apres, description, date_operation, virement_id
                from operations where compte_id = :compteId order by date_operation desc, id desc""")
                .param("compteId", compteId)
                .query(OperationsJdbc::operation)
                .list();
    }

    private static Operation operation(ResultSet ligne, int numero) throws SQLException {
        return new Operation(ligne.getLong("compte_id"), TypeOperation.valueOf(ligne.getString("type")),
                ligne.getBigDecimal("montant"), ligne.getBigDecimal("solde_apres"), ligne.getString("description"),
                ligne.getTimestamp("date_operation").toInstant(), ligne.getLong("virement_id"));
    }
}
