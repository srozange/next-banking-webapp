package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.ProjectionClients;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : table {@code clients}, projection locale du nom complet. L'insertion ou la mise à jour n'a lieu
 * que si {@code version_agregat} reçue est strictement supérieure à celle de la ligne.
 */
@Component
public class ProjectionClientsJdbc implements ProjectionClients {

    private final JdbcClient jdbc;

    public ProjectionClientsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean enregistrerSiPlusRecent(long clientId, String nomComplet, long versionAgregat) {
        return jdbc.sql("""
                insert into clients (client_id, nom_complet, version_agregat)
                values (:clientId, :nomComplet, :version)
                on conflict (client_id) do update
                set nom_complet = excluded.nom_complet, version_agregat = excluded.version_agregat
                where clients.version_agregat < excluded.version_agregat""")
                .param("clientId", clientId)
                .param("nomComplet", nomComplet)
                .param("version", versionAgregat)
                .update() == 1;
    }
}
