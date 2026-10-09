package fr.nextbanking.identiteclients.adapter.out.persistance;

import fr.nextbanking.identiteclients.application.port.out.AnnuaireUtilisateurs;
import fr.nextbanking.identiteclients.domain.FicheUtilisateur;
import fr.nextbanking.identiteclients.domain.Role;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : utilisateurs non supprimés de la table {@code customers}, avec le nom complet du conseiller
 * ({@code advisor_id}) et la date de création de la fiche ({@code date_creation}, BUG-055 : jamais
 * {@code advisor_since}).
 */
@Component
public class AnnuaireUtilisateursJdbc implements AnnuaireUtilisateurs {

    private final JdbcClient jdbc;

    public AnnuaireUtilisateursJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<FicheUtilisateur> tous() {
        return jdbc.sql("select c.id, c.full_name, c.username, c.email, c.phone, c.role, c.active,"
                        + " a.full_name as nom_conseiller, c.date_creation from customers c"
                        + " left join customers a on a.id = c.advisor_id"
                        + " where not c.supprime order by c.full_name, c.id")
                .query((ligne, numero) -> new FicheUtilisateur(ligne.getLong("id"), ligne.getString("full_name"),
                        ligne.getString("username"), ligne.getString("email"), ligne.getString("phone"),
                        Role.valueOf(ligne.getString("role")), ligne.getBoolean("active"),
                        ligne.getString("nom_conseiller"), ligne.getObject("date_creation", LocalDate.class)))
                .list();
    }
}
