package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterVirementsProgrammes;
import fr.nextbanking.coeurbancaire.application.port.in.CreerVirementProgramme;
import fr.nextbanking.coeurbancaire.application.port.in.GererEtatVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.LigneVirementProgramme;
import java.util.List;
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
 * Adaptateur entrant HTTP : {@code GET} (virements programmés du client du jeton) et {@code POST} (création), {@code POST …/{id}/suspension|reactivation} et {@code DELETE …/{id}} sur
 * {@code /api/coeur-bancaire/virements-programmes}. Le client est celui du jeton ({@code sub}), jamais un paramètre.
 */
@RestController
public class PointEntreeVirementsProgrammes {

    static final String CHEMIN = "/api/coeur-bancaire/virements-programmes";

    private final ConsulterVirementsProgrammes consulter;
    private final CreerVirementProgramme creer;
    private final GererEtatVirementProgramme etat;

    public PointEntreeVirementsProgrammes(ConsulterVirementsProgrammes consulter, CreerVirementProgramme creer,
            GererEtatVirementProgramme etat) {
        this.consulter = consulter;
        this.creer = creer;
        this.etat = etat;
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

    @PostMapping(CHEMIN + "/{id}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspendre(@AuthenticationPrincipal Jwt jeton, @PathVariable long id) {
        etat.suspendre(clientId(jeton), id);
    }

    @PostMapping(CHEMIN + "/{id}/reactivation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactiver(@AuthenticationPrincipal Jwt jeton, @PathVariable long id) {
        etat.reactiver(clientId(jeton), id);
    }

    @DeleteMapping(CHEMIN + "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal Jwt jeton, @PathVariable long id) {
        etat.supprimer(clientId(jeton), id);
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
