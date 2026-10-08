package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.GererPlafonds;
import fr.nextbanking.coeurbancaire.domain.Plafond;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code PUT /api/coeur-bancaire/budget/plafonds} (création ou remplacement) et
 * {@code DELETE /api/coeur-bancaire/budget/plafonds/{id}}. Le client est celui du jeton ({@code sub}), jamais un
 * paramètre ; montants en chaîne décimale à point.
 */
@RestController
public class PointEntreePlafonds {

    static final String CHEMIN = "/api/coeur-bancaire/budget/plafonds";

    private final GererPlafonds plafonds;

    public PointEntreePlafonds(GererPlafonds plafonds) {
        this.plafonds = plafonds;
    }

    @PutMapping(CHEMIN)
    public PlafondDto enregistrer(@AuthenticationPrincipal Jwt jeton, @RequestBody CorpsPlafond corps) {
        Plafond plafond = plafonds.enregistrer(Long.parseLong(jeton.getSubject()), corps.categorie(), corps.mois(),
                corps.plafond());
        return new PlafondDto(plafond.id(), plafond.categorie().libelle(), plafond.mois(), plafond.montant().texte());
    }

    @DeleteMapping(CHEMIN + "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal Jwt jeton, @PathVariable long id) {
        plafonds.supprimer(Long.parseLong(jeton.getSubject()), id);
    }

    /** Corps de l'enregistrement : la saisie brute, validée par le domaine ({@code mois} au format AAAA-MM). */
    public record CorpsPlafond(String categorie, String mois, String plafond) {
    }

    /** Plafond enregistré. */
    public record PlafondDto(long id, String categorie, String mois, String plafond) {
    }
}
