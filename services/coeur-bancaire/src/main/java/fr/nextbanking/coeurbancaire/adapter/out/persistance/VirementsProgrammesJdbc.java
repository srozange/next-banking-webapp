package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.VirementsProgrammes;
import fr.nextbanking.coeurbancaire.domain.DemandeVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.FrequenceVirement;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Adaptateur sortant : table {@code virements_programmes}. */
@Component
public class VirementsProgrammesJdbc implements VirementsProgrammes {

    private final JdbcClient jdbc;

    public VirementsProgrammesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public VirementProgramme enregistrer(long clientId, DemandeVirementProgramme demande, Instant prochaineExecution,
            Instant creeLe) {
        long id = jdbc.sql("""
                insert into virements_programmes (client_id, compte_source_id, numero_beneficiaire,
                    libelle_beneficiaire, montant, frequence, motif, prochaine_execution, actif, nb_executions, cree_le)
                values (:clientId, :compteSourceId, :numero, :libelle, :montant, :frequence, :motif, :prochaine,
                    true, 0, :creeLe)
                returning id""")
                .param("clientId", clientId)
                .param("compteSourceId", demande.compteSourceId())
                .param("numero", demande.numeroBeneficiaire())
                .param("libelle", demande.libelleBeneficiaire())
                .param("montant", demande.montant().valeur())
                .param("frequence", demande.frequence().name())
                .param("motif", demande.motif())
                .param("prochaine", Timestamp.from(prochaineExecution))
                .param("creeLe", Timestamp.from(creeLe))
                .query(Long.class)
                .single();
        return new VirementProgramme(id, clientId, demande.compteSourceId(), demande.numeroBeneficiaire(),
                demande.libelleBeneficiaire(), demande.montant(), demande.frequence(), demande.motif(),
                prochaineExecution, true, 0, creeLe);
    }

    @Override
    public List<VirementProgramme> listerDuClient(long clientId) {
        return jdbc.sql("""
                select id, client_id, compte_source_id, numero_beneficiaire, libelle_beneficiaire, montant, frequence,
                    motif, prochaine_execution, actif, nb_executions, cree_le
                from virements_programmes where client_id = :clientId order by prochaine_execution, id""")
                .param("clientId", clientId)
                .query(VirementsProgrammesJdbc::virement)
                .list();
    }

    private static VirementProgramme virement(ResultSet ligne, int numero) throws SQLException {
        return new VirementProgramme(ligne.getLong("id"), ligne.getLong("client_id"), ligne.getLong("compte_source_id"),
                ligne.getString("numero_beneficiaire"), ligne.getString("libelle_beneficiaire"),
                new Montant(ligne.getBigDecimal("montant")), FrequenceVirement.valueOf(ligne.getString("frequence")),
                ligne.getString("motif"), ligne.getTimestamp("prochaine_execution").toInstant(),
                ligne.getBoolean("actif"), ligne.getInt("nb_executions"), ligne.getTimestamp("cree_le").toInstant());
    }
}
