package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : table {@code comptes}. Le débit est une mise à jour conditionnelle
 * ({@code solde >= montant}) : un solde ne devient jamais négatif, même en concurrence.
 */
@Component
public class ComptesJdbc implements Comptes {

    private final JdbcClient jdbc;

    public ComptesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Compte> duClient(long clientId) {
        return jdbc.sql("select id, client_id, numero, type, solde from comptes where client_id = :clientId order by id")
                .param("clientId", clientId)
                .query(ComptesJdbc::compte)
                .list();
    }

    @Override
    public Optional<Compte> trouver(long compteId) {
        return jdbc.sql("select id, client_id, numero, type, solde from comptes where id = :id")
                .param("id", compteId)
                .query(ComptesJdbc::compte)
                .optional();
    }

    @Override
    public List<Compte> verrouiller(long premierId, long secondId) {
        return jdbc.sql("""
                select id, client_id, numero, type, solde from comptes
                where id in (:premier, :second) order by id for update""")
                .param("premier", premierId)
                .param("second", secondId)
                .query(ComptesJdbc::compte)
                .list();
    }

    @Override
    public boolean debiter(CompteId compteId, Montant montant) {
        return jdbc.sql("update comptes set solde = solde - :montant where id = :id and solde >= :montant")
                .param("montant", montant.valeur())
                .param("id", compteId.valeur())
                .update() == 1;
    }

    @Override
    public void crediter(CompteId compteId, Montant montant) {
        jdbc.sql("update comptes set solde = solde + :montant where id = :id")
                .param("montant", montant.valeur())
                .param("id", compteId.valeur())
                .update();
    }

    private static Compte compte(ResultSet ligne, int numero) throws SQLException {
        return new Compte(ligne.getLong("id"), ligne.getLong("client_id"), ligne.getString("numero"),
                TypeCompte.valueOf(ligne.getString("type")), new Montant(ligne.getBigDecimal("solde")));
    }
}
