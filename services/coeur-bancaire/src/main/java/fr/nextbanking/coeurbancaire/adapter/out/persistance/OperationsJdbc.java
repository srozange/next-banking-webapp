package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.domain.Operation;
import java.sql.Timestamp;
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
}
