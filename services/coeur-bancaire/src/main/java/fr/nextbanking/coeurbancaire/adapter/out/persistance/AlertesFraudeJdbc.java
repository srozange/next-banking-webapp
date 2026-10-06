package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.AlertesFraude;
import fr.nextbanking.coeurbancaire.domain.AlerteFraude;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Adaptateur sortant : table {@code alertes_fraude} (FRAUD_ALERTS du legacy), écrite dans la transaction du virement. */
@Component
public class AlertesFraudeJdbc implements AlertesFraude {

    private final JdbcClient jdbc;

    public AlertesFraudeJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void enregistrer(AlerteFraude alerte) {
        jdbc.sql("""
                insert into alertes_fraude (client_id, virement_id, regle, score, description, statut, cree_le)
                values (:clientId, :virementId, :regle, :score, :description, :statut, :creeLe)""")
                .param("clientId", alerte.clientId())
                .param("virementId", alerte.virementId())
                .param("regle", alerte.regle().name())
                .param("score", alerte.score())
                .param("description", alerte.description())
                .param("statut", alerte.statut().name())
                .param("creeLe", Timestamp.from(alerte.creeLe()))
                .update();
    }
}
