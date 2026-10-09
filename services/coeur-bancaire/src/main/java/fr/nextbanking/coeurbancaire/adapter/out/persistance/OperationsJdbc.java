package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.TypeOperation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
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
    public long enregistrer(Operation operation) {
        return jdbc.sql("""
                insert into operations (compte_id, type, montant, solde_apres, description, date_operation, virement_id)
                values (:compteId, :type, :montant, :soldeApres, :description, :date, :virementId)
                returning id""")
                .param("compteId", operation.compteId())
                .param("type", operation.type().name())
                .param("montant", operation.montant().valeur())
                .param("soldeApres", operation.soldeApres().valeur())
                .param("description", operation.description())
                .param("date", Timestamp.from(operation.date()))
                .param("virementId", operation.virementId())
                .query(Long.class)
                .single();
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

    @Override
    public List<YearMonth> moisAvecOperations(long clientId, ZoneId fuseau, int limite) {
        return jdbc.sql("""
                select distinct to_char(o.date_operation at time zone :fuseau, 'YYYY-MM') as mois
                from operations o join comptes c on c.id = o.compte_id
                where c.client_id = :clientId order by mois desc limit :limite""")
                .param("fuseau", fuseau.getId())
                .param("clientId", clientId)
                .param("limite", limite)
                .query((ligne, numero) -> YearMonth.parse(ligne.getString("mois")))
                .list();
    }

    @Override
    public List<Operation> duClientSurPeriode(long clientId, Instant debut, Instant fin) {
        return jdbc.sql("""
                select o.compte_id, o.type, o.montant, o.solde_apres, o.description, o.date_operation, o.virement_id
                from operations o join comptes c on c.id = o.compte_id
                where c.client_id = :clientId and o.date_operation >= :debut and o.date_operation < :fin
                order by o.date_operation, o.id""")
                .param("clientId", clientId)
                .param("debut", Timestamp.from(debut))
                .param("fin", Timestamp.from(fin))
                .query(OperationsJdbc::operation)
                .list();
    }

    private static Operation operation(ResultSet ligne, int numero) throws SQLException {
        return new Operation(ligne.getLong("compte_id"), TypeOperation.valueOf(ligne.getString("type")),
                new Montant(ligne.getBigDecimal("montant")),
                new Montant(ligne.getBigDecimal("solde_apres")), ligne.getString("description"),
                ligne.getTimestamp("date_operation").toInstant(), ligne.getLong("virement_id"));
    }
}
