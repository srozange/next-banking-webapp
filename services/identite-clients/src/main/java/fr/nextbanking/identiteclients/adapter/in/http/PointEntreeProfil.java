package fr.nextbanking.identiteclients.adapter.in.http;

import fr.nextbanking.identiteclients.application.port.in.ConsulterProfil;
import fr.nextbanking.identiteclients.domain.Client;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code GET /api/identite-clients/moi}, profil du client désigné par le sujet du jeton.
 * Un sujet illisible ou un client absent donnent 401.
 */
@RestController
public class PointEntreeProfil {

    private final ConsulterProfil consulterProfil;

    public PointEntreeProfil(ConsulterProfil consulterProfil) {
        this.consulterProfil = consulterProfil;
    }

    @GetMapping("/api/identite-clients/moi")
    public Profil moi(@AuthenticationPrincipal Jwt jeton) {
        Client client = consulterProfil.consulter(clientId(jeton))
                .orElseThrow(AuthentificationRequise::new);
        return new Profil(client.identifiant(), client.nomComplet(), List.of(client.role().name()));
    }

    private static long clientId(Jwt jeton) {
        try {
            return Long.parseLong(jeton.getSubject());
        } catch (NumberFormatException e) {
            throw new AuthentificationRequise();
        }
    }

    /** Corps de la réponse. */
    public record Profil(String identifiant, String nomComplet, List<String> roles) {
    }
}
