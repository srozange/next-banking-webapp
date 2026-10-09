package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterPortefeuille;
import fr.nextbanking.coeurbancaire.domain.ClientEtSesComptes;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code GET /api/coeur-bancaire/conseiller/portefeuille}, comptes des clients du conseiller
 * désigné par le sujet du jeton. Aucun paramètre de requête n'est lu ; le rôle CONSEILLER ou ADMIN est vérifié par le
 * service (403 sinon), un sujet non numérique donne 401. Codes de type tels quels, soldes en chaîne à 2 décimales.
 */
@RestController
public class PointEntreePortefeuille {

    static final String CHEMIN = "/api/coeur-bancaire/conseiller/portefeuille";

    private final ConsulterPortefeuille consulterPortefeuille;

    public PointEntreePortefeuille(ConsulterPortefeuille consulterPortefeuille) {
        this.consulterPortefeuille = consulterPortefeuille;
    }

    @GetMapping(CHEMIN)
    public List<ClientJson> portefeuille(@AuthenticationPrincipal Jwt jeton) {
        return consulterPortefeuille.consulter(acteurId(jeton), JetonRoles.depuis(jeton)).stream()
                .map(PointEntreePortefeuille::client)
                .toList();
    }

    private static long acteurId(Jwt jeton) {
        try {
            return Long.parseLong(jeton.getSubject());
        } catch (NumberFormatException e) {
            throw new SujetIllisible();
        }
    }

    private static ClientJson client(ClientEtSesComptes client) {
        return new ClientJson(client.clientId(), client.comptes().stream()
                .map(compte -> new CompteJson(compte.id(), compte.numero(), compte.type().name(),
                        compte.solde().texte()))
                .toList());
    }

    /** Client du portefeuille et ses comptes. */
    public record ClientJson(long clientId, List<CompteJson> comptes) {
    }

    /** Compte : type en code ({@code COURANT}, {@code EPARGNE}), solde en chaîne décimale ({@code "2130.00"}). */
    public record CompteJson(long compteId, String numero, String type, String solde) {
    }
}
