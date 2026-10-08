package fr.nextbanking.credits.adapter.in.http;

import fr.nextbanking.credits.application.port.in.ConsulterPipelinePrets;
import fr.nextbanking.credits.application.port.in.DecisionAdministrateurPret;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.PretAvecClient;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : administration des prêts (pipeline EN_ATTENTE, historique, approbation et refus). Le rôle
 * ADMIN est exigé par les cas d'usage, d'après la revendication {@code roles} du jeton ; l'adresse IP d'origine est
 * résolue ici, une seule fois. Un sujet qui n'est pas un identifiant numérique donne 401.
 */
@RestController
public class PointEntreeAdministrationPrets {

    private final ConsulterPipelinePrets pipeline;
    private final DecisionAdministrateurPret decision;
    private final ResolveurAdresseIp adresses;

    public PointEntreeAdministrationPrets(ConsulterPipelinePrets pipeline, DecisionAdministrateurPret decision,
            ResolveurAdresseIp adresses) {
        this.pipeline = pipeline;
        this.decision = decision;
        this.adresses = adresses;
    }

    @GetMapping("/api/credits/administration/prets/en-attente")
    public List<PretAdministrationJson> enAttente(@AuthenticationPrincipal Jwt jeton) {
        return pipeline.enAttente(JetonRoles.depuis(jeton)).stream().map(PretAdministrationJson::de).toList();
    }

    @GetMapping("/api/credits/administration/prets")
    public List<PretAdministrationJson> historique(@AuthenticationPrincipal Jwt jeton) {
        return pipeline.historique(JetonRoles.depuis(jeton)).stream().map(PretAdministrationJson::de).toList();
    }

    @PostMapping("/api/credits/administration/prets/{pretId}/approbation")
    @ResponseStatus(HttpStatus.OK)
    public void approuver(@AuthenticationPrincipal Jwt jeton, @PathVariable long pretId,
            HttpServletRequest requete) {
        decision.approuver(JetonRoles.depuis(jeton), administrateur(jeton), pretId, adresses.resoudre(requete));
    }

    @PostMapping("/api/credits/administration/prets/{pretId}/refus")
    @ResponseStatus(HttpStatus.OK)
    public void refuser(@AuthenticationPrincipal Jwt jeton, @PathVariable long pretId, HttpServletRequest requete) {
        decision.refuser(JetonRoles.depuis(jeton), administrateur(jeton), pretId, adresses.resoudre(requete));
    }

    private static String administrateur(Jwt jeton) {
        try {
            return Long.toString(Long.parseLong(jeton.getSubject()));
        } catch (NumberFormatException e) {
            throw new AuthentificationRequise();
        }
    }

    /**
     * Ligne d'administration : champs en français, montants en chaînes à 2 décimales, date {@code yyyy-MM-dd},
     * {@code type} nul pour un prêt sans type (BUG-078).
     */
    public record PretAdministrationJson(long id, long clientId, String nomClient, String type, String montant,
            String statut, int dureeMois, String tauxAnnuel, String mensualite, String objet, String dateDebut) {

        static PretAdministrationJson de(PretAvecClient ligne) {
            Pret pret = ligne.pret();
            return new PretAdministrationJson(pret.id(), pret.clientId(), ligne.nomClient(),
                    pret.type() == null ? null : pret.type().name(), pret.montant().toPlainString(),
                    pret.statut().name(), pret.dureeMois(), pret.tauxAnnuel().toPlainString(),
                    pret.mensualite().toPlainString(), pret.objet(), pret.dateDebut().toString());
        }
    }
}
