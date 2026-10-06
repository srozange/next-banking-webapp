package fr.nextbanking.openbankingconseiller.adapter.out.persistance;

import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.domain.CompteExterne;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import fr.nextbanking.openbankingconseiller.domain.TransactionExterne;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adaptateur sortant : état Powens par client ({@code etat_powens}) et {@code state} à usage unique. */
@Component
public class DepotEtatPowensJdbc implements DepotEtatPowens {

    private final JdbcClient jdbc;

    public DepotEtatPowensJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<EtatPowens> lire(String sub) {
        return jdbc.sql("select jeton_permanent, connecte, message from etat_powens where sub = :sub")
                .param("sub", sub)
                .query((ligne, numero) -> new EtatPowens(ligne.getString("jeton_permanent"),
                        ligne.getBoolean("connecte"), ligne.getString("message")))
                .optional();
    }

    @Override
    public void enregistrerJeton(String sub, String jeton) {
        jdbc.sql("insert into etat_powens (sub, jeton_permanent) values (:sub, :jeton)"
                        + " on conflict (sub) do update set jeton_permanent = excluded.jeton_permanent")
                .param("sub", sub)
                .param("jeton", jeton)
                .update();
    }

    @Override
    public void enregistrerState(StatePowens state) {
        jdbc.sql("insert into states_powens (nonce, sub, expire_a) values (:nonce, :sub, :expire)")
                .param("nonce", state.nonce())
                .param("sub", state.sub())
                .param("expire", Timestamp.from(state.expiration()))
                .update();
    }

    @Override
    public boolean consommerState(StatePowens state, Instant maintenant) {
        return jdbc.sql("update states_powens set utilise = true where nonce = :nonce and sub = :sub"
                        + " and not utilise and expire_a > :maintenant")
                .param("nonce", state.nonce())
                .param("sub", state.sub())
                .param("maintenant", Timestamp.from(maintenant))
                .update() == 1;
    }

    @Override
    @Transactional
    public void remettreAZero(String sub) {
        effacerComptes(sub);
        jdbc.sql("update etat_powens set jeton_permanent = null, connecte = false, message = null,"
                + " compte_selectionne = null where sub = :sub").param("sub", sub).update();
    }

    @Override
    public void poserMessage(String sub, String message) {
        jdbc.sql("insert into etat_powens (sub, message) values (:sub, :message)"
                        + " on conflict (sub) do update set message = excluded.message")
                .param("sub", sub)
                .param("message", message)
                .update();
    }

    @Override
    @Transactional
    public void enregistrerConnexion(String sub, List<CompteExterne> comptes, Long selectionne,
            List<TransactionExterne> transactions, String message) {
        effacerComptes(sub);
        for (CompteExterne compte : comptes) {
            jdbc.sql("insert into comptes_externes (sub, compte_id, nom, type, solde, iban)"
                            + " values (:sub, :id, :nom, :type, :solde, :iban)")
                    .param("sub", sub).param("id", compte.id()).param("nom", compte.nom())
                    .param("type", compte.type()).param("solde", compte.solde()).param("iban", compte.iban())
                    .update();
        }
        for (TransactionExterne transaction : transactions) {
            jdbc.sql("insert into transactions_externes (sub, compte_id, id, date, libelle, montant)"
                            + " values (:sub, :compte, :id, :date, :libelle, :montant)")
                    .param("sub", sub).param("compte", selectionne).param("id", transaction.id())
                    .param("date", Date.valueOf(transaction.date())).param("libelle", transaction.libelle())
                    .param("montant", transaction.montant())
                    .update();
        }
        jdbc.sql("insert into etat_powens (sub, connecte, message, compte_selectionne)"
                        + " values (:sub, true, :message, :selectionne)"
                        + " on conflict (sub) do update set connecte = true, message = excluded.message,"
                        + " compte_selectionne = excluded.compte_selectionne")
                .param("sub", sub).param("message", message).param("selectionne", selectionne)
                .update();
    }

    private void effacerComptes(String sub) {
        jdbc.sql("delete from transactions_externes where sub = :sub").param("sub", sub).update();
        jdbc.sql("delete from comptes_externes where sub = :sub").param("sub", sub).update();
    }
}
