package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.Beneficiaires;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Adaptateur sortant : table {@code beneficiaires_externes}, en lecture seule. */
@Component
public class BeneficiairesJdbc implements Beneficiaires {

    private final JdbcClient jdbc;

    public BeneficiairesJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean estActif(long clientId, String numero) {
        return jdbc.sql("select count(*) from beneficiaires_externes "
                + "where client_id = :clientId and iban = :numero and actif")
                .param("clientId", clientId)
                .param("numero", numero)
                .query(Long.class)
                .single() > 0;
    }
}
