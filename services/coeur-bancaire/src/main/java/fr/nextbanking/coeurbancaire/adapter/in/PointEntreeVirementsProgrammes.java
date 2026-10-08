package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterVirementsProgrammes;
import fr.nextbanking.coeurbancaire.application.port.in.CreerVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.LigneVirementProgramme;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code GET} (virements programmés du client du jeton) et {@code POST} (création) sur
 * {@code /api/coeur-bancaire/virements-programmes}. Le client est celui du jeton ({@code sub}), jamais un paramètre.
 */
@RestController
public class PointEntreeVirementsProgrammes {

    static final String CHEMIN = "/api/coeur-bancaire/virements-programmes";

    private final ConsulterVirementsProgrammes consulter;
    private final CreerVirementProgramme creer;

    public PointEntreeVirementsProgrammes(ConsulterVirementsProgrammes consulter, CreerVirementProgramme creer) {
        this.consulter = consulter;
        this.creer = creer;
    }

    @GetMapping(CHEMIN)
    public List<VirementProgrammeDto> virements(@AuthenticationPrincipal Jwt jeton) {
        return consulter.duClient(clientId(jeton)).stream().map(PointEntreeVirementsProgrammes::dto).toList();
    }

    @PostMapping(CHEMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public VirementProgrammeDto creer(@AuthenticationPrincipal Jwt jeton, @RequestBody CorpsCreation corps) {
        return dto(creer.creer(clientId(jeton), corps.compteSourceId(), corps.numeroBeneficiaire(),
                corps.libelleBeneficiaire(), corps.montant(), corps.frequence(), corps.motif(),
                corps.datePremiereExecution()));
    }

    private static long clientId(Jwt jeton) {
        return Long.parseLong(jeton.getSubject());
    }

    private static VirementProgrammeDto dto(LigneVirementProgramme l) {
        return new VirementProgrammeDto(l.id(), l.compteSourceId(), l.numeroCompteSource(), l.numeroBeneficiaire(),
                l.libelleBeneficiaire(), l.montant().texte(), l.frequence().name(), l.motif(),
                l.prochaineExecution().toString(), l.actif(), l.nbExecutions());
    }

    /** Corps de la création : la saisie brute, validée et normalisée par le domaine. */
    public record CorpsCreation(Long compteSourceId, String numeroBeneficiaire, String libelleBeneficiaire,
            String montant, String frequence, String motif, String datePremiereExecution) {
    }

    /** Virement programmé rendu : montant en chaîne, prochaine exécution en date {@code yyyy-MM-dd}. */
    public record VirementProgrammeDto(long id, long compteSourceId, String numeroCompteSource,
            String numeroBeneficiaire, String libelleBeneficiaire, String montant, String frequence, String motif,
            String prochaineExecution, boolean actif, int nbExecutions) {
    }
}
