package fr.nextbanking.openbankingconseiller.adapter.in.http;

import fr.nextbanking.openbankingconseiller.application.port.in.ConnecterPowens;
import fr.nextbanking.openbankingconseiller.application.port.in.ConsulterEtatPowens;
import fr.nextbanking.openbankingconseiller.domain.EtatPowens;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP de l'intégration Powens, ouvert à tout utilisateur authentifié (FR-012-05, FR-012-17). L'état
 * est celui du client du jeton ({@code sub}). Ni le jeton Powens ni le secret client ne sortent jamais.
 */
@RestController
public class PointEntreePowens {

    static final String CHEMIN = "/api/open-banking-conseiller/powens";
    static final String CHEMIN_CONNEXION = CHEMIN + "/connexion";

    private final ConnecterPowens connecter;
    private final ConsulterEtatPowens consulter;

    public PointEntreePowens(ConnecterPowens connecter, ConsulterEtatPowens consulter) {
        this.connecter = connecter;
        this.consulter = consulter;
    }

    @PostMapping(CHEMIN_CONNEXION)
    public ReponseConnexion connexion(@AuthenticationPrincipal Jwt jeton) {
        return new ReponseConnexion(connecter.connecter(jeton.getSubject()));
    }

    @GetMapping(CHEMIN)
    public ReponseEtat etat(@AuthenticationPrincipal Jwt jeton) {
        return ReponseEtat.depuis(consulter.consulter(jeton.getSubject()));
    }

    /** Adresse de la page web de Powens. */
    public record ReponseConnexion(String adresse) {
    }

    /** État Powens du client : comptes et transactions arrivent avec les stories suivantes. */
    public record ReponseEtat(boolean connecte, List<Object> comptes, List<Object> transactions, String message) {

        static ReponseEtat depuis(EtatPowens etat) {
            return new ReponseEtat(etat.connecte(), List.of(), List.of(), etat.message());
        }
    }
}
