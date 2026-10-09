package fr.nextbanking.coeurbancaire.adapter.in;

import fr.nextbanking.coeurbancaire.application.port.in.RevoirAlertesFraude;
import fr.nextbanking.coeurbancaire.domain.AlerteAdministree;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP des alertes de fraude de l'administration : liste de toutes les alertes, confirmation et
 * classement en faux positif. Aucun paramètre de requête n'est lu ; le rôle ADMIN est vérifié par le service (403
 * sinon). Une revue acceptée par le rôle rend 204, que l'alerte ait changé ou non (déjà traitée, inconnue).
 */
@RestController
public class PointEntreeAlertesFraude {

    static final String CHEMIN = "/api/coeur-bancaire/administration/alertes-fraude";
    static final String CHEMIN_CONFIRMATION = CHEMIN + "/{alerteId}/confirmation";
    static final String CHEMIN_FAUX_POSITIF = CHEMIN + "/{alerteId}/faux-positif";

    private final RevoirAlertesFraude revoirAlertesFraude;

    public PointEntreeAlertesFraude(RevoirAlertesFraude revoirAlertesFraude) {
        this.revoirAlertesFraude = revoirAlertesFraude;
    }

    @GetMapping(CHEMIN)
    public List<AlerteJson> toutes(@AuthenticationPrincipal Jwt jeton) {
        return revoirAlertesFraude.toutes(JetonRoles.depuis(jeton)).stream().map(PointEntreeAlertesFraude::json)
                .toList();
    }

    @PostMapping(CHEMIN_CONFIRMATION)
    public ResponseEntity<Void> confirmer(@AuthenticationPrincipal Jwt jeton, @PathVariable long alerteId) {
        return revoir(jeton, alerteId, StatutAlerte.CONFIRMED);
    }

    @PostMapping(CHEMIN_FAUX_POSITIF)
    public ResponseEntity<Void> fauxPositif(@AuthenticationPrincipal Jwt jeton, @PathVariable long alerteId) {
        return revoir(jeton, alerteId, StatutAlerte.FALSE_POSITIVE);
    }

    private ResponseEntity<Void> revoir(Jwt jeton, long alerteId, StatutAlerte issue) {
        revoirAlertesFraude.revoir(alerteId, issue, acteurId(jeton), JetonRoles.depuis(jeton));
        return ResponseEntity.noContent().build();
    }

    private static long acteurId(Jwt jeton) {
        try {
            return Long.parseLong(jeton.getSubject());
        } catch (NumberFormatException e) {
            throw new SujetIllisible();
        }
    }

    private static AlerteJson json(AlerteAdministree alerte) {
        return new AlerteJson(alerte.alerteId(), alerte.clientId(), alerte.nomClient(), alerte.regle(),
                alerte.score(), alerte.statut().name(), alerte.description(), alerte.creeLe().toString(),
                alerte.revuPar(), alerte.nomRelecteur());
    }

    /** Alerte : codes métier tels quels, date de création en ISO-8601 UTC, noms nuls hors projection. */
    public record AlerteJson(long alerteId, long clientId, String nomClient, String regle, int score, String statut,
            String description, String creeLe, String revuPar, String nomRelecteur) {
    }
}
