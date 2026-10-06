package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.domain.DecisionRevue;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.IssueRevue;
import fr.nextbanking.coeurbancaire.domain.LigneRevue;
import fr.nextbanking.coeurbancaire.domain.LigneSuivi;
import fr.nextbanking.coeurbancaire.domain.LigneTraitee;
import fr.nextbanking.coeurbancaire.domain.StatutAffichage;
import fr.nextbanking.coeurbancaire.domain.Virement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
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
            select id, client_id, compte_source_id, compte_destinataire_id, montant, motif, etat, statut, soumis_le,
                   heure_butoir, motif_suspension
            from virements""";

    private static final String COLONNES_REVUE = """
            select v.id, v.client_id, c.nom_complet, v.montant, d.numero as numero_destinataire, v.motif_suspension,
                   v.heure_butoir, v.issue, v.date_issue
            from virements v
            join comptes d on d.id = v.compte_destinataire_id
            left join clients c on c.client_id = v.client_id""";

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
                                           etat, statut, soumis_le, cle_idempotence, heure_butoir,
                                           motif_suspension)
                    values (:id, :clientId, :source, :destinataire, :montant, :motif, :etat, :statut, :soumisLe,
                            :cle, :heureButoir, :motifSuspension)""")
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
                    .param("heureButoir", virement.heureButoir() == null ? null
                            : Timestamp.from(virement.heureButoir()), Types.TIMESTAMP)
                    .param("motifSuspension", virement.motifSuspension())
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

    @Override
    public boolean transiter(long virementId, EtatVirement de, EtatVirement vers, DecisionRevue decision) {
        if (!de.peutTransiterVers(vers)) {
            return false;
        }
        if (decision == null) {
            return jdbc.sql("update virements set etat = :vers where id = :id and etat = :de")
                    .param("vers", vers.name())
                    .param("id", virementId)
                    .param("de", de.name())
                    .update() == 1;
        }
        return jdbc.sql("""
                update virements set etat = :vers, issue = :issue, decideur = :decideur, motif_decision = :motif,
                                     date_issue = :date
                where id = :id and etat = :de""")
                .param("vers", vers.name())
                .param("issue", decision.issue().name())
                .param("decideur", decision.decideur())
                .param("motif", decision.motif())
                .param("date", Timestamp.from(decision.date()))
                .param("id", virementId)
                .param("de", de.name())
                .update() == 1;
    }

    @Override
    public List<Virement> suspendusEchus(Instant maintenant) {
        return jdbc.sql(COLONNES + " where etat = 'SUSPENDU' and heure_butoir <= :maintenant order by heure_butoir, id")
                .param("maintenant", Timestamp.from(maintenant))
                .query(DepotVirementsJdbc::virement)
                .list();
    }

    @Override
    public List<LigneRevue> fileDeRevue() {
        return jdbc.sql(COLONNES_REVUE + " where v.etat = 'SUSPENDU' order by v.heure_butoir, v.id")
                .query(DepotVirementsJdbc::ligneRevue)
                .list();
    }

    @Override
    public List<LigneTraitee> historiqueDesTraites(int limite) {
        return jdbc.sql(COLONNES_REVUE + " where v.issue is not null order by v.date_issue desc, v.id desc limit :limite")
                .param("limite", limite)
                .query((ligne, numero) -> new LigneTraitee(ligneRevue(ligne, numero),
                        IssueRevue.valueOf(ligne.getString("issue")), ligne.getTimestamp("date_issue").toInstant()))
                .list();
    }

    @Override
    public List<LigneSuivi> suiviDuClient(long clientId, int limite) {
        return jdbc.sql("""
                select v.id, v.soumis_le, v.montant, v.etat, v.heure_butoir, d.numero as numero_destinataire
                from virements v join comptes d on d.id = v.compte_destinataire_id
                where v.client_id = :clientId and v.heure_butoir is not null
                order by v.soumis_le desc, v.id desc limit :limite""")
                .param("clientId", clientId)
                .param("limite", limite)
                .query((ligne, numero) -> new LigneSuivi(ligne.getLong("id"), ligne.getTimestamp("soumis_le").toInstant(),
                        ligne.getString("numero_destinataire"), ligne.getBigDecimal("montant"),
                        EtatVirement.valueOf(ligne.getString("etat")),
                        ligne.getTimestamp("heure_butoir").toInstant()))
                .list();
    }

    private static LigneRevue ligneRevue(ResultSet ligne, int numero) throws SQLException {
        return new LigneRevue(ligne.getLong("id"), ligne.getLong("client_id"), ligne.getString("nom_complet"),
                ligne.getBigDecimal("montant"), ligne.getString("numero_destinataire"),
                ligne.getString("motif_suspension"), ligne.getTimestamp("heure_butoir").toInstant());
    }

    private static Virement virement(ResultSet ligne, int numero) throws SQLException {
        Timestamp butoir = ligne.getTimestamp("heure_butoir");
        return new Virement(ligne.getLong("id"), ligne.getLong("client_id"), ligne.getLong("compte_source_id"),
                ligne.getLong("compte_destinataire_id"), ligne.getBigDecimal("montant"), ligne.getString("motif"),
                EtatVirement.valueOf(ligne.getString("etat")), StatutAffichage.valueOf(ligne.getString("statut")),
                ligne.getTimestamp("soumis_le").toInstant(), butoir == null ? null : butoir.toInstant(),
                ligne.getString("motif_suspension"));
    }
}
