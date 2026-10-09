package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.AlertesFraude;
import fr.nextbanking.coeurbancaire.domain.AlerteAdministree;
import fr.nextbanking.coeurbancaire.domain.AlerteFraude;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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

    @Override
    public int passerEnRevue(long virementId, String revuPar, Instant revueLe) {
        return jdbc.sql("""
                update alertes_fraude set statut = 'REVIEWED', revu_le = :revueLe, revu_par = :revuPar
                where virement_id = :virementId and statut = 'OPEN'""")
                .param("revueLe", Timestamp.from(revueLe))
                .param("revuPar", revuPar)
                .param("virementId", virementId)
                .update();
    }

    @Override
    public List<AlerteAdministree> toutes() {
        return jdbc.sql("""
                select a.id, a.client_id, c.nom_complet as nom_client, a.regle, a.score, a.statut, a.description,
                       a.cree_le, a.revu_par, r.nom_complet as nom_relecteur
                from alertes_fraude a
                left join clients c on c.client_id = a.client_id
                left join clients r on r.client_id::text = a.revu_par
                order by a.cree_le desc, a.id desc""")
                .query((ligne, rang) -> new AlerteAdministree(ligne.getLong("id"), ligne.getLong("client_id"),
                        ligne.getString("nom_client"), ligne.getString("regle"), ligne.getInt("score"),
                        StatutAlerte.valueOf(ligne.getString("statut")), ligne.getString("description"),
                        ligne.getTimestamp("cree_le").toInstant(), ligne.getString("revu_par"),
                        ligne.getString("nom_relecteur")))
                .list();
    }

    @Override
    public boolean revoir(long alerteId, StatutAlerte issue, String revuPar, Instant revueLe) {
        return jdbc.sql("""
                update alertes_fraude set statut = :issue, revu_par = :revuPar, revu_le = :revueLe
                where id = :id and statut = 'OPEN'""")
                .param("issue", issue.name())
                .param("revuPar", revuPar)
                .param("revueLe", Timestamp.from(revueLe))
                .param("id", alerteId)
                .update() == 1;
    }
}
