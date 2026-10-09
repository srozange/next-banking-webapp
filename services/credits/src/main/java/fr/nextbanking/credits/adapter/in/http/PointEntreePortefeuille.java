package fr.nextbanking.credits.adapter.in.http;

import fr.nextbanking.credits.application.port.in.ConsulterPortefeuille;
import fr.nextbanking.credits.domain.ClientEtSesPrets;
import fr.nextbanking.credits.domain.Pret;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code GET /api/credits/conseiller/portefeuille}, prêts des clients du conseiller désigné
 * par le sujet du jeton. Aucun paramètre de requête n'est lu ; le rôle CONSEILLER ou ADMIN est exigé par le cas
 * d'usage (403 sinon), un sujet non numérique donne 401. Codes de type et de statut tels quels ({@code type} nul pour
 * un prêt sans type, BUG-078), montants en chaînes à 2 décimales.
 */
@RestController
public class PointEntreePortefeuille {

    private final ConsulterPortefeuille consulterPortefeuille;

    public PointEntreePortefeuille(ConsulterPortefeuille consulterPortefeuille) {
        this.consulterPortefeuille = consulterPortefeuille;
    }

    @GetMapping("/api/credits/conseiller/portefeuille")
    public List<ClientJson> portefeuille(@AuthenticationPrincipal Jwt jeton) {
        return consulterPortefeuille.consulter(acteurId(jeton), JetonRoles.depuis(jeton)).stream()
                .map(PointEntreePortefeuille::client)
                .toList();
    }

    private static long acteurId(Jwt jeton) {
        try {
            return Long.parseLong(jeton.getSubject());
        } catch (NumberFormatException e) {
            throw new AuthentificationRequise();
        }
    }

    private static ClientJson client(ClientEtSesPrets client) {
        return new ClientJson(client.clientId(), client.prets().stream().map(PointEntreePortefeuille::pret).toList());
    }

    private static PretJson pret(Pret pret) {
        return new PretJson(pret.id(), pret.type() == null ? null : pret.type().name(),
                pret.montant().toPlainString(), pret.mensualite().toPlainString(), pret.statut().name());
    }

    /** Client du portefeuille et ses prêts. */
    public record ClientJson(long clientId, List<PretJson> prets) {
    }

    /** Prêt : type et statut en codes, montant et mensualité en chaînes décimales. */
    public record PretJson(long pretId, String type, String montant, String mensualite, String statut) {
    }
}
