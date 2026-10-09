package fr.nextbanking.credits.adapter.out.persistance;

import fr.nextbanking.credits.application.port.out.ProjectionClients;
import fr.nextbanking.credits.domain.ClientProjete;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : projection locale des clients, table {@code clients} ; {@code version_agregat} écarte un
 * message ancien.
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
                insert into clients (client_id, login, nom_complet, role, actif, conseiller_id, date_creation,
                                     supprime, version_agregat)
                values (:clientId, :login, :nomComplet, :role, :actif, :conseillerId, :dateCreation, :supprime,
                        :versionAgregat)
                on conflict (client_id) do update set login = excluded.login, nom_complet = excluded.nom_complet,
                    role = excluded.role, actif = excluded.actif, conseiller_id = excluded.conseiller_id,
                    date_creation = excluded.date_creation, supprime = excluded.supprime,
                    version_agregat = excluded.version_agregat
                where clients.version_agregat < excluded.version_agregat""")
                .param("clientId", client.clientId())
                .param("login", client.login())
                .param("nomComplet", client.nomComplet())
                .param("role", client.role().name())
                .param("actif", client.actif())
                .param("conseillerId", client.conseillerId(), java.sql.Types.BIGINT)
                .param("dateCreation", client.dateCreation())
                .param("supprime", client.supprime())
                .param("versionAgregat", client.versionAgregat())
                .update() == 1;
    }

    @Override
    public Map<Long, String> nomsComplets(Collection<Long> clientIds) {
        Map<Long, String> noms = new HashMap<>();
        jdbc.sql("select client_id, nom_complet from clients where client_id in (:ids)")
                .param("ids", clientIds)
                .query((ligne, numero) -> noms.put(ligne.getLong("client_id"), ligne.getString("nom_complet")))
                .list();
        return noms;
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
