package fr.nextbanking.identiteclients.adapter.in.http;

import fr.nextbanking.identiteclients.application.port.in.ConsulterPortefeuille;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code GET /api/identite-clients/conseiller/portefeuille}, clients du conseiller désigné
 * par le sujet du jeton. Aucun paramètre de requête n'est lu ; le rôle CONSEILLER ou ADMIN est contrôlé par le cas
 * d'usage (403 sinon), un sujet illisible donne 401.
 */
@RestController
public class PointEntreePortefeuille {

    private final ConsulterPortefeuille consulterPortefeuille;

    public PointEntreePortefeuille(ConsulterPortefeuille consulterPortefeuille) {
        this.consulterPortefeuille = consulterPortefeuille;
    }

    @GetMapping("/api/identite-clients/conseiller/portefeuille")
    public List<ClientJson> portefeuille(@AuthenticationPrincipal Jwt jeton) {
        return consulterPortefeuille.consulter(JetonActeur.acteurId(jeton), JetonActeur.roles(jeton)).stream()
                .map(client -> new ClientJson(client.clientId(), client.nomComplet(), client.login(), client.email(),
                        client.telephone(), client.actif()))
                .toList();
    }

    /** Client du portefeuille ; {@code telephone} peut être nul. */
    public record ClientJson(long clientId, String nomComplet, String login, String email, String telephone,
            boolean actif) {
    }
}
