package fr.nextbanking.openbankingconseiller.adapter.out.persistance;

import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import fr.nextbanking.openbankingconseiller.domain.StatePowens;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

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
}
