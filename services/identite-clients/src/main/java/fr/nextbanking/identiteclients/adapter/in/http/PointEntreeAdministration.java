package fr.nextbanking.identiteclients.adapter.in.http;

import fr.nextbanking.identiteclients.application.port.in.ConsulterAdministration;
import fr.nextbanking.identiteclients.domain.FicheUtilisateur;
import fr.nextbanking.identiteclients.domain.IndicateursUtilisateurs;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : {@code GET /api/identite-clients/administration/{indicateurs|clients|utilisateurs}}. Aucun
 * paramètre de requête n'est lu ; le rôle ADMIN est contrôlé par le cas d'usage (403 sinon).
 */
@RestController
public class PointEntreeAdministration {

    private final ConsulterAdministration consulterAdministration;

    public PointEntreeAdministration(ConsulterAdministration consulterAdministration) {
        this.consulterAdministration = consulterAdministration;
    }

    @GetMapping("/api/identite-clients/administration/indicateurs")
    public IndicateursJson indicateurs(@AuthenticationPrincipal Jwt jeton) {
        IndicateursUtilisateurs indicateurs = consulterAdministration.indicateurs(JetonActeur.roles(jeton));
        return new IndicateursJson(indicateurs.totalClients(), indicateurs.clientsActifs(), indicateurs.conseillers(),
                indicateurs.totalUtilisateurs());
    }

    @GetMapping("/api/identite-clients/administration/clients")
    public List<ClientJson> clients(@AuthenticationPrincipal Jwt jeton) {
        return consulterAdministration.clients(JetonActeur.roles(jeton)).stream().map(ClientJson::de).toList();
    }

    @GetMapping("/api/identite-clients/administration/utilisateurs")
    public List<UtilisateurJson> utilisateurs(@AuthenticationPrincipal Jwt jeton) {
        return consulterAdministration.utilisateurs(JetonActeur.roles(jeton)).stream().map(UtilisateurJson::de)
                .toList();
    }

    /** Indicateurs globaux. */
    public record IndicateursJson(long totalClients, long clientsActifs, long conseillers, long totalUtilisateurs) {
    }

    /**
     * Client de la gestion des clients ; {@code telephone} et {@code nomConseiller} peuvent être nuls,
     * {@code dateCreation} au format {@code yyyy-MM-dd}.
     */
    public record ClientJson(long clientId, String nomComplet, String login, String email, String telephone,
            String role, boolean actif, String nomConseiller, String dateCreation) {

        static ClientJson de(FicheUtilisateur fiche) {
            return new ClientJson(fiche.id(), fiche.nomComplet(), fiche.login(), fiche.email(), fiche.telephone(),
                    fiche.role().name(), fiche.actif(), fiche.nomConseiller(), fiche.dateCreation().toString());
        }
    }

    /** Utilisateur de la liste de tous les utilisateurs. */
    public record UtilisateurJson(long utilisateurId, String nomComplet, String login, String email, String role,
            boolean actif) {

        static UtilisateurJson de(FicheUtilisateur fiche) {
            return new UtilisateurJson(fiche.id(), fiche.nomComplet(), fiche.login(), fiche.email(),
                    fiche.role().name(), fiche.actif());
        }
    }
}
