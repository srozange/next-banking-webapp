package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import fr.nextbanking.coeurbancaire.application.port.out.Beneficiaires;
import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import fr.nextbanking.coeurbancaire.domain.Iban;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Adaptateur sortant : table {@code beneficiaires_externes} ; la suppression est une désactivation logique. */
@Component
public class BeneficiairesJdbc implements Beneficiaires {

    private static final String COLONNES = "select id, client_id, libelle, titulaire, iban, bic, banque "
            + "from beneficiaires_externes ";

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

    @Override
    public List<BeneficiaireExterne> actifsDuClient(long clientId) {
        return jdbc.sql(COLONNES + "where client_id = :clientId and actif order by libelle, id")
                .param("clientId", clientId)
                .query((lignes, numero) -> new BeneficiaireExterne(lignes.getLong("id"), lignes.getLong("client_id"),
                        lignes.getString("libelle"), lignes.getString("titulaire"), new Iban(lignes.getString("iban")),
                        lignes.getString("bic"), lignes.getString("banque")))
                .list();
    }

    @Override
    public Optional<BeneficiaireExterne> trouverActif(long beneficiaireId) {
        return jdbc.sql(COLONNES + "where id = :id and actif")
                .param("id", beneficiaireId)
                .query((lignes, numero) -> new BeneficiaireExterne(lignes.getLong("id"), lignes.getLong("client_id"),
                        lignes.getString("libelle"), lignes.getString("titulaire"), new Iban(lignes.getString("iban")),
                        lignes.getString("bic"), lignes.getString("banque")))
                .optional();
    }

    @Override
    public boolean desactiver(long beneficiaireId, long clientId) {
        return jdbc.sql("update beneficiaires_externes set actif = false "
                + "where id = :id and client_id = :clientId and actif")
                .param("id", beneficiaireId)
                .param("clientId", clientId)
                .update() == 1;
    }
}
