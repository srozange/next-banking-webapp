package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.StatutAffichage;
import fr.nextbanking.coeurbancaire.domain.Virement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : table {@code virements}. L'unicité de la clé d'idempotence par client est portée par un index
 * unique : une deuxième écriture sous la même clé est refusée.
 */
@Component
public class DepotVirementsJdbc implements DepotVirements {

    private static final String COLONNES = """
            select id, client_id, compte_source_id, compte_destinataire_id, montant, motif, etat, statut, soumis_le
            from virements""";

    private final JdbcClient jdbc;

    public DepotVirementsJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long attribuerIdentifiant() {
        return jdbc.sql("select nextval(pg_get_serial_sequence('virements', 'id'))").query(Long.class).single();
    }

    @Override
    public void enregistrer(Virement virement, String cleIdempotence) {
        try {
            jdbc.sql("""
                    insert into virements (id, client_id, compte_source_id, compte_destinataire_id, montant, motif,
                                           etat, statut, soumis_le, cle_idempotence)
                    values (:id, :clientId, :source, :destinataire, :montant, :motif, :etat, :statut, :soumisLe,
                            :cle)""")
                    .param("id", virement.id())
                    .param("clientId", virement.clientId())
                    .param("source", virement.compteSourceId())
                    .param("destinataire", virement.compteDestinataireId())
                    .param("montant", virement.montant())
                    .param("motif", virement.motif())
                    .param("etat", virement.etat().name())
                    .param("statut", virement.statut().name())
                    .param("soumisLe", Timestamp.from(virement.soumisLe()))
                    .param("cle", cleIdempotence)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new ErreurVirement(Cause.CLE_IDEMPOTENCE_DEJA_UTILISEE, e);
        }
    }

    @Override
    public Optional<Virement> parCle(long clientId, String cleIdempotence) {
        return jdbc.sql(COLONNES + " where client_id = :clientId and cle_idempotence = :cle")
                .param("clientId", clientId)
                .param("cle", cleIdempotence)
                .query(DepotVirementsJdbc::virement)
                .optional();
    }

    @Override
    public Optional<Virement> parId(long virementId) {
        return jdbc.sql(COLONNES + " where id = :id")
                .param("id", virementId)
                .query(DepotVirementsJdbc::virement)
                .optional();
    }

    private static Virement virement(ResultSet ligne, int numero) throws SQLException {
        return new Virement(ligne.getLong("id"), ligne.getLong("client_id"), ligne.getLong("compte_source_id"),
                ligne.getLong("compte_destinataire_id"), ligne.getBigDecimal("montant"), ligne.getString("motif"),
                EtatVirement.valueOf(ligne.getString("etat")), StatutAffichage.valueOf(ligne.getString("statut")),
                ligne.getTimestamp("soumis_le").toInstant());
    }
}
