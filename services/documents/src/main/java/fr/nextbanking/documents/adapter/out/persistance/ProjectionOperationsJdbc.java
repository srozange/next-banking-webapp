package fr.nextbanking.documents.adapter.out.persistance;

import fr.nextbanking.documents.application.port.out.ProjectionOperations;
import fr.nextbanking.documents.domain.OperationProjetee;
import fr.nextbanking.documents.domain.TypeOperation;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : projection locale des opérations, table {@code operations}. L'insertion ignore un identifiant
 * déjà connu. Le total des intérêts reprend la requête du legacy ({@code LIKE '%interet%'}, sensible à la casse) sur
 * tous les comptes du client de la projection des comptes.
 */
@Component
public class ProjectionOperationsJdbc implements ProjectionOperations {

    private final JdbcClient jdbc;

    public ProjectionOperationsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean enregistrerSiAbsente(OperationProjetee operation) {
        return jdbc.sql("""
                insert into operations (operation_id, compte_id, instant, libelle, type, montant, solde_apres,
                                        version_agregat)
                values (:operationId, :compteId, :instant, :libelle, :type, :montant, :soldeApres, :versionAgregat)
                on conflict (operation_id) do nothing""")
                .param("operationId", operation.operationId())
                .param("compteId", operation.compteId())
                .param("instant", Timestamp.from(operation.instant()))
                .param("libelle", operation.libelle())
                .param("type", operation.type().name())
                .param("montant", operation.montant())
                .param("soldeApres", operation.soldeApres())
                .param("versionAgregat", operation.versionAgregat())
                .update() == 1;
    }

    @Override
    public List<OperationProjetee> duCompte(long compteId, Instant debut, Instant fin) {
        return jdbc.sql("""
                select operation_id, compte_id, instant, libelle, type, montant, solde_apres, version_agregat
                from operations
                where compte_id = :compteId and instant >= :debut and instant < :fin
                order by instant, operation_id""")
                .param("compteId", compteId)
                .param("debut", Timestamp.from(debut))
                .param("fin", Timestamp.from(fin))
                .query((ligne, numero) -> new OperationProjetee(ligne.getLong("operation_id"),
                        ligne.getLong("compte_id"), ligne.getTimestamp("instant").toInstant(),
                        ligne.getString("libelle"), TypeOperation.valueOf(ligne.getString("type")),
                        ligne.getBigDecimal("montant"), ligne.getBigDecimal("solde_apres"),
                        ligne.getLong("version_agregat")))
                .list();
    }

    @Override
    public BigDecimal totalInteretsCredites(long clientId, Instant debut, Instant fin) {
        return jdbc.sql("""
                select coalesce(sum(o.montant), 0)
                from operations o join comptes c on c.compte_id = o.compte_id
                where c.client_id = :clientId and o.type = 'CREDIT' and o.libelle like '%interet%'
                  and o.instant >= :debut and o.instant < :fin""")
                .param("clientId", clientId)
                .param("debut", Timestamp.from(debut))
                .param("fin", Timestamp.from(fin))
                .query(BigDecimal.class)
                .single();
    }
}
