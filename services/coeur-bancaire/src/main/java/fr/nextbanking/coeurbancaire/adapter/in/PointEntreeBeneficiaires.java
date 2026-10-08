package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.AjouterBeneficiaire;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterBeneficiaires;
import fr.nextbanking.coeurbancaire.application.port.in.SupprimerBeneficiaire;
import fr.nextbanking.coeurbancaire.application.port.in.VerifierIban;
import fr.nextbanking.coeurbancaire.domain.BeneficiaireExterne;
import fr.nextbanking.coeurbancaire.domain.VerificationIban;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    private final VerifierIban verifier;
    private final AjouterBeneficiaire ajouter;

    public PointEntreeBeneficiaires(ConsulterBeneficiaires consulter, SupprimerBeneficiaire supprimer,
            VerifierIban verifier, AjouterBeneficiaire ajouter) {
        this.consulter = consulter;
        this.supprimer = supprimer;
        this.verifier = verifier;
        this.ajouter = ajouter;
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

    @PostMapping(CHEMIN_BENEFICIAIRES + "/verification-iban")
    public ResultatVerification verifierIban(@RequestBody CorpsVerification corps) {
        VerificationIban resultat = verifier.verifier(corps.iban());
        return new ResultatVerification(resultat.statut().name().toLowerCase(Locale.ROOT), resultat.codePays(),
                resultat.zoneSepa());
    }

    @PostMapping(CHEMIN_BENEFICIAIRES)
    @ResponseStatus(HttpStatus.CREATED)
    public Beneficiaire ajouter(@AuthenticationPrincipal Jwt jeton, @RequestBody CorpsAjout corps) {
        return beneficiaire(ajouter.ajouter(clientId(jeton), corps.libelle(), corps.titulaire(), corps.iban(),
                corps.bic(), corps.banque()));
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

    /** Corps de la vérification : l'IBAN tel que saisi. */
    public record CorpsVerification(String iban) {
    }

    /** Résultat de la vérification, sans texte : statut {@code vide}, {@code invalide} ou {@code valide}. */
    public record ResultatVerification(String statut, String codePays, boolean zoneSepa) {
    }

    /** Corps de l'ajout : la saisie brute, validée et normalisée par le domaine. */
    public record CorpsAjout(String libelle, String titulaire, String iban, String bic, String banque) {
    }
}
