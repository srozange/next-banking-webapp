package fr.nextbanking.identiteclients.adapter.out.persistance;

import fr.nextbanking.identiteclients.application.port.out.PortefeuilleClients;
import fr.nextbanking.identiteclients.domain.ClientDuPortefeuille;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : clients de la table {@code customers} affectés à un conseiller ({@code advisor_id}).
 */
@Component
public class PortefeuilleClientsJdbc implements PortefeuilleClients {

    private final JdbcClient jdbc;

    public PortefeuilleClientsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ClientDuPortefeuille> duConseiller(long conseillerId) {
        return jdbc.sql("select id, full_name, username, email, phone, active from customers"
                        + " where advisor_id = :conseiller and not supprime order by full_name, id")
                .param("conseiller", conseillerId)
                .query((ligne, numero) -> new ClientDuPortefeuille(ligne.getLong("id"), ligne.getString("full_name"),
                        ligne.getString("username"), ligne.getString("email"), ligne.getString("phone"),
                        ligne.getBoolean("active")))
                .list();
    }
}
