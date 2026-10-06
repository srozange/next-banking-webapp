package fr.nextbanking.identiteclients.adapter.in.http;

import fr.nextbanking.identiteclients.application.port.in.RenouvelerSession;
import fr.nextbanking.identiteclients.application.port.in.SeDeconnecter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP de la session : {@code POST /api/identite-clients/deconnexion} (204, audit LOGOUT, sans
 * liste de révocation) et {@code POST /api/identite-clients/renouvellement} (jeton neuf, 401 si le client est
 * inactif ou inconnu). Les deux exigent un jeton valide. Le journal ne porte jamais de jeton.
 */
@RestController
public class PointEntreeSession {

    private static final Logger JOURNAL = LoggerFactory.getLogger(PointEntreeSession.class);

    private final SeDeconnecter seDeconnecter;
    private final RenouvelerSession renouvelerSession;
    private final ResolveurAdresseIp resolveurAdresseIp;

    public PointEntreeSession(SeDeconnecter seDeconnecter, RenouvelerSession renouvelerSession,
            ResolveurAdresseIp resolveurAdresseIp) {
        this.seDeconnecter = seDeconnecter;
        this.renouvelerSession = renouvelerSession;
        this.resolveurAdresseIp = resolveurAdresseIp;
    }

    @PostMapping("/api/identite-clients/deconnexion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void seDeconnecter(@AuthenticationPrincipal Jwt jeton, HttpServletRequest requete) {
        long clientId = clientId(jeton);
        seDeconnecter.seDeconnecter(clientId, resolveurAdresseIp.resoudre(requete));
        JOURNAL.info("Déconnexion (client {})", clientId);
    }

    @PostMapping("/api/identite-clients/renouvellement")
    public PointEntreeConnexion.JetonEmis renouveler(@AuthenticationPrincipal Jwt jeton) {
        long clientId = clientId(jeton);
        String neuf = renouvelerSession.renouveler(clientId).orElseThrow(() -> {
            JOURNAL.info("Renouvellement refusé (client {})", clientId);
            return new AuthentificationRequise();
        });
        JOURNAL.info("Session renouvelée (client {})", clientId);
        return new PointEntreeConnexion.JetonEmis(neuf);
    }

    private static long clientId(Jwt jeton) {
        try {
            return Long.parseLong(jeton.getSubject());
        } catch (NumberFormatException e) {
            throw new AuthentificationRequise();
        }
    }
}
