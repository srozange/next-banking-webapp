package fr.nextbanking.openbankingconseiller.adapter.out.persistance;

import fr.nextbanking.openbankingconseiller.application.port.out.DepotComptesExternes;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import java.sql.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adaptateur sortant : comptes ({@code comptes_externes}) et transactions ({@code transactions_externes}) par client. */
@Component
public class DepotComptesExternesJdbc implements DepotComptesExternes {

    private final JdbcClient jdbc;

    public DepotComptesExternesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CompteExterne> comptes(String sub) {
        return jdbc.sql("select compte_id, nom, type, solde, iban from comptes_externes where sub = :sub"
                        + " order by compte_id")
                .param("sub", sub)
                .query((ligne, numero) -> new CompteExterne(ligne.getLong("compte_id"), ligne.getString("nom"),
                        ligne.getString("type"), ligne.getBigDecimal("solde"), ligne.getString("iban")))
                .list();
    }

    @Override
    public Optional<Long> compteSelectionne(String sub) {
        return jdbc.sql("select compte_selectionne from etat_powens where sub = :sub and compte_selectionne is not null")
                .param("sub", sub)
                .query(Long.class)
                .optional();
    }

    @Override
    public List<TransactionExterne> transactions(String sub) {
        return jdbc.sql("select id, date, libelle, montant from transactions_externes where sub = :sub"
                        + " order by date desc, id desc")
                .param("sub", sub)
                .query((ligne, numero) -> new TransactionExterne(ligne.getLong("id"),
                        ligne.getDate("date").toLocalDate(), ligne.getString("libelle"),
                        ligne.getBigDecimal("montant")))
                .list();
    }

    @Override
    public boolean appartient(String sub, long compteId) {
        return jdbc.sql("select count(*) from comptes_externes where sub = :sub and compte_id = :compte")
                .param("sub", sub).param("compte", compteId)
                .query(Integer.class).single() > 0;
    }

    @Override
    @Transactional
    public void enregistrerSelection(String sub, long compteId, List<TransactionExterne> transactions) {
        jdbc.sql("delete from transactions_externes where sub = :sub").param("sub", sub).update();
        for (TransactionExterne transaction : transactions) {
            jdbc.sql("insert into transactions_externes (sub, compte_id, id, date, libelle, montant)"
                            + " values (:sub, :compte, :id, :date, :libelle, :montant)")
                    .param("sub", sub).param("compte", compteId).param("id", transaction.id())
                    .param("date", Date.valueOf(transaction.date())).param("libelle", transaction.libelle())
                    .param("montant", transaction.montant())
                    .update();
        }
        jdbc.sql("update etat_powens set compte_selectionne = :compte where sub = :sub")
                .param("sub", sub).param("compte", compteId).update();
    }
}
