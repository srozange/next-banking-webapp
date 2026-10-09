package fr.nextbanking.documents.adapter.out.persistance;

import fr.nextbanking.documents.application.port.out.ProjectionComptes;
import fr.nextbanking.documents.domain.CompteProjete;
import fr.nextbanking.documents.domain.TypeCompte;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : projection locale des comptes, table {@code comptes} ; {@code version_agregat} écarte un
 * message ancien. Aucune requête ne modifie un solde autrement que par l'état complet reçu de coeur-bancaire.
 */
@Component
public class ProjectionComptesJdbc implements ProjectionComptes {

    private final JdbcClient jdbc;

    public ProjectionComptesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean enregistrerSiPlusRecent(CompteProjete compte) {
        return jdbc.sql("""
                insert into comptes (compte_id, client_id, numero, type, iban, bic, solde, version_agregat)
                values (:compteId, :clientId, :numero, :type, :iban, :bic, :solde, :versionAgregat)
                on conflict (compte_id) do update set client_id = excluded.client_id, numero = excluded.numero,
                    type = excluded.type, iban = excluded.iban, bic = excluded.bic, solde = excluded.solde,
                    version_agregat = excluded.version_agregat
                where comptes.version_agregat < excluded.version_agregat""")
                .param("compteId", compte.compteId())
                .param("clientId", compte.clientId())
                .param("numero", compte.numero())
                .param("type", compte.type().name())
                .param("iban", compte.iban())
                .param("bic", compte.bic())
                .param("solde", compte.solde())
                .param("versionAgregat", compte.versionAgregat())
                .update() == 1;
    }

    @Override
    public Optional<CompteProjete> trouver(long compteId) {
        return jdbc.sql("""
                select compte_id, client_id, numero, type, iban, bic, solde, version_agregat
                from comptes where compte_id = :id""")
                .param("id", compteId)
                .query((ligne, numero) -> new CompteProjete(ligne.getLong("compte_id"), ligne.getLong("client_id"),
                        ligne.getString("numero"), TypeCompte.valueOf(ligne.getString("type")),
                        ligne.getString("iban"), ligne.getString("bic"), ligne.getBigDecimal("solde"),
                        ligne.getLong("version_agregat")))
                .optional();
    }

    @Override
    public List<CompteProjete> parClient(long clientId) {
        return jdbc.sql("""
                select compte_id, client_id, numero, type, iban, bic, solde, version_agregat
                from comptes where client_id = :id order by compte_id""")
                .param("id", clientId)
                .query((ligne, numero) -> new CompteProjete(ligne.getLong("compte_id"), ligne.getLong("client_id"),
                        ligne.getString("numero"), TypeCompte.valueOf(ligne.getString("type")),
                        ligne.getString("iban"), ligne.getString("bic"), ligne.getBigDecimal("solde"),
                        ligne.getLong("version_agregat")))
                .list();
    }
}
