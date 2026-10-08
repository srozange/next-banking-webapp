package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterBeneficiaires;
import fr.nextbanking.coeurbancaire.application.port.in.SupprimerBeneficiaire;
import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code GET /api/coeur-bancaire/beneficiaires} (bénéficiaires actifs du client du jeton) et
 * {@code DELETE /api/coeur-bancaire/beneficiaires/{id}} (suppression logique). Le client est celui du jeton
 * ({@code sub}), jamais un paramètre. L'IBAN n'est rendu que sous sa forme d'affichage ({@code FR35***9010}).
 */
@RestController
public class PointEntreeBeneficiaires {

    static final String CHEMIN_BENEFICIAIRES = "/api/coeur-bancaire/beneficiaires";

    private final ConsulterBeneficiaires consulter;
    private final SupprimerBeneficiaire supprimer;

    public PointEntreeBeneficiaires(ConsulterBeneficiaires consulter, SupprimerBeneficiaire supprimer) {
        this.consulter = consulter;
        this.supprimer = supprimer;
    }

    @GetMapping(CHEMIN_BENEFICIAIRES)
    public List<Beneficiaire> beneficiaires(@AuthenticationPrincipal Jwt jeton) {
        return consulter.beneficiaires(clientId(jeton)).stream().map(PointEntreeBeneficiaires::beneficiaire).toList();
    }

    @DeleteMapping(CHEMIN_BENEFICIAIRES + "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal Jwt jeton, @PathVariable long id) {
        supprimer.supprimer(clientId(jeton), id);
    }

    private static long clientId(Jwt jeton) {
        return Long.parseLong(jeton.getSubject());
    }

    private static Beneficiaire beneficiaire(BeneficiaireExterne b) {
        return new Beneficiaire(b.id(), b.libelle(), b.titulaire(), b.iban().affichage(), b.bic(), b.banque(),
                b.iban().zoneSepa());
    }

    /** Bénéficiaire affiché : IBAN masqué (3 étoiles), BIC et banque nuls s'ils ne sont pas renseignés. */
    public record Beneficiaire(long id, String libelle, String titulaire, String iban, String bic, String banque,
            boolean zoneSepa) {
    }
}
