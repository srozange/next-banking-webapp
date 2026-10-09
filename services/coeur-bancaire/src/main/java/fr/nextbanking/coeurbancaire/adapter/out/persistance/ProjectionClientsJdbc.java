package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.ProjectionClients;
import fr.nextbanking.coeurbancaire.domain.ClientProjete;
import java.sql.Types;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : table {@code clients}, projection locale du nom complet, du conseiller affecté et de la
 * suppression. L'insertion ou la mise à jour n'a lieu que si {@code version_agregat} reçue est strictement supérieure
 * à celle de la ligne.
 */
@Component
public class ProjectionClientsJdbc implements ProjectionClients {

    private final JdbcClient jdbc;

    public ProjectionClientsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean enregistrerSiPlusRecent(ClientProjete client) {
        return jdbc.sql("""
                insert into clients (client_id, nom_complet, conseiller_id, supprime, version_agregat)
                values (:clientId, :nomComplet, :conseillerId, :supprime, :version)
                on conflict (client_id) do update
                set nom_complet = excluded.nom_complet, conseiller_id = excluded.conseiller_id,
                    supprime = excluded.supprime, version_agregat = excluded.version_agregat
                where clients.version_agregat < excluded.version_agregat""")
                .param("clientId", client.clientId())
                .param("nomComplet", client.nomComplet())
                .param("conseillerId", client.conseillerId(), Types.BIGINT)
                .param("supprime", client.supprime())
                .param("version", client.versionAgregat())
                .update() == 1;
    }

    @Override
    public List<Long> clientsDuConseiller(long conseillerId) {
        return jdbc.sql("select client_id from clients where conseiller_id = :conseiller and not supprime"
                        + " order by client_id")
                .param("conseiller", conseillerId)
                .query(Long.class)
                .list();
    }
}
