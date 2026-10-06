package fr.nextbanking.credits.adapter.out.persistance;

import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.domain.EcheancePret;
import fr.nextbanking.credits.domain.NouveauPret;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.StatutPret;
import fr.nextbanking.credits.domain.TypePret;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : prêts de la table {@code loans}.
 */
@Component
public class PretRepositoryJdbc implements PretRepository {

    private static final String COLONNES = "select id, customer_id, type, montant, taux_annuel, duree_mois, mensualite,"
            + " cout_total, total_interets, motif, status, date_debut, date_fin from loans";

    private final JdbcClient jdbc;
    private final JdbcTemplate modele;

    public PretRepositoryJdbc(JdbcTemplate modele) {
        this.modele = modele;
        this.jdbc = JdbcClient.create(modele);
    }

    @Override
    public List<Pret> parClient(long clientId) {
        return jdbc.sql(COLONNES + " where customer_id = :clientId order by date_debut desc, id desc")
                .param("clientId", clientId)
                .query(PretRepositoryJdbc::pret)
                .list();
    }

    @Override
    public Optional<Pret> parId(long pretId) {
        return jdbc.sql(COLONNES + " where id = :id")
                .param("id", pretId)
                .query(PretRepositoryJdbc::pret)
                .optional();
    }

    @Override
    public long creer(NouveauPret pret) {
        return jdbc.sql("""
                insert into loans (customer_id, type, montant, taux_annuel, duree_mois, mensualite, cout_total,
                                   total_interets, motif, status, date_debut, date_fin)
                values (:clientId, :type, :montant, :taux, :duree, :mensualite, :coutTotal, :totalInterets, :motif,
                        :statut, :dateDebut, :dateFin) returning id""")
                .param("clientId", pret.clientId())
                .param("type", pret.type().name())
                .param("montant", pret.montant())
                .param("taux", pret.tauxAnnuel())
                .param("duree", pret.dureeMois())
                .param("mensualite", pret.mensualite())
                .param("coutTotal", pret.coutTotal())
                .param("totalInterets", pret.totalInterets())
                .param("motif", pret.objet())
                .param("statut", StatutPret.EN_ATTENTE.name())
                .param("dateDebut", pret.dateDebut())
                .param("dateFin", pret.dateFin())
                .query(Long.class)
                .single();
    }

    @Override
    public boolean transitionner(long pretId, StatutPret de, StatutPret vers) {
        return jdbc.sql("update loans set status = :vers where id = :id and status = :de")
                .param("vers", vers.name())
                .param("id", pretId)
                .param("de", de.name())
                .update() == 1;
    }

    @Override
    public void creerEcheancier(long pretId, List<EcheancePret> echeances) {
        modele.batchUpdate("""
                insert into loan_repayments (loan_id, numero_echeance, date_echeance, montant_capital,
                                             montant_interets, montant_total, capital_restant, paye)
                values (?, ?, ?, ?, ?, ?, ?, false)""", echeances, echeances.size(), (instruction, echeance) -> {
            instruction.setLong(1, pretId);
            instruction.setInt(2, echeance.numero());
            instruction.setObject(3, echeance.dateEcheance());
            instruction.setBigDecimal(4, echeance.capital());
            instruction.setBigDecimal(5, echeance.interets());
            instruction.setBigDecimal(6, echeance.total());
            instruction.setBigDecimal(7, echeance.capitalRestant());
        });
    }

    private static Pret pret(ResultSet ligne, int numero) throws SQLException {
        return new Pret(ligne.getLong("id"), ligne.getLong("customer_id"), type(ligne.getString("type")),
                ligne.getBigDecimal("montant"), ligne.getBigDecimal("taux_annuel"), ligne.getInt("duree_mois"),
                ligne.getBigDecimal("mensualite"), ligne.getBigDecimal("cout_total"),
                ligne.getBigDecimal("total_interets"), ligne.getString("motif"),
                StatutPret.valueOf(ligne.getString("status")), ligne.getObject("date_debut", java.time.LocalDate.class),
                ligne.getObject("date_fin", java.time.LocalDate.class));
    }

    private static TypePret type(String valeur) {
        return valeur == null ? null : TypePret.valueOf(valeur);
    }
}
