package fr.nextbanking.identiteclients.adapter.out.persistance;

import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.domain.Client;
import fr.nextbanking.identiteclients.domain.Role;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : clients de la table {@code customers}.
 */
@Component
public class DepotClientsJdbc implements DepotClients {

    private static final String COLONNES = "select id, username, full_name, role, active, password from customers";

    private final JdbcClient jdbc;

    public DepotClientsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Client> parIdentifiant(String identifiant) {
        return jdbc.sql(COLONNES + " where username = :identifiant")
                .param("identifiant", identifiant)
                .query(DepotClientsJdbc::client)
                .optional();
    }

    @Override
    public Optional<Client> parId(long clientId) {
        return jdbc.sql(COLONNES + " where id = :id")
                .param("id", clientId)
                .query(DepotClientsJdbc::client)
                .optional();
    }

    @Override
    public void changerActif(long clientId, boolean actif) {
        jdbc.sql("update customers set active = :actif, version_agregat = version_agregat + 1 where id = :id")
                .param("actif", actif)
                .param("id", clientId)
                .update();
    }

    private static Client client(ResultSet ligne, int numero) throws SQLException {
        return new Client(ligne.getLong("id"), ligne.getString("username"), ligne.getString("full_name"),
                Role.valueOf(ligne.getString("role")), ligne.getBoolean("active"), ligne.getString("password"));
    }
}
